package com.example.bbs.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StaticPathUtil {

    private StaticPathUtil() {
        // インスタンス化禁止（ユーティリティクラス）
    }

    public static boolean isStaticPath(String path) {
        return path.equals("/favicon.ico") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/static/") ||
                path.startsWith("/webjars/");
    }

    public static boolean shouldSkip(HttpServletRequest request) {

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.substring(ctx.length());

        if (path.startsWith("/error")) {
            return true;
        }

        return isStaticPath(path)
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
