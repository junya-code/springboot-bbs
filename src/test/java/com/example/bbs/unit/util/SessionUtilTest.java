package com.example.bbs.unit.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.example.bbs.model.UserSession;
import com.example.bbs.util.SessionUtil;

class SessionUtilTest {

    @Test
    void daysUntilExpire_30日前が最終ログインの場合_残り約60日を返す() {
        // Arrange
        UserSession session = new UserSession();
        // 30日前の日時をセット
        session.setLastSeenAt(LocalDateTime.now().minusDays(30));

        // Act
        long days = SessionUtil.daysUntilExpire(session);

        // Assert
        // 有効期限が90日の計算であれば、90 - 30 = 60日前後になるはず
        assertTrue(days >= 59 && days <= 60, "残り日数が計算通りであること");
    }
}