package com.example.bbs.repository;

import com.example.bbs.model.Post;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
        @EntityGraph(attributePaths = { "user" })
        Page<Post> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "user" })
        Page<Post> findByTitleStartingWithOrContentStartingWith(String titlePrefix, String contentPrefix,
                        Pageable pageable);

        @EntityGraph(attributePaths = { "user" })
        Page<Post> findByTitleEndingWithOrContentEndingWith(String titleSuffix, String contentSuffix,
                        Pageable pageable);

        @EntityGraph(attributePaths = "user")
        Optional<Post> findById(Long id);

        List<Post> findByUserId(Long userId);

        boolean existsByUserIdAndTitleAndContentAndCreatedAtAfter(
                        Long userId,
                        String title,
                        String content,
                        LocalDateTime createdAt);

}
