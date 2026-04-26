package com.example.bbs.infrastructure;

import org.springframework.stereotype.Component;

import com.example.bbs.security.LoginUser;
import com.example.bbs.service.UserActionLogService;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

/**
 * SESSION_EXPIRED を記録する目的
 *
 * 本アプリでは ActionLogFilter により全リクエストを記録しているため、
 * user_action_logs から「最終アクセス時刻」を推定することはできる。
 *
 * しかし、静的ファイル取得・OPTIONS（プリフライト）・304再検証など、
 * doFilter を通らずにセッションだけが延命されるケースが存在する。
 * この場合、user_action_logs だけでは「セッションがいつ実際に終了したか」を
 * 正確に把握できない。
 *
 * SESSION_EXPIRED は「サーバ側で HttpSession が破棄された瞬間」を
 * 確定値として記録するためのログであり、
 * 以下のような場面で重要な意味を持つ：
 * - セッション延命ロジックの追加や変更時の検証
 * - タイムアウト制御の調整
 * - doFilter を通らないリクエストによる延命の有無の調査
 * - セッション管理の不具合解析や異常検知
 *
 * 現時点では分析・デバッグ用途の補助的ログだが、
 * 将来的なセッション管理の信頼性向上に寄与する基盤情報として扱う。
 */

@Component
public class SessionExpiredListener implements HttpSessionListener {

    private final UserActionLogService userActionLogService;

    public SessionExpiredListener(UserActionLogService userActionLogService) {
        this.userActionLogService = userActionLogService;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {

        HttpSession session = se.getSession();

        // ★ ログインユーザーがいなければ何もしない（BOT対策）
        LoginUser loginUser = (LoginUser) session.getAttribute("LOGIN_USER");
        if (loginUser == null) {
            return;
        }

        Boolean logoutFlag = (Boolean) session.getAttribute("LOGOUT_FLAG");
        if (logoutFlag != null && logoutFlag) {
            return; // ログアウト由来の破棄は無視
        }

        String uuid = (String) session.getAttribute("BROWSER_SESSION_UUID");

        Long userId = loginUser.getId();

        // ★ SESSION_EXPIRED を保存
        userActionLogService.saveSessionExpired(uuid, userId);
    }
}
