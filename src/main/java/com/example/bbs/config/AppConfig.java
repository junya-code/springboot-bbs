package com.example.bbs.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        HmacProperties.class,
        CookieProperties.class
})
public class AppConfig {
}
