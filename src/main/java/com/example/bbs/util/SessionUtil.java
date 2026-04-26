package com.example.bbs.util;

import java.time.LocalDateTime;

import com.example.bbs.config.SessionConfig;
import com.example.bbs.model.UserSession;

public class SessionUtil {

    private SessionUtil() {
        // インスタンス化禁止（ユーティリティクラス）
    }

    private static final int LAST_SEEN_UPDATE_THRESHOLD_MINUTES = 1;

    /**
     * 現状の設計ではセッション期限切れ判定は
     * DB 側（クエリ or バッチ）で処理しているため未使用。
     * 将来的にアプリ側でセッション状態を扱う場合に備えて残している。
     */
    public static boolean isExpired(UserSession session) {
        if (session == null || session.getLastSeenAt() == null) {
            return true;
        }
        return session.getLastSeenAt()
                .isBefore(LocalDateTime.now().minusDays(SessionConfig.SESSION_EXPIRE_DAYS));
    }

    // セッションの「残り寿命」を返す（デバッグ用）
    public static long daysUntilExpire(UserSession session) {
        if (session == null || session.getLastSeenAt() == null) {
            return 0;
        }
        return LocalDateTime.now()
                .until(session.getLastSeenAt().plusDays(SessionConfig.SESSION_EXPIRE_DAYS),
                        java.time.temporal.ChronoUnit.DAYS);
    }

    // lastSeenAt を更新する必要があるか（1分以内の連続アクセスは更新しない）
    public static boolean shouldUpdateLastSeen(UserSession session) {
        if (session == null || session.getLastSeenAt() == null) {
            return true;
        }
        return session.getLastSeenAt()
                .isBefore(LocalDateTime.now().minusMinutes(LAST_SEEN_UPDATE_THRESHOLD_MINUTES));

    }
}
