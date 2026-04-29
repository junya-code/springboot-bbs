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
        // substring(数値) は「先頭からその数値ぶんを切り落として、残り全部を取り出す」メソッド
        String path = uri.substring(ctx.length());

        if (path.startsWith("/error")) {
            return true;
        }

        // A || B は “A も B も false のときだけ false、それ以外は true” になる
        return isStaticPath(path)
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
