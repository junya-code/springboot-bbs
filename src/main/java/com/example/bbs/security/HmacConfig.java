package com.example.bbs.security;

import com.example.bbs.config.HmacProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HmacConfig {

    private final HmacProperties hmacProperties;

    public HmacConfig(HmacProperties hmacProperties) {
        this.hmacProperties = hmacProperties;
    }

    @Bean
    public CookieSigner cookieSigner() {
        return new CookieSigner(hmacProperties.secret());
    }

    @Bean
    public CookieSignatureValidator cookieSignatureValidator() {
        return new CookieSignatureValidator(hmacProperties.secret());
    }
}