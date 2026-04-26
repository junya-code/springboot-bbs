package com.example.bbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bbs.model.CommentHistory;

public interface CommentHistoryRepository extends JpaRepository<CommentHistory, Long> {
}
