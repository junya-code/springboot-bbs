package com.example.bbs.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.bbs.config.SpamConfig;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.repository.UserActionLogRepository;

@Service
public class SpamCheckService {

    private final UserActionLogService userActionLogService;
    private final UserActionLogRepository repo;

    // ★ sessionId → ロック開始時刻（1つの Map に統合）
    private final Map<String, LocalDateTime> lockMap = new ConcurrentHashMap<>();

    // ★ userId → (ActionType → BAN解除時刻)
    private final Map<Long, Map<ActionType, LocalDateTime>> userBanMap = new ConcurrentHashMap<>();

    // ★ sessionId → (ActionType → BAN解除時刻)
    private final Map<String, Map<ActionType, LocalDateTime>> sessionBanMap = new ConcurrentHashMap<>();

    public SpamCheckService(UserActionLogService userActionLogService,
            UserActionLogRepository repo) {
        this.userActionLogService = userActionLogService;
        this.repo = repo;
    }

    // ★ ロック中かどうか
    public boolean isLocked(String sessionId) {
        return lockMap.containsKey(sessionId);
    }

    // ★ ロック開始
    public void lock(String sessionId) {
        lockMap.put(sessionId, LocalDateTime.now());
    }

    // ★ ロック解除
    public void unlock(String sessionId) {
        lockMap.remove(sessionId);
    }

    // ★ ロック後に SEND_CONTACT が発生したか
    public boolean hasContactLog(String sessionId) {
        LocalDateTime lockStartedAt = lockMap.get(sessionId);
        if (lockStartedAt == null)
            return false;

        return repo.countActionAfter(
                sessionId,
                ActionType.SEND_CONTACT,
                lockStartedAt) > 0;
    }

    public boolean isPostCreateSpam(Long userId) {
        long count = userActionLogService.countRecentAction(
                userId,
                ActionType.CREATE_POST,
                SpamConfig.CREATE_POST_COOLDOWN_HOURS);
        return count >= SpamConfig.CREATE_POST_LIMIT;
    }

    public boolean isCommentCreateSpam(Long userId) {
        long count = userActionLogService.countRecentAction(
                userId,
                ActionType.CREATE_COMMENT,
                SpamConfig.CREATE_COMMENT_COOLDOWN_HOURS);
        return count >= SpamConfig.CREATE_COMMENT_LIMIT;
    }

    public boolean isPostEditSpam(Long userId) {
        long count = userActionLogService.countRecentAction(
                userId,
                ActionType.EDIT_POST,
                SpamConfig.EDIT_POST_COOLDOWN_HOURS);
        return count >= SpamConfig.EDIT_POST_LIMIT;
    }

    public boolean isContactSendSpam(String sessionId) {
        long count = repo.countRecentActionBySession(
                sessionId,
                ActionType.SEND_CONTACT,
                LocalDateTime.now().minusHours(SpamConfig.SEND_CONTACT_COOLDOWN_HOURS));
        return count >= SpamConfig.SEND_CONTACT_LIMIT;
    }

    public boolean isAccountCreateSpam(String sessionId) {
        long count = repo.countRecentActionBySession(
                sessionId,
                ActionType.ACCOUNT_CREATE,
                LocalDateTime.now().minusHours(SpamConfig.ACCOUNT_CREATE_COOLDOWN_HOURS));
        return count >= SpamConfig.ACCOUNT_CREATE_LIMIT;
    }

    // ============================
    // BAN 判定（userId）
    // ============================
    public boolean isUserBanned(Long userId, ActionType action) {
        Map<ActionType, LocalDateTime> map = userBanMap.get(userId);
        if (map == null)
            return false;

        LocalDateTime until = map.get(action);
        return until != null && LocalDateTime.now().isBefore(until);
    }

    public long getUserRemainingMinutes(Long userId, ActionType action) {
        Map<ActionType, LocalDateTime> map = userBanMap.get(userId);
        if (map == null)
            return 0;

        LocalDateTime until = map.get(action);
        if (until == null)
            return 0;

        return Math.max(0, Duration.between(LocalDateTime.now(), until).toMinutes());
    }

    public void banUser(Long userId, ActionType action, int hours) {
        userBanMap
                .computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(action, LocalDateTime.now().plusHours(hours));
    }

    public void cleanupUserBanMap() {
        LocalDateTime now = LocalDateTime.now();

        userBanMap.entrySet().removeIf(userEntry -> {
            Map<ActionType, LocalDateTime> inner = userEntry.getValue();

            // ActionType → until の期限切れを削除
            inner.entrySet().removeIf(e -> e.getValue().isBefore(now));

            // 内側が空になったら userId ごと削除
            return inner.isEmpty();
        });
    }

    // ============================
    // BAN 判定（sessionId）
    // ============================
    public boolean isSessionBanned(String sessionId, ActionType action) {
        Map<ActionType, LocalDateTime> map = sessionBanMap.get(sessionId);
        if (map == null)
            return false;

        LocalDateTime until = map.get(action);
        return until != null && LocalDateTime.now().isBefore(until);
    }

    public long getSessionRemainingMinutes(String sessionId, ActionType action) {
        Map<ActionType, LocalDateTime> map = sessionBanMap.get(sessionId);
        if (map == null)
            return 0;

        LocalDateTime until = map.get(action);
        if (until == null)
            return 0;

        return Math.max(0, Duration.between(LocalDateTime.now(), until).toMinutes());
    }

    public void banSession(String sessionId, ActionType action, int hours) {
        sessionBanMap
                .computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(action, LocalDateTime.now().plusHours(hours));
    }

    public void cleanupSessionBanMap() {
        LocalDateTime now = LocalDateTime.now();

        sessionBanMap.entrySet().removeIf(sessionEntry -> {
            Map<ActionType, LocalDateTime> inner = sessionEntry.getValue();

            inner.entrySet().removeIf(e -> e.getValue().isBefore(now));

            return inner.isEmpty();
        });
    }

}
