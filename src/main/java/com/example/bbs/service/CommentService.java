package com.example.bbs.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bbs.config.SpamConfig;
import com.example.bbs.dto.CommentForm;
import com.example.bbs.model.Comment;
import com.example.bbs.model.Post;
import com.example.bbs.model.User;
import com.example.bbs.repository.CommentRepository;
import com.example.bbs.repository.PostRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentHistoryService commentHistoryService;

    public CommentService(CommentRepository commentRepository,
            PostRepository postRepository, CommentHistoryService commentHistoryService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.commentHistoryService = commentHistoryService;
    }

    public List<Comment> findByUserId(Long userId) {
        return commentRepository.findByUserId(userId);
    }

    public List<Comment> getAllComments(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    public Page<Comment> getCommentsPage(Long postId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findByPostId(postId, pageable);
    }

    public Page<Comment> getSafeCommentsPage(Long postId, int page, int size) {

        Page<Comment> commentPage = getCommentsPage(postId, page, size);
        int totalPages = commentPage.getTotalPages();

        // コメント0件 → page=0 を許可
        if (totalPages == 0) {
            return commentPage;
        }

        // 範囲外 → 最終ページへ補正
        if (page < 0 || page >= totalPages) {
            int lastPage = totalPages - 1;
            return getCommentsPage(postId, lastPage, size);
        }

        return commentPage;
    }

    private boolean verifyOwnership(Comment comment, User user) {
        return comment.getUser() != null && comment.getUser().getId().equals(user.getId());
    }

    @Transactional
    public void addComment(Long postId, CommentForm form, User user) {

        log.info("AddComment called. postId={}, userId={}", postId, user.getId());

        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> {
                        log.warn("Post not found. postId={}", postId);
                        return new NoSuchElementException("Post not found with id: " + postId);
                    });

            // ★ 冪等化：短時間の重複投稿を防止
            boolean exists = commentRepository.existsByUserIdAndPostIdAndContentAndCreatedAtAfter(
                    user.getId(),
                    postId,
                    form.getContent(),
                    LocalDateTime.now().minusSeconds(SpamConfig.COMMENT_DUPLICATE_WINDOW_SECONDS));

            if (exists) {
                log.warn("Duplicate comment skipped. postId={}, userId={}", postId, user.getId());
                return;
            }

            Comment comment = new Comment(
                    form.getContent(),
                    post,
                    user);
            commentRepository.save(comment);

            log.info("Comment added. postId={}, userId={}", postId, user.getId());

        } catch (Exception e) {
            log.error("AddComment failed. postId={}, userId={}", postId, user.getId(), e);
            throw e;
        }
    }

    @Transactional
    public void deleteComment(Long id, User user) {

        log.info("DeleteComment called. commentId={}, userId={}", id, user.getId());

        try {
            Optional<Comment> commentOpt = commentRepository.findById(id);

            // ★ 冪等性：すでに削除済みなら何もしない
            if (commentOpt.isEmpty()) {
                log.warn("Delete skipped. Already deleted. commentId={}", id);
                return;
            }

            Comment comment = commentOpt.get();

            if (!verifyOwnership(comment, user)) {
                log.warn("Delete denied. commentId={}, userId={}, commentUserId={}",
                        id, user.getId(), comment.getUser().getId());
                throw new AccessDeniedException("Not authorized");
            }

            String oldContent = comment.getContent();
            commentHistoryService.saveDeleteHistory(comment, oldContent, user.getId());

            commentRepository.delete(comment);

            log.info("Delete command issued for Comment. commentId={}, userId={}, postId={}",
                    id, user.getId(), comment.getPost().getId());

        } catch (Exception e) {
            log.error("DeleteComment failed. commentId={}, userId={}", id, user.getId(), e);
            throw e;
        }
    }

}
