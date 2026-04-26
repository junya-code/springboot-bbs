package com.example.bbs.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bbs.model.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = { "user" })
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    List<Comment> findByPostId(Long postId);

    List<Comment> findByUserId(Long userId);

    // ★ 冪等化のための重複チェック
    boolean existsByUserIdAndPostIdAndContentAndCreatedAtAfter(
            Long userId,
            Long postId,
            String content,
            LocalDateTime createdAt);
}
