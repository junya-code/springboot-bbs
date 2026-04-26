package com.example.bbs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bbs.config.CookieConfig;
import com.example.bbs.model.User;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        return "admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User admin = userService.getCurrentUser();

        // ★ 試行ログ（人間の動きが一目で分かる）
        log.info("DeleteUser called. adminId={}, targetUserId={}", admin.getId(), id);

        String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        try {
            userService.deleteUserCompletely(id, request, sessionId, botStatus);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.user.delete.failed");
            return "redirect:/admin/users?error=deleteFailed";
        }

        redirectAttributes.addFlashAttribute("successMessageKey", "flash.user.delete.success");
        return "redirect:/admin/users";
    }

}
