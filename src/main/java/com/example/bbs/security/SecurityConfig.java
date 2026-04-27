package com.example.bbs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.bbs.config.BrowserSessionFilter;
import com.example.bbs.config.CookieProperties;
import com.example.bbs.service.UserSessionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class SecurityConfig {
        private final PreLogoutCleanupHandler preLogoutCleanupHandler;
        private final CustomLoginSuccessHandler loginSuccessHandler;
        private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

        SecurityConfig(CustomLoginSuccessHandler loginSuccessHandler,
                        CustomLogoutSuccessHandler customLogoutSuccessHandler,
                        PreLogoutCleanupHandler preLogoutCleanupHandler) {
                this.loginSuccessHandler = loginSuccessHandler;
                this.customLogoutSuccessHandler = customLogoutSuccessHandler;
                this.preLogoutCleanupHandler = preLogoutCleanupHandler;
        }

        @Bean
        public BrowserSessionFilter browserSessionFilter(UserSessionService s, CookieSigner cs,
                        CookieSignatureValidator v, CookieProperties cp) {
                return new BrowserSessionFilter(s, cs, v, cp);
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        HttpSecurity http,
                        CustomAuthenticationProvider provider) throws Exception {

                return http.getSharedObject(AuthenticationManagerBuilder.class)
                                .authenticationProvider(provider)
                                .build();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        BrowserSessionFilter browserSessionFilter,
                        CustomAuthFailureHandler customAuthFailureHandler)
                        throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/auth/register", "/auth/login", "/contact/**",
                                                                "/posts/privacy", "/posts/terms",
                                                                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                                                                "/error", "/robots.txt")
                                                .permitAll()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/auth/login")
                                                .loginProcessingUrl("/auth/login-processing")
                                                .failureHandler(customAuthFailureHandler)
                                                .successHandler(loginSuccessHandler)
                                                .permitAll())
                                .logout(logout -> logout
                                                .addLogoutHandler(preLogoutCleanupHandler)
                                                .logoutUrl("/auth/logout")
                                                .logoutSuccessHandler(customLogoutSuccessHandler)
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .exceptionHandling(exception -> exception
                                                .accessDeniedPage("/error/403"));

                // ← ここで Security の内部フィルタチェーンに挿入する（必須）
                http.addFilterBefore(browserSessionFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // DB が漏れたときにユーザーを守るためパスワードをハッシュ化する
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
