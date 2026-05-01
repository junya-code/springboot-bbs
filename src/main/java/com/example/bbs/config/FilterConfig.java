package com.example.bbs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import com.example.bbs.infrastructure.ClientIpResolve;
import com.example.bbs.service.UserActionLogService;

@Configuration
public class FilterConfig {

    /**
     * UARejectFilter（最優先）
     * 
     * Go-http-client などの悪質 BOT を最速で拒否する
     * BrowserSessionFilter より前に置くことで、
     * user_action_logs に記録されるのを防ぐ
     * Cookie 発行や UserSession 作成も行われない
     */
    @Bean
    public FilterRegistrationBean<UARejectFilter> uaRejectFilter() {
        FilterRegistrationBean<UARejectFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UARejectFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(0); // ★ 一番最初に実行
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(ClientIpResolve clientIpResolve) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();

        // RateLimitFilter
        // レート制限は Security の内部処理（認証エラーや内部リダイレクト）を
        // カウントに含めると誤BANの原因になる。
        // Security の内部処理後に確実に配置する。
        // Ordered.LOWEST_PRECEDENCE は、そのための特別な値（絶対最後）を表す。

        registration.setFilter(new RateLimitFilter(clientIpResolve));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 1);

        return registration;
    }

    @Bean
    public FilterRegistrationBean<ActionLogFilter> actionLogFilterRegistration(
            UserActionLogService userActionLogService,
            ClientIpResolve clientIpResolve) {

        FilterRegistrationBean<ActionLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ActionLogFilter(userActionLogService));
        registration.addUrlPatterns("/*");

        // RateLimitFilter より後ろに置く
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);

        return registration;
    }

}

// ===========================================================================================

// アプリ全体のフィルター内訳（合計 約19個の構成）

// 【外側の世界：6個】

// uaRejectFilter (カスタム: Order 0)

// CharacterEncodingFilter (Springデフォルト)

// FormContentFilter (Springデフォルト)

// RequestContextFilter (Springデフォルト)

// springSecurityFilterChain (内側の13個を包む親)

// rateLimitFilter (カスタム: Order Lowest - 1)

// actionLogFilter (カスタム: Order Lowest)
// ※厳密にはTomcat内部用がさらにもう1つあるかもしれませんが、意識すべきは上の6〜7個です。

// 【内側の世界：13個】

// Security標準の12個（CSRFや認証、認可など）

// BrowserSessionFilter (カスタム: 7番目に挿入)

// これらを足すと、外側 6個 + 内側 13個 = 19個 という計算になります。

// ===========================================================================================

// 内側の世界（SecurityFilterChain）の13個の内訳

// 1 DisableEncodeUrlFilterセッションIDがURLに漏れるのを防ぐ、
// 地味だけど大事な初動処理。
//
// 2 WebAsyncManagerIntegrationFilter非同期リクエスト
// （Callableなど）でもセキュリティ情報を引き継げるようにする。

// 3 SecurityContextHolderFilterセッションから
// 認証情報（誰がログインしているか）を読み出す最重要拠点。

// 4 HeaderWriterFilterX-Frame-Options など、
// ブラウザ向けのセキュリティヘッダーを付与する。

// 5 CsrfFilterCSRF攻撃を防ぐ。

// 6 LogoutFilter/auth/logout へのリクエストかどうかを監視。
// 違えば即スルー。

// 7 BrowserSessionFilter【カスタム】
// ここでSQL実行！Cookieの署名検証やセッションの生存確認を行うあなたのアプリの心臓部。

// 8 UsernamePasswordAuthenticationFilterログイン実行
// （POST /auth/login-processing）を待ち構えている。

// 9 RequestCacheAwareFilterログイン成功後、
// 「さっきアクセスしようとしたページ」へ戻すためのキャッシュ処理。

// 10 SecurityContextHolderAwareRequestFilterHttpServletRequest に
// getUserPrincipal() などのSpring Security機能を統合。

// 11 AnonymousAuthenticationFilter未ログインなら
// 「匿名ユーザー（ROLE_ANONYMOUS）」という仮の身分を与える。

// 12 ExceptionTranslationFilter後ろでエラー（認証・認可不足）が起きたとき、
// ログイン画面へ飛ばす等の後始末役。

// 13 uthorizationFilter【最終門番】
// SecurityConfig で書いた requestMatchers("/auth/login").permitAll() などの
// ルールに基づき、通していいか最終判断する。