package com.example.bbs.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class PreLogoutCleanupHandler implements LogoutHandler {

    @Override
    public void logout(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        HttpSession session = request.getSession(false);
        if (session != null) {
            // ★ ログアウト由来のセッション破棄であることを示すフラグ
            session.setAttribute("LOGOUT_FLAG", true);

            // ★ SESSION_EXPIRED を防ぐために LOGIN_USER を削除
            session.removeAttribute("LOGIN_USER");
        }
    }
}
