package com.example.bbs.service;

import java.time.LocalDateTime;
import java.net.URI;

import org.springframework.stereotype.Service;

import com.example.bbs.infrastructure.ClientIpResolve;
import com.example.bbs.model.UserActionLog;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import com.example.bbs.repository.UserActionLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UserActionLogService {

    private final UserActionLogRepository repo;
    private final ClientIpResolve clientIpResolve;

    public UserActionLogService(UserActionLogRepository repo,
            ClientIpResolve clientIpResolve) {
        this.repo = repo;
        this.clientIpResolve = clientIpResolve;
    }

    public void saveUserIpAlways(
            String sessionId,
            Long userId,
            String ip,
            Integer remotePort,
            String ua,
            ActionType action,
            HttpMethodType method,
            String path,
            BotStatus botStatus) {

        UserActionLog log = new UserActionLog();
        log.setSessionId(sessionId);
        log.setUserId(userId);
        log.setIpAddress(ip);
        log.setRemotePort(remotePort);
        log.setUserAgent(ua);
        log.setAction(action);
        log.setMethod(method);
        log.setPath(path);
        log.setBotStatus(botStatus);

        repo.save(log);
    }

    // SESSION_EXPIRED 専用メソッド
    public void saveSessionExpired(String sessionId, Long userId) {

        UserActionLog log = new UserActionLog();
        log.setSessionId(sessionId);
        log.setUserId(userId);

        // SESSION_EXPIRED はサーバー側の自動イベント
        log.setAction(ActionType.SESSION_EXPIRED);
        log.setMethod(HttpMethodType.SYSTEM);
        log.setPath("/system/session-expired");

        // IP / UA は取得できないので null
        log.setIpAddress(null);
        log.setUserAgent(null);
        log.setBotStatus(BotStatus.UNKNOWN);

        repo.save(log);
    }

    public long countRecentAction(Long userId, ActionType action, int hours) {
        return repo.countRecentAction(
                userId,
                action,
                LocalDateTime.now().minusHours(hours));
    }

    public void logActionSuccess(
            HttpServletRequest request,
            String sessionId,
            Long userId,
            ActionType action,
            HttpMethodType method,
            BotStatus botStatus) {

        String ip = clientIpResolve.getClientIp(request);
        String ua = request.getHeader("User-Agent");

        Integer remotePort = (Integer) request.getAttribute("REMOTE_PORT");
        // Spring Security の LogoutSuccessHandler では
        // request attribute が消えている場合があるため
        if (remotePort == null) {
            remotePort = request.getRemotePort();
        }

        // 内部フォワード（認証チェックでのログイン画面遷移など）を考慮し、元のパスを取得
        String path = (String) request.getAttribute("jakarta.servlet.forward.request_uri");
        if (path == null) {
            // 404/500エラーなどのエラーページ遷移の場合
            path = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        }
        // 2. 未ログインでアクセスし、ログイン画面へリダイレクトされた場合の「本来のURL」をセッションから取得
        // Spring Security は認証が必要なページへのアクセスを遮断した際、その情報をセッションに保存します。
        // ログイン画面を「表示（GET）」した時のみ、どこから飛ばされてきたかを記録するように限定します。
        String servletPath = request.getServletPath();
        if (path == null && method == HttpMethodType.GET && "/auth/login".equals(servletPath)) {
            // removeRequest メソッドを使用するために具象クラスをインスタンス化
            HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, null);
            if (savedRequest != null) {
                String redirectUrl = savedRequest.getRedirectUrl();

                // 一度ログ判定に使用した SavedRequest は、パースの成否に関わらず削除する
                // これにより、パースエラー時でも無限ループ（居残りログ）を防げる
                requestCache.removeRequest(request, null);

                if (redirectUrl != null) {
                    try {
                        // フルURLからパス部分とクエリ部分だけを抜き出す
                        URI uri = new URI(redirectUrl);
                        path = uri.getRawPath();
                        if (uri.getRawQuery() != null) {
                            path += "?" + uri.getRawQuery();
                        }
                    } catch (Exception e) {
                        path = redirectUrl; // 解析失敗時はフォールバック
                    }
                }
            }
        }

        // 3. 通常のリクエストの場合（クエリパラメータ ?q=... 等も含めて記録）
        if (path == null) {
            // getRequestURI() ではなく getServletPath() を使うと、
            // コンテキストパスを除いた「/posts」のような綺麗なパスが記録される
            path = request.getServletPath();
            String queryString = request.getQueryString();
            if (queryString != null) {
                path += "?" + queryString;
            }
        }

        saveUserIpAlways(
                sessionId,
                userId,
                ip,
                remotePort,
                ua,
                action,
                method,
                path,
                botStatus);
    }

}
