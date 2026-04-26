package com.example.bbs.controller;

import com.example.bbs.config.CookieConfig;
import com.example.bbs.config.SpamConfig;
import com.example.bbs.dto.LoginForm;
import com.example.bbs.dto.UserRegisterForm;
import com.example.bbs.model.User;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.security.CustomUserDetailsService;
import com.example.bbs.service.SpamCheckService;
import com.example.bbs.service.UserActionLogService;
import com.example.bbs.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/auth")
public class AuthController {
    private final SpamCheckService spamCheckService;
    private final UserActionLogService userActionLogService;
    private final UserService userService;

    public AuthController(CustomUserDetailsService userDetailsService,
            SpamCheckService spamCheckService, UserActionLogService userActionLogService,
            UserService userService) {
        this.spamCheckService = spamCheckService;
        this.userActionLogService = userActionLogService;
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {

        // ★ loginForm が model に無い場合のみ、新規フォームを作る
        // （POST /login の @Valid エラー時は Spring MVC の仕組みで
        // FlashAttribute が自動的に model に復元されるため、ここには入らない）
        if (!model.containsAttribute("loginForm")) {
            LoginForm form = new LoginForm();

            // ★ Spring Security の認証失敗時は、Spring MVC の redirect ではなく
            // Security 独自の redirect が実行されるため、
            // FlashAttribute や BindingResult が復元されない。
            // そのため FailureHandler が一時的にセッションへ保存した username をここで復元する。
            String username = (String) session.getAttribute("loginFormUsername");
            if (username != null) {
                form.setUsername(username);
                session.removeAttribute("loginFormUsername");
            }

            model.addAttribute("loginForm", form);
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("userRegisterForm")) {
            model.addAttribute("userRegisterForm", new UserRegisterForm());
        }
        return "auth/register";
    }

    // Spring Security のデフォルトでは POST /login を自作する必要はない。
    // しかし、独自のバリデーション（@Valid）や、
    // 認証前にエラーメッセージを表示する仕組みを使いたいため、
    // 一度 Controller でフォーム入力を検証してから
    // Spring Security の認証処理へ forward する構成にしている。
    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginForm") LoginForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            @RequestParam(name = "hp_field", required = false) String honeypot) {

        // ★ ハニーポット判定
        if (honeypot != null && !honeypot.isBlank()) {
            request.setAttribute("IS_BOT", BotStatus.BOT);
            return "redirect:/"; // BOT は即終了
        }

        log.info("User login attempt. username={}", form.getUsername());

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.loginForm", result);
            redirectAttributes.addFlashAttribute("loginForm", form);

            return "redirect:/auth/login";
        }

        // バリデーションOK → Spring Security に渡す
        // Controller の中で Spring Security の loginProcessingUrl にリクエストを “内部転送（forward）”
        // するための命令
        return "forward:/auth/login-processing";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("userRegisterForm") UserRegisterForm form,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            @RequestParam(name = "hp_field", required = false) String honeypot) {

        // ★ ハニーポット判定
        if (honeypot != null && !honeypot.isBlank()) {
            request.setAttribute("IS_BOT", BotStatus.BOT);
            return "redirect:/"; // BOT は即終了
        }

        log.info("User register attempt. username={}", form.getUsername());

        if (!result.hasFieldErrors("username") &&
                userService.isUsernameTaken(form.getUsername())) {
            result.rejectValue("username", "error.username.duplicate");
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.userRegisterForm", result);
            redirectAttributes.addFlashAttribute("userRegisterForm", form);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.register.failed");
            return "redirect:/auth/register";
        }

        String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        // --- A. BAN 判定（種類ごとに分離された新方式） ---
        if (spamCheckService.isSessionBanned(sessionId, ActionType.ACCOUNT_CREATE)) {
            long remaining = spamCheckService.getSessionRemainingMinutes(sessionId, ActionType.ACCOUNT_CREATE);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.register.tooManyAccounts");
            redirectAttributes.addFlashAttribute("errorMessageArg", remaining);

            return "redirect:/auth/login";
        }

        // --- B. スパム判定（回数チェック） ---
        if (spamCheckService.isAccountCreateSpam(sessionId)) {

            // ★ BAN 発動（固定長）
            spamCheckService.banSession(
                    sessionId,
                    ActionType.ACCOUNT_CREATE,
                    SpamConfig.ACCOUNT_CREATE_COOLDOWN_HOURS);

            long remaining = spamCheckService.getSessionRemainingMinutes(sessionId, ActionType.ACCOUNT_CREATE);

            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.register.tooManyAccounts");
            redirectAttributes.addFlashAttribute("errorMessageArg", remaining);
            return "redirect:/auth/login";
        }

        // ★ ここで例外が出たら GlobalExceptionHandler が処理する
        User createdUser = userService.registerUser(form);

        // ★ ログ保存
        userActionLogService.logActionSuccess(
                request,
                sessionId,
                createdUser.getId(),
                ActionType.ACCOUNT_CREATE,
                HttpMethodType.POST,
                botStatus);

        redirectAttributes.addFlashAttribute("successMessage", "flash.register.success");
        return "redirect:/auth/login";
    }
}