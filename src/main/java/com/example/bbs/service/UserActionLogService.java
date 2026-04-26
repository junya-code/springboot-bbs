package com.example.bbs.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.bbs.infrastructure.ClientIpResolve;
import com.example.bbs.model.UserActionLog;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.repository.UserActionLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UserActionLogService {

    private final UserActionLogRepository repo;
    private final ClientIpResolve clientIpResolve;

    public UserActionLogService(UserActionLogRepository repo,
            ClientIpResolve clientIpResolve) {
        this.repo = repo;
        this.clientIpResolve = clientIpResolve;
    }

    public void saveUserIpAlways(
            String sessionId,
            Long userId,
            String ip,
            Integer remotePort,
            String ua,
            ActionType action,
            HttpMethodType method,
            String path,
            BotStatus botStatus) {

        UserActionLog log = new UserActionLog();
        log.setSessionId(sessionId);
        log.setUserId(userId);
        log.setIpAddress(ip);
        log.setRemotePort(remotePort);
        log.setUserAgent(ua);
        log.setAction(action);
        log.setMethod(method);
        log.setPath(path);
        log.setBotStatus(botStatus);

        repo.save(log);
    }

    // ★ SESSION_EXPIRED 専用メソッド
    public void saveSessionExpired(String sessionId, Long userId) {

        UserActionLog log = new UserActionLog();
        log.setSessionId(sessionId);
        log.setUserId(userId);

        // SESSION_EXPIRED はサーバー側の自動イベント
        log.setAction(ActionType.SESSION_EXPIRED);
        log.setMethod(HttpMethodType.SYSTEM);
        log.setPath("/system/session-expired");

        // IP / UA は取得できないので null
        log.setIpAddress(null);
        log.setUserAgent(null);
        log.setBotStatus(BotStatus.UNKNOWN);

        repo.save(log);
    }

    public long countRecentAction(Long userId, ActionType action, int hours) {
        return repo.countRecentAction(
                userId,
                action,
                LocalDateTime.now().minusHours(hours));
    }

    public void logActionSuccess(
            HttpServletRequest request,
            String sessionId,
            Long userId,
            ActionType action,
            HttpMethodType method,
            BotStatus botStatus) {

        String ip = clientIpResolve.getClientIp(request);
        String ua = request.getHeader("User-Agent");

        Integer remotePort = (Integer) request.getAttribute("REMOTE_PORT");
        // Spring Security の LogoutSuccessHandler では
        // request attribute が消えている場合があるため
        if (remotePort == null) {
            remotePort = request.getRemotePort();
        }

        saveUserIpAlways(
                sessionId,
                userId,
                ip,
                remotePort,
                ua,
                action,
                method,
                request.getRequestURI(),
                botStatus);
    }

}
