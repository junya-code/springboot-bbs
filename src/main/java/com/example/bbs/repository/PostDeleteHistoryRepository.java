package com.example.bbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bbs.model.PostDeleteHistory;

public interface PostDeleteHistoryRepository extends JpaRepository<PostDeleteHistory, Long> {
}
