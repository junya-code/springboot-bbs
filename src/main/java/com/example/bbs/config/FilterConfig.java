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
