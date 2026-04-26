package com.example.bbs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bbs.config.CookieConfig;
import com.example.bbs.config.SpamConfig;
import com.example.bbs.dto.CommentForm;
import com.example.bbs.model.User;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.service.CommentService;
import com.example.bbs.service.SpamCheckService;
import com.example.bbs.service.UserActionLogService;
import com.example.bbs.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class CommentController {
    private final UserActionLogService userActionLogService;
    private final CommentService commentService;
    private final UserService userService;
    private final SpamCheckService spamCheckService;

    public CommentController(CommentService commentService,
            UserService userService,
            UserActionLogService userActionLogService,
            SpamCheckService spamCheckService) {
        this.commentService = commentService;
        this.userService = userService;
        this.userActionLogService = userActionLogService;
        this.spamCheckService = spamCheckService;
    }

    @PostMapping("/posts/{postId}/comments/create")
    public String addComment(
            @PathVariable Long postId,
            @Valid @ModelAttribute CommentForm commentForm,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            @RequestParam(name = "hp_field", required = false) String honeypot) {

        // ★ ハニーポット判定
        if (honeypot != null && !honeypot.isBlank()) {
            // BOT と判定
            request.setAttribute("IS_BOT", BotStatus.BOT);
            return "redirect:/"; // 何もせずトップへ返す
        }

        if (result.hasErrors()) {
            log.warn("Comment validation failed. postId={}, errors={}", postId, result.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.commentForm", result);
            redirectAttributes.addFlashAttribute("commentForm", commentForm);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.post.create.failed");
            return "redirect:/posts/" + postId;
        }

        User user = userService.getCurrentUser();

        String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        if (spamCheckService.isUserBanned(user.getId(), ActionType.CREATE_COMMENT)) {
            long remaining = spamCheckService.getUserRemainingMinutes(user.getId(), ActionType.CREATE_COMMENT);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.rateLimit.tooManyPosts");
            redirectAttributes.addFlashAttribute("errorMessageArg", remaining);
            return "redirect:/posts/" + postId;
        }

        if (spamCheckService.isCommentCreateSpam(user.getId())) {
            spamCheckService.banUser(
                    user.getId(),
                    ActionType.CREATE_COMMENT,
                    SpamConfig.CREATE_COMMENT_COOLDOWN_HOURS);

            long remaining = spamCheckService.getUserRemainingMinutes(user.getId(), ActionType.CREATE_COMMENT);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.rateLimit.tooManyPosts");
            redirectAttributes.addFlashAttribute("errorMessageArg", remaining);
            return "redirect:/posts/" + postId;
        }

        commentService.addComment(postId, commentForm, user);

        userActionLogService.logActionSuccess(
                request,
                sessionId,
                user.getId(),
                ActionType.CREATE_COMMENT,
                HttpMethodType.POST,
                botStatus);

        redirectAttributes.addFlashAttribute("successMessage", "flash.post.create.success");
        return "redirect:/posts/" + postId;
    }

    @PostMapping("/posts/{postId}/comments/{id}/delete")
    public String deleteComment(
            @PathVariable Long postId,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User user = userService.getCurrentUser();

        log.info("DeleteComment controller called. commentId={}, postId={}, userId={}",
                id, postId, user.getId());

        String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        commentService.deleteComment(id, user);

        userActionLogService.logActionSuccess(
                request,
                sessionId,
                user.getId(),
                ActionType.DELETE_COMMENT,
                HttpMethodType.POST,
                botStatus);

        redirectAttributes.addFlashAttribute("successMessage", "flash.post.delete.success");

        return "redirect:/posts/" + postId;
    }

}
