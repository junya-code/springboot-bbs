package com.example.bbs.config;

import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.security.LoginUser;
import com.example.bbs.service.UserActionLogService;
import com.example.bbs.util.StaticPathUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

public class ActionLogFilter implements Filter {

    private final UserActionLogService userActionLogService;

    public ActionLogFilter(UserActionLogService userActionLogService) {
        this.userActionLogService = userActionLogService;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // 静的ファイル・OPTIONS・エラー はログ対象外
        if (StaticPathUtil.shouldSkip(request)) {
            chain.doFilter(request, response);
            return;
        }

        // まず後続処理
        chain.doFilter(request, response);

        // BrowserSessionFilter がセットした UUID を取得
        String sessionUuid = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);
        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null)
            botStatus = BotStatus.UNKNOWN;

        // ログインユーザー取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof LoginUser loginUser) {
            userId = loginUser.getId();
        }

        // ★ アクション種別（enum 化）
        ActionType action = switch (request.getMethod()) {
            case "GET" -> ActionType.PAGE_VIEW;
            case "POST", "DELETE", "PUT", "PATCH" -> ActionType.REQUEST;
            default -> ActionType.OTHER;
        };

        // ★ HTTP メソッド（enum 化）
        HttpMethodType method = switch (request.getMethod()) {
            case "GET" -> HttpMethodType.GET;
            case "POST" -> HttpMethodType.POST;
            case "DELETE" -> HttpMethodType.DELETE;
            case "PUT" -> HttpMethodType.PUT;
            case "PATCH" -> HttpMethodType.PATCH;
            default -> HttpMethodType.OTHER;
        };

        // ログ保存（正常系のみ）
        userActionLogService.logActionSuccess(
                request,
                sessionUuid,
                userId,
                action,
                method,
                botStatus);
    }
}
