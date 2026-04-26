package com.example.bbs.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.bbs.config.CookieConfig;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.service.UserActionLogService;
import com.example.bbs.util.CookieUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final UserActionLogService userActionLogService;
    private final CookieSignatureValidator cookieSignatureValidator;

    public CustomLogoutSuccessHandler(UserActionLogService userActionLogService,
            CookieSignatureValidator cookieSignatureValidator) {
        this.userActionLogService = userActionLogService;
        this.cookieSignatureValidator = cookieSignatureValidator;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        HttpSession session = request.getSession(false);

        if (authentication != null) {

            // LoginUser を取り出す
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            Long userId = loginUser.getId();

            // BrowserSessionFilter がセットした UUID（36文字）
            String sessionId = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);

            BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
            if (botStatus == null) {
                botStatus = BotStatus.HUMAN;
            }

            if (sessionId == null) {
                // Cookie から署名付き値を取得
                String signed = CookieUtil.findCookie(request, CookieConfig.BROWSER_SESSION_COOKIE_NAME);

                // 署名を検証して UUID を抽出（署名不正なら null）
                sessionId = cookieSignatureValidator.validateAndExtract(signed);
            }

            // UUID のみを session_id として保存
            userActionLogService.logActionSuccess(
                    request,
                    sessionId,
                    userId,
                    ActionType.LOGOUT,
                    HttpMethodType.POST, // ← ログアウトは必ず POST
                    botStatus);
        }

        if (session != null) {
            session.invalidate();
        }

        response.sendRedirect("/auth/login?logout");
    }
}
