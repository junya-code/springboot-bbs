package com.example.bbs.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bbs.model.UserActionLog;
import com.example.bbs.model.enums.ActionType;

public interface UserActionLogRepository extends JpaRepository<UserActionLog, Long> {

    @Query("""
            SELECT COUNT(l)
            FROM UserActionLog l
            WHERE l.userId = :userId
            AND l.action = :action
            AND l.createdAt > :since
            """)
    long countRecentAction(
            @Param("userId") Long userId,
            @Param("action") ActionType action,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT MIN(l.createdAt)
            FROM UserActionLog l
            WHERE l.userId = :userId
            AND l.action = :action
            AND l.createdAt > :since
            """)
    LocalDateTime findOldestActionTime(
            @Param("userId") Long userId,
            @Param("action") ActionType action,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(l)
            FROM UserActionLog l
            WHERE l.sessionId = :sessionId
            AND l.action = :action
            AND l.createdAt > :since
            """)
    long countRecentActionBySession(
            @Param("sessionId") String sessionId,
            @Param("action") ActionType action,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT MIN(l.createdAt)
            FROM UserActionLog l
            WHERE l.sessionId = :sessionId
            AND l.action = :action
            AND l.createdAt > :since
            """)
    LocalDateTime findOldestActionTimeBySession(
            @Param("sessionId") String sessionId,
            @Param("action") ActionType action,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(l)
            FROM UserActionLog l
            WHERE l.sessionId = :sessionId
            AND l.action = :action
            AND l.createdAt > :after
            """)
    long countActionAfter(
            @Param("sessionId") String sessionId,
            @Param("action") ActionType action,
            @Param("after") LocalDateTime after);
}
