package com.example.bbs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hmac")
public record HmacProperties(String secret) {
}
