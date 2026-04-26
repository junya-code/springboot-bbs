package com.example.bbs.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.bbs.infrastructure.ClientIpResolve;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.util.StaticPathUtil;

@Slf4j
public class RateLimitFilter implements Filter {

    // Java では new で作られたオブジェクトは通常ヒープに置かれる。
    // ただし、JIT の最適化（Escape Analysis）により、
    // 逃げない小さなオブジェクトはヒープに置かれない場合もある。
    // ConcurrentHashMap のような共有オブジェクトは必ずヒープ。

    // UUIDごとの直近1秒のリクエスト数
    private final Map<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();

    // BANされたUUIDと解除時刻
    private final Map<String, Long> bannedKeys = new ConcurrentHashMap<>();

    private final ClientIpResolve clientIpResolve;

    public RateLimitFilter(ClientIpResolve clientIpResolve) {
        this.clientIpResolve = clientIpResolve;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // Servlet API の Filter は歴史的に「HTTP 以外のプロトコルも扱える」設計のため、
        // Tomcat は ServletRequest / ServletResponse 型で渡してくる。
        // ただし実体は常に HttpServletRequest / HttpServletResponse。
        // そのため HTTP 情報（パス、ヘッダ、Cookie、IP など）を扱うにはキャストが必要。
        //
        // 一方、Spring Security のフィルター（OncePerRequestFilterを利用しているBrowserSessionFilter
        // など）は
        // Spring が内部で ServletRequest を HttpServletRequest にキャストしてから渡してくれるため、
        // doFilterInternal() では最初から HttpServletRequest が使える。
        // → 純粋な Servlet Filter（このクラスなど）だけがキャストを必要とする。

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        long now = Instant.now().toEpochMilli();

        // 静的ファイル・OPTIONS・エラー はログ対象外
        if (StaticPathUtil.shouldSkip(request)) {
            chain.doFilter(request, response);
            return;
        }

        // 署名付き Cookie を取得
        String uuid = (String) request.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);

        BotStatus botStatus = (BotStatus) request.getAttribute("IS_BOT");
        if (botStatus == null) {
            botStatus = BotStatus.UNKNOWN;
        }

        // レート制限キー（人間は UUID、BOTとUNKNOWN は IP。IPでの制限は公共Wi-Fiやマンションなどでは他のユーザーもBANとなる問題あり）
        String key = (botStatus == BotStatus.HUMAN)
                ? uuid
                : clientIpResolve.getClientIp(request);

        // BOT と人間でレート制限を変える
        int limit = switch (botStatus) {
            case HUMAN -> RateLimitConfig.HUMAN_REQUESTS_PER_SECOND;
            case UNKNOWN -> RateLimitConfig.UNKNOWN_REQUESTS_PER_SECOND;
            case BOT -> RateLimitConfig.BOT_REQUESTS_PER_SECOND;
        };

        // 古いカウンタ削除（1秒以上アクセスなし）
        requestCounters.entrySet().removeIf(entry -> now - entry.getValue().timestamp > RateLimitConfig.WINDOW_MS);

        // 古いBAN削除（期限切れ）
        bannedKeys.entrySet().removeIf(entry -> entry.getValue() < now);

        // ③ BAN チェック(IPを偽装するBOTには効果なし)
        if (bannedKeys.containsKey(key)) {
            response.setStatus(RateLimitConfig.HTTP_TOO_MANY_REQUESTS);
            response.getWriter().write(RateLimitConfig.BAN_MESSAGE);
            return;
        }

        // ④ レート制限カウンタ更新
        RequestCounter counter = requestCounters.computeIfAbsent(key, k -> new RequestCounter());
        synchronized (counter) {
            if (now - counter.timestamp < RateLimitConfig.WINDOW_MS) {
                counter.count++;
            } else {
                counter.timestamp = now;
                counter.count = 1;
            }

            if (counter.count > limit) {
                long banUntil = now + RateLimitConfig.BAN_DURATION_MS;
                bannedKeys.put(key, banUntil);

                log.warn("RateLimitFilter: BAN triggered. key={}, banUntil={}, botStatus={}",
                        key, banUntil, botStatus);

                response.setStatus(RateLimitConfig.HTTP_TOO_MANY_REQUESTS);
                response.getWriter().write(RateLimitConfig.BAN_MESSAGE);
                return;
            }
        }

        // ⑤ 通常処理
        chain.doFilter(req, res);
    }

    // カウンタ構造体
    private static class RequestCounter {
        long timestamp = Instant.now().toEpochMilli();
        int count = 0;
    }
}
