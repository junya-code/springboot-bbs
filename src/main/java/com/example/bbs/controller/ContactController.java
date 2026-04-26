package com.example.bbs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import com.example.bbs.config.CookieConfig;
import com.example.bbs.config.SpamConfig;
import com.example.bbs.dto.ContactForm;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.service.EmailService;
import com.example.bbs.service.SpamCheckService;
import com.example.bbs.service.UserActionLogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/contact")
@SessionAttributes("contactForm")
public class ContactController {
    private final EmailService emailService;
    private final SpamCheckService spamCheckService;
    private final UserActionLogService userActionLogService;

    public ContactController(EmailService emailService,
            SpamCheckService spamCheckService,
            UserActionLogService userActionLogService) {
        this.emailService = emailService;
        this.spamCheckService = spamCheckService;
        this.userActionLogService = userActionLogService;
    }

    @ModelAttribute("contactForm")
    public ContactForm createContactForm() {
        return new ContactForm();
    }

    @GetMapping
    public String contactForm() {
        return "contact/form";
    }

    @GetMapping("/complete")
    public String completeForm() {
        return "contact/complete";
    }

    @GetMapping("/sending")
    public String sendingForm() {
        return "contact/sending";
    }

    @PostMapping
    public String backToForm() {
        return "contact/form";
    }

    @PostMapping("/confirm")
    public String confirmContact(@Valid @ModelAttribute("contactForm") ContactForm contactForm,
            BindingResult result,
            Model model,
            HttpServletRequest request,
            @RequestParam(name = "hp_field", required = false) String honeypot) {

        // ★ ハニーポット判定
        if (honeypot != null && !honeypot.isBlank()) {
            request.setAttribute("IS_BOT", BotStatus.BOT);
            return "redirect:/"; // BOT は即終了
        }

        if (result.hasErrors()) {
            log.warn("Contact validation failed: {}", result.getAllErrors());
            model.addAttribute("errorMessageKey", "flash.validation.error");
            return "contact/form";
        }

        log.info("Contact confirm OK. name={}", contactForm.getName());

        return "contact/confirm";
    }

    @PostMapping("/submit")
    public String submitContact(
            @Valid @ModelAttribute("contactForm") ContactForm contactForm,
            BindingResult result,
            SessionStatus status,
            Model model,
            HttpServletRequest request,
            @RequestParam(name = "hp_field", required = false) String honeypot) {

        // ★ ハニーポット判定（本番）
        if (honeypot != null && !honeypot.isBlank()) {
            request.setAttribute("IS_BOT", BotStatus.BOT);
            return "redirect:/"; // BOT は即終了
        }

        String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        // --- A. セキュリティ＆バリデーション・ガード ---
        if (result.hasErrors()) {
            log.warn("Validation failed on submit. Unauthorized or invalid access. errors={}", result.getAllErrors());
            model.addAttribute("errorMessageKey", "flash.sessionOrValidationError");
            return "contact/form";
        }

        // --- B. BAN 判定 ---
        if (spamCheckService.isSessionBanned(sessionId, ActionType.SEND_CONTACT)) {
            long remaining = spamCheckService.getSessionRemainingMinutes(sessionId, ActionType.SEND_CONTACT);
            model.addAttribute("errorMessageKey", "flash.contact.tooManyContacts");
            model.addAttribute("errorMessageArg", remaining);
            return "contact/confirm";
        }

        // --- B. 連打・二重送信ガード（PRGパターン用） ---
        // ※ 現在はフロント側で submit ボタンを即時 disable しているため、
        // 通常の利用ではここに到達しない（連打による二重送信が発生しない）。
        // ただし URL 直打ちや BOT など、画面を経由しないアクセスに対する
        // 最終的な保険として残している。
        if (spamCheckService.isLocked(sessionId)) {
            log.info("Request locked: sessionId={}", sessionId);
            return "redirect:/contact/sending";
        }

        if (spamCheckService.isLocked(sessionId)) {
            log.info("Request locked: sessionId={}", sessionId);
            return "redirect:/contact/sending";
        }

        // ★ ロックセット
        spamCheckService.lock(sessionId);

        try {
            // --- D. スパム判定（回数チェック） ---
            if (spamCheckService.isContactSendSpam(sessionId)) {

                // ★ BAN 発動（固定長）
                spamCheckService.banSession(
                        sessionId,
                        ActionType.SEND_CONTACT,
                        SpamConfig.SEND_CONTACT_COOLDOWN_HOURS);

                long remaining = spamCheckService.getSessionRemainingMinutes(sessionId, ActionType.SEND_CONTACT);
                model.addAttribute("errorMessageKey", "flash.contact.tooManyContacts");
                model.addAttribute("errorMessageArg", remaining);

                return "contact/confirm";
            }

            log.info("Contact submit called. name={}", contactForm.getName());

            // ★ メール送信
            emailService.sendUserEmail(contactForm);
            emailService.sendAdminEmail(contactForm);

            // ★ ログ保存
            userActionLogService.logActionSuccess(
                    request,
                    sessionId,
                    null,
                    ActionType.SEND_CONTACT,
                    HttpMethodType.POST,
                    botStatus);

            log.info("Contact email sent successfully. name={}", contactForm.getName());

            status.setComplete();
            return "redirect:/contact/complete";

        } catch (Exception e) {
            log.error("Contact submit failed. error={}", e.getMessage(), e);
            model.addAttribute("errorMessageKey", "flash.contact.sendFailed");

            return "contact/confirm";

        } finally {
            // ★ 必ずロック解除
            spamCheckService.unlock(sessionId);
        }
    }
}
