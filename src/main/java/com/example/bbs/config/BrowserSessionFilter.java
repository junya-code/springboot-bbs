package com.example.bbs.config;

import com.example.bbs.security.CookieSignatureValidator;
import com.example.bbs.security.CookieSigner;
import com.example.bbs.service.UserSessionService;
import com.example.bbs.util.CookieUtil;
import com.example.bbs.util.StaticPathUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.bbs.model.enums.BotStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ブラウザセッション管理用フィルタ。
 * * [設計上の注意]
 * Spring Security の内部フォワード（FORWARD）発生時などに、同一リクエスト内で
 * フィルタが複数回実行されるのを防ぐため、OncePerRequestFilter を継承しています。
 * これにより、DB更新（UserSessionService）が1リクエストにつき確実に1回のみ行われます。
 */

@Slf4j
public class BrowserSessionFilter extends OncePerRequestFilter {

    private final UserSessionService userSessionService;
    private final CookieSigner cookieSigner;
    private final CookieSignatureValidator validator;
    private final CookieProperties cookieProperties;

    public BrowserSessionFilter(
            UserSessionService userSessionService,
            CookieSigner cookieSigner,
            CookieSignatureValidator validator,
            CookieProperties cookieProperties) {

        this.userSessionService = userSessionService;
        this.cookieSigner = cookieSigner;
        this.validator = validator;
        this.cookieProperties = cookieProperties;
    }

    // 静的ファイル・OPTIONS・エラー はログ対象外
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return StaticPathUtil.shouldSkip(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // OncePerRequestFilter により、内部フォワード時（認証リダイレクト等）の
        // 二重実行は自動的に防止されます。ここでは1リクエスト1回限りの主処理を記述。

        String path = request.getRequestURI();

        // WordPress系スキャンBOTの拒否
        String[] blockedPatterns = { ".php" };
        for (String pattern : blockedPatterns) {
            if (path.contains(pattern)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return; // ここで終了
            }
        }

        // UUIDの取得・生成
        String sessionUuid = resolveOrCreateUuid(request, response);

        // セッションの永続化
        LocalDateTime now = LocalDateTime.now();
        userSessionService.updateOrCreate(sessionUuid, now);

        // 後続フィルタ（RateLimitFilter等）で使えるように属性セット
        request.setAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME, sessionUuid);

        int port = request.getRemotePort();
        request.setAttribute("REMOTE_PORT", port);

        filterChain.doFilter(request, response);
    }

    private String resolveOrCreateUuid(HttpServletRequest request, HttpServletResponse response) {
        String signedValue = CookieUtil.findCookie(request, CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        if (signedValue != null) {

            // UUIDを偽装していないか署名で確認
            String extracted = validator.validateAndExtract(signedValue);
            if (extracted != null) {
                request.setAttribute("IS_BOT", BotStatus.HUMAN);
                return extracted;
            }
        }

        // 新規発行
        String newUuid = UUID.randomUUID().toString();
        String newSignedValue = cookieSigner.sign(newUuid);

        Cookie cookie = new Cookie(CookieConfig.BROWSER_SESSION_COOKIE_NAME, newSignedValue);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieProperties.secure());// ★ HTTPS のみで送信
        cookie.setMaxAge(CookieConfig.BROWSER_SESSION_MAX_AGE); // ← クライアント側の Cookie の寿命を設定
        response.addCookie(cookie);

        // UUIDを毎リクエスト偽装するBOTと新規の人間を区別できないため、一旦最初はBOT判定。
        // 次回のリクエストで人間は同じUUIDと署名のはずなので実害はない。
        request.setAttribute("IS_BOT", BotStatus.BOT);

        return newUuid;
    }
}