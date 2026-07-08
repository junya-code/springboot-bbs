package com.example.bbs.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public final class CookieUtil {

    private CookieUtil() {
    }

    /**
     * 指定された Cookie 名の値を返す。
     * 存在しない場合は null を返す。
     */
    public static String findCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName()) &&
                    c.getValue() != null &&
                    !c.getValue().isEmpty()) {
                return c.getValue();
            }
        }

        return null;
    }
}
