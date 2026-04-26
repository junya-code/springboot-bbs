package com.example.bbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bbs.model.PostHistory;

public interface PostHistoryRepository extends JpaRepository<PostHistory, Long> {
    PostHistory findTop1ByPostIdOrderByEditedAtDesc(Long postId);

}
