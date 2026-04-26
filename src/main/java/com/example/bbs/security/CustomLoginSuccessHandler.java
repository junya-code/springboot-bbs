package com.example.bbs.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.bbs.config.CookieConfig;
import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;
import com.example.bbs.service.UserActionLogService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserActionLogService userActionLogService;

    public CustomLoginSuccessHandler(UserActionLogService userActionLogService) {
        this.userActionLogService = userActionLogService;
        setDefaultTargetUrl("/posts");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
            HttpServletResponse res,
            Authentication auth)
            throws IOException, ServletException {

        String sessionId = (String) req.getAttribute(CookieConfig.BROWSER_SESSION_COOKIE_NAME);

        // HttpSession（ヒープメモリ）
        req.getSession().setAttribute("LOGIN_USER", auth.getPrincipal());
        req.getSession().setAttribute("BROWSER_SESSION_UUID", sessionId);

        Long userId = ((LoginUser) auth.getPrincipal()).getId();

        BotStatus botStatus = (BotStatus) req.getAttribute("IS_BOT");
        if (botStatus == null) {
            botStatus = BotStatus.HUMAN;
        }

        userActionLogService.logActionSuccess(
                req,
                sessionId,
                userId,
                ActionType.LOGIN_SUCCESS,
                HttpMethodType.POST, // ログイン成功は必ず POST
                botStatus);

        req.getSession().setAttribute("successMessage", "flash.login.success");

        super.onAuthenticationSuccess(req, res, auth);
    }
}
