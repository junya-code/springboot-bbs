package com.example.bbs.controller;

import com.example.bbs.config.CookieConfig;
import com.example.bbs.config.PagingConfig;
import com.example.bbs.config.SpamConfig;
import com.example.bbs.dto.CommentForm;
import com.example.bbs.dto.LikeInfo;
import com.example.bbs.dto.PostForm;
import com.example.bbs.model.Comment;
import com.example.bbs.model.Post;
import com.example.bbs.model.User;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.service.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/posts")
public class PostController {
    private final UserActionLogService userActionLogService;
    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    private final LikeService likeService;
    private final SpamCheckService spamCheckService;

    public PostController(PostService postService,
            CommentService commentService,
            UserService userService,
            LikeService likeService,
            UserActionLogService userActionLogService,
            SpamCheckService spamCheckService) {
        this.postService = postService;
        this.commentService = commentService;
        this.userService = userService;
        this.likeService = likeService;
        this.userActionLogService = userActionLogService;
        this.spamCheckService = spamCheckService;
    }

    @GetMapping
    public String listPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "contains") String matchType,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        User loggedInUser = userService.getCurrentUser();
        log.info("ListPosts called. keyword={}, matchType={}, sortBy={}, sortOrder={}, page={},userId={}",
                keyword, matchType, sortBy, sortOrder, page, loggedInUser.getId());

        int size = PagingConfig.POST_PAGE_SIZE;
        Page<Post> posts = postService.getSafePostsPage(
                keyword, matchType, sortBy, sortOrder, page, size);

        String msg = (String) session.getAttribute("successMessage");
        if (msg != null) {
            model.addAttribute("successMessage", msg);
            session.removeAttribute("successMessage"); // ★ 1回だけ表示
        }

        model.addAttribute("posts", posts);
        model.addAttribute("loggedInUserId", loggedInUser.getId());
        model.addAttribute("loggedInUsername", loggedInUser.getUsername());

        return "posts/list";
    }

    @GetMapping("/privacy")
    public String viewPrivacy() {
        return "posts/privacy";
    }

    @GetMapping("/terms")
    public String viewTerms() {
        return "posts/terms";
    }

    @GetMapping("/{id}")
    public String viewPost(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        User loggedInUser = userService.getCurrentUser();

        log.info("ViewPost called. id={},userId={}", id, loggedInUser.getId());

        model.addAttribute("loggedInUserId", loggedInUser.getId());

        Post post = postService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Post not found with id: " + id));
        model.addAttribute("post", post);

        LikeInfo likeInfo = likeService.getLikeInfo(loggedInUser, post);
        model.addAttribute("likeInfo", likeInfo);

        int size = PagingConfig.COMMENT_PAGE_SIZE;
        Page<Comment> commentPage = commentService.getSafeCommentsPage(id, page, size);
        model.addAttribute("comments", commentPage);

        LocalDateTime lastEditedAt = postService.getLastEditedAt(id);
        model.addAttribute("lastEditedAt", lastEditedAt);
        model.addAttribute("edited", lastEditedAt != null);

        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CommentForm());
        }

        return "posts/detail";
    }

    @GetMapping("/new")
    public String newPostForm(Model model) {
        User loggedInUser = userService.getCurrentUser();
        log.info("NewPostForm called. userId={}", loggedInUser.getId());
        if (!model.containsAttribute("post")) {
            model.addAttribute("post", new PostForm());
        }
        return "posts/new";
    }

    @GetMapping("/{id}/edit")
    public String editPostForm(@PathVariable Long id, Model model) {
        User loggedInUser = userService.getCurrentUser();
        log.info("EditPostForm called. id={},userId={}", id, loggedInUser.getId());

        if (!model.containsAttribute("post")) {
            PostForm form = postService.getEditForm(id, loggedInUser);
            model.addAttribute("post", form);
        }

        model.addAttribute("postId", id);

        return "posts/edit";
    }

    @PostMapping("/{id}")
    public String updatePost(
            @PathVariable Long id,
            @Valid @ModelAttribute("post") PostForm postForm,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            @RequestParam(name = "hp_field", required = false) String honeypot) {

        if (result.hasErrors()) {
            log.warn("Validation failed on update. id={}, errors={}", id, result.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.post", result);
            redirectAttributes.addFlashAttribute("post", postForm);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.post.create.failed");
            return "redirect:/posts/" + id + "/edit";
        }

        // ★ ハニーポット判定
        if (honeypot != null && !honeypot.isBlank()) { // BOT と判定
            request.setAttribute("IS_BOT", BotStatus.BOT);
            return "redirect:/"; // 何もせずトップへ返す
        }

        User loggedInUser = userService.getCurrentUser();

        String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        if (spamCheckService.isUserBanned(loggedInUser.getId(), ActionType.EDIT_POST)) {
            long remaining = spamCheckService.getUserRemainingMinutes(loggedInUser.getId(), ActionType.EDIT_POST);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.edit.tooManyEdits");
            redirectAttributes.addFlashAttribute("errorMessageArg", remaining);
            return "redirect:/posts/" + id;
        }

        if (spamCheckService.isPostEditSpam(loggedInUser.getId())) {
            spamCheckService.banUser(
                    loggedInUser.getId(),
                    ActionType.EDIT_POST,
                    SpamConfig.EDIT_POST_COOLDOWN_HOURS);

            long remaining = spamCheckService.getUserRemainingMinutes(loggedInUser.getId(), ActionType.EDIT_POST);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.edit.tooManyEdits");
            redirectAttributes.addFlashAttribute("errorMessageArg", remaining);
            return "redirect:/posts/" + id;
        }

        try {
            postService.updatePost(id, postForm, loggedInUser);
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.notAuthorized");
            return "redirect:/posts?error=notAuthorized";
        }

        userActionLogService.logActionSuccess(
                request,
                sessionId,
                loggedInUser.getId(),
                ActionType.EDIT_POST,
                HttpMethodType.POST,
                botStatus);

        redirectAttributes.addFlashAttribute("successMessage", "flash.post.update.success");
        return "redirect:/posts/" + id;
    }

    @PostMapping
    public String createPost(
            @Valid @ModelAttribute("post") PostForm postForm,
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
            log.warn("Validation failed: {}", result.getAllErrors());
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.post", result);
            redirectAttributes.addFlashAttribute("post", postForm);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.post.create.failed");
            return "redirect:/posts/new";
        }

        User user = userService.getCurrentUser();

        String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        // すでにBAN中なら即終了
        if (spamCheckService.isUserBanned(user.getId(), ActionType.CREATE_POST)) {
            long remaining = spamCheckService.getUserRemainingMinutes(user.getId(), ActionType.CREATE_POST);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.rateLimit.tooManyPosts");
            redirectAttributes.addFlashAttribute("errorMessageArg", remaining);
            return "redirect:/posts";
        }

        // スパム判定（回数チェック）
        if (spamCheckService.isPostCreateSpam(user.getId())) {
            spamCheckService.banUser(
                    user.getId(),
                    ActionType.CREATE_POST,
                    SpamConfig.CREATE_POST_COOLDOWN_HOURS);

            long remaining = spamCheckService.getUserRemainingMinutes(user.getId(), ActionType.CREATE_POST);
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.rateLimit.tooManyPosts");
            redirectAttributes.addFlashAttribute("errorMessageArg", remaining);
            return "redirect:/posts";
        }

        postService.createPost(postForm, user);

        userActionLogService.logActionSuccess(
                request,
                sessionId,
                user.getId(),
                ActionType.CREATE_POST,
                HttpMethodType.POST,
                botStatus);

        redirectAttributes.addFlashAttribute("successMessage", "flash.post.create.success");

        return "redirect:/posts";
    }

    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {

        User loggedInUser = userService.getCurrentUser();
        log.info("DeletePost called. userId={}, postId={}", loggedInUser.getId(), id);

        String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        try {
            postService.deletePost(id, loggedInUser);
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessageKey", "flash.board.delete.failed");
            return "redirect:/posts?error=notAuthorized";
        }

        userActionLogService.logActionSuccess(
                request,
                sessionId,
                loggedInUser.getId(),
                ActionType.DELETE_POST,
                HttpMethodType.POST,
                botStatus);

        redirectAttributes.addFlashAttribute("successMessage", "flash.post.delete.success");
        return "redirect:/posts";
    }

}
