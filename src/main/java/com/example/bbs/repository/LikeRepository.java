package com.example.bbs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bbs.model.Like;
import com.example.bbs.model.Post;
import com.example.bbs.model.User;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserAndPost(User user, Post post);

    boolean existsByUserAndPost(User user, Post post);

    int countByPost(Post post);

    List<Like> findByUserId(Long userId);

    List<Like> findByPostId(Long postId);

}
