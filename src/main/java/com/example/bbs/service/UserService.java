package com.example.bbs.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bbs.config.SpamConfig;
import com.example.bbs.dto.UserRegisterForm;
import com.example.bbs.model.Comment;
import com.example.bbs.model.Like;
import com.example.bbs.model.Post;
import com.example.bbs.model.User;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.model.enums.Role;
import com.example.bbs.repository.CommentRepository;
import com.example.bbs.repository.LikeRepository;
import com.example.bbs.repository.PostRepository;
import com.example.bbs.repository.UserRepository;
import com.example.bbs.security.LoginUser;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PostService postService;
    private final CommentService commentService;

    private final CommentHistoryService commentHistoryService;
    private final PostDeleteHistoryService postDeleteHistoryService;
    private final UserHistoryService userHistoryService;
    private final PasswordEncoder passwordEncoder;
    private final UserActionLogService userActionLogService;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final LikeService likeService;
    private final LikeRepository likeRepository;

    public UserService(
            UserRepository userRepository,
            CommentService commentService,
            CommentHistoryService commentHistoryService,
            PostDeleteHistoryService postDeleteHistoryService,
            UserHistoryService userHistoryService,
            PostService postService,
            PasswordEncoder passwordEncoder,
            UserActionLogService userActionLogService,
            CommentRepository commentRepository,
            PostRepository postRepository,
            LikeService likeService,
            LikeRepository likeRepository) {

        this.userRepository = userRepository;
        this.commentService = commentService;
        this.commentHistoryService = commentHistoryService;
        this.postDeleteHistoryService = postDeleteHistoryService;
        this.userHistoryService = userHistoryService;
        this.postService = postService;
        this.passwordEncoder = passwordEncoder;
        this.userActionLogService = userActionLogService;
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.likeService = likeService;
        this.likeRepository = likeRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            Long userId = loginUser.getId();

            return userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("Authenticated user not found in DB. userId={}", userId);
                        return new IllegalStateException(
                                "Authenticated user not found in database: id=" + userId +
                                        ". This may occur if the user was deleted after logging in.");
                    });
        }

        log.warn("getCurrentUser called but principal is not LoginUser. principal={}",
                auth != null ? auth.getPrincipal() : null);

        throw new IllegalStateException("User not logged in");
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // 管理者画面からのみ実行されるユーザー完全削除処理。
    // 対象ユーザーが作成した投稿・コメント、および対象ユーザーの投稿に付いた他ユーザーのコメントを含む
    // すべての関連データを削除し、削除対象となった全データの履歴を保存する。
    @Transactional
    public void deleteUserCompletely(Long targetUserId,
            HttpServletRequest request,
            String sessionId,
            BotStatus botStatus) {

        Long adminId = getCurrentUser().getId();

        // ★ 試行ログ（人間の破壊的操作をファイルログに残す）
        log.info("DeleteUserCompletely called. adminId={}, targetUserId={}", adminId, targetUserId);

        try {
            User user = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // ★ ユーザー自身の Like を全削除
            List<Like> userLikes = likeService.findByUserId(targetUserId);
            for (Like like : userLikes) {
                likeRepository.delete(like);
            }

            // ユーザーが書いたコメント
            List<Comment> userComments = commentService.findByUserId(targetUserId);
            for (Comment comment : userComments) {
                commentHistoryService.saveDeleteHistory(comment, comment.getContent(), adminId);
                commentRepository.delete(comment);
            }

            // ユーザーの投稿と、その投稿に付いたコメント（全員分）
            List<Post> posts = postService.findByUserId(targetUserId);

            for (Post post : posts) {

                // ★ 投稿に付いた Like（全員分）を削除
                List<Like> postLikes = likeService.findByPostId(post.getId());
                for (Like like : postLikes) {
                    likeRepository.delete(like);
                }

                // 投稿に付いたコメントは全て履歴に残す
                List<Comment> comments = commentService.getAllComments(post.getId());
                for (Comment comment : comments) {
                    if (!comment.getUser().getId().equals(targetUserId)) {
                        commentHistoryService.saveDeleteHistory(comment, comment.getContent(), adminId);
                        commentRepository.delete(comment);
                    }
                }

                postDeleteHistoryService.saveDeleteHistory(post, adminId);
                postRepository.delete(post);
            }

            // ユーザー削除履歴
            userHistoryService.saveDeleteHistory(user, adminId);

            userRepository.delete(user);

            // ★ 成功ログ（監査ログ）
            userActionLogService.logActionSuccess(
                    request,
                    sessionId,
                    adminId,
                    ActionType.DELETE_USER_COMPLETELY,
                    HttpMethodType.POST,
                    botStatus);

        } catch (Exception e) {

            // ★ 例外ログ（失敗ログ）
            log.error("DeleteUserCompletely failed. adminId={}, targetUserId={}", adminId, targetUserId, e);

            throw e; // 例外は上に投げる
        }
    }

    @Transactional
    public User registerUser(UserRegisterForm form) {

        log.info("RegisterUser called. username={}", form.getUsername());

        try {
            User user = new User(
                    form.getUsername(),
                    passwordEncoder.encode(form.getPassword()),
                    Role.ROLE_USER);

            userRepository.save(user);

            log.info("User registered successfully. username={}", form.getUsername());
            return user;
        } catch (Exception e) {
            log.error("User register failed in service. username={}", form.getUsername(), e);
            throw e;
        }
    }

    // トランザクション終了時に Hibernate が 自動で UPDATE を発行する
    // Entity側のincreaseLoginFailCountメソッドを呼び出してログイン失敗回数を元にBAN判定
    @Transactional
    public void increaseLoginFail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.increaseLoginFailCount(
                SpamConfig.LOGIN_FAIL_THRESHOLD,
                LocalDateTime.now().plusMinutes(SpamConfig.LOGIN_LOCK_MINUTES));
    }

    // トランザクション終了時に Hibernate が 自動で UPDATE を発行する
    // Entity側のresetLoginFailメソッドを呼び出してBAN解除
    @Transactional
    public void resetLoginFail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.resetLoginFail();
    }

}
