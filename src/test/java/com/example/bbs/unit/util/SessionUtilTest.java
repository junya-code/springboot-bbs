package com.example.bbs.unit.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.example.bbs.model.UserSession;
import com.example.bbs.util.SessionUtil;

class SessionUtilTest {

    @Test
    void daysUntilExpire_30日前が最終ログインの場合_残り約60日を返す() {

        UserSession session = new UserSession();
        // 30日前の日時をセット
        session.setLastSeenAt(LocalDateTime.now().minusDays(30));

        long days = SessionUtil.daysUntilExpire(session);

        // LocalDateTime.now()の実行タイミングによるミリ秒のズレを考慮し、59日〜60日の許容範囲で判定。
        assertThat(days)
                .as("残り日数が計算通りであること")
                .isBetween(59L, 60L);
    }
}