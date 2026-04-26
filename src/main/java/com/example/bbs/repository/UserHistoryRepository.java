package com.example.bbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bbs.model.UserHistory;

public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
}
