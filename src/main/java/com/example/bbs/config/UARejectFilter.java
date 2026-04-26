package com.example.bbs.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * UARejectFilter
 *
 * 悪質 BOT（特に Go-http-client/1.1）を最速で拒否するフィルタ。
 *
 * - BrowserSessionFilter より前に実行される（FilterConfig で order=0）
 * - user_action_logs に記録されない
 * - Cookie も発行されない
 * - UserSession も作られない
 * - RateLimitFilter にも到達しない
 *
 * つまり「最も軽いコストで BOT を即拒否する」ための専用フィルタ。
 */
public class UARejectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ua = request.getHeader("User-Agent");

        // Go 言語の標準 HTTP クライアントは 100% BOT
        if (ua != null && ua.startsWith("Go-http-client")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return; // ★ 即時拒否（ログもセッションも作らない）
        }

        chain.doFilter(req, res);
    }
}
