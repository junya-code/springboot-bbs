package com.example.bbs.infrastructure;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

// プロキシがちゃんと X-Forwarded-For / X-Real-IP を付けてくれている前提で、
// できるだけ“元のクライアントIP”を取るための実装
@Component
public class ClientIpResolve {
    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
