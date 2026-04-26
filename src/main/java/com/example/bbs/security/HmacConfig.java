package com.example.bbs.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HmacConfig {

    @Value("${hmac.secret}")
    private String secret;

    @Bean
    public CookieSigner cookieSigner() {
        return new CookieSigner(secret);
    }

    @Bean
    public CookieSignatureValidator cookieSignatureValidator() {
        return new CookieSignatureValidator(secret);
    }
}
