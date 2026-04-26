package com.example.bbs.security;

import java.io.IOException;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.example.bbs.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final UserService userService;

    public CustomAuthFailureHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        // Spring MVC の外なので @RequestParam は動かないので使えない
        String username = request.getParameter("username");

        // username が存在する場合のみカウント
        userService.findByUsername(username).ifPresent(user -> {
            userService.increaseLoginFail(user.getId());
        });

        // LockedExceptionはSpring Security が公式に用意している “アカウントロック専用の例外”
        // ★ ロック中のユーザーは専用エラーを返す
        if (exception instanceof LockedException) {
            setDefaultFailureUrl("/auth/login?locked");
        } else {
            // 通常のログイン失敗
            setDefaultFailureUrl("/auth/login?badCredentials");
        }

        request.getSession().setAttribute("loginFormUsername", username);

        super.onAuthenticationFailure(request, response, exception);
    }
}
