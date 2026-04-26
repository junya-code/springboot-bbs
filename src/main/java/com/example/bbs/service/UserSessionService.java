package com.example.bbs.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bbs.model.UserSession;
import com.example.bbs.repository.UserSessionRepository;
import com.example.bbs.util.SessionUtil;

@Service
public class UserSessionService {

    private final UserSessionRepository repo;

    public UserSessionService(UserSessionRepository repo) {
        this.repo = repo;
    }

    /**
     * セッションIDに対応する UserSession を作成または更新する。
     * - 存在しない場合は新規作成
     * - 存在する場合は lastSeenAt を必要に応じて更新
     */

    @Transactional
    public UserSession updateOrCreate(String sessionId, LocalDateTime now) {
        UserSession session = repo.findById(sessionId).orElse(null);

        if (session == null) {
            session = new UserSession();
            session.setId(sessionId);
            session.setCreatedAt(now);
            session.setLastSeenAt(now);
        } else {
            if (SessionUtil.shouldUpdateLastSeen(session)) {
                session.setLastSeenAt(now);
            }
        }

        return repo.save(session);
    }
}
