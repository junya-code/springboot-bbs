package com.example.bbs.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.bbs.service.UserService;

// POST /auth/login-processing が来た瞬間に Spring Security が
// AuthenticationProvider を呼ぶ。
// 具体的には UsernamePasswordAuthenticationFilter が username/password を取り出し、
// AuthenticationManager（ProviderManager）に渡す。
// その際、登録されている CustomAuthenticationProvider が選ばれて authenticate() が実行される。
// このクラスを手書き実装している理由は、Spring Security のデフォルト認証
// （DaoAuthenticationProvider）では対応できない要件があるため。
// 具体的には「ログイン失敗回数のDB管理」「ロック判定」「成功時のfailCountリセット」
// 「監査ログ」「BOT対策」「警察協力のための追跡性」など、実務的な制御を行う必要があるため。

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public CustomAuthenticationProvider(
            CustomUserDetailsService userDetailsService,
            UserService userService,
            PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();

        // getCredentials() の戻り値は Spring Security の仕様で Object 型になっている。
        // 理由は、認証方式によって資格情報（credentials）の型が異なるため。
        // （パスワードは String だが、他の認証方式ではトークンや証明書など別の型になる）
        // そのため、パスワードとして扱う場合は toString() で文字列化している。
        String rawPassword = authentication.getCredentials().toString();

        // 必ず UserDetailsService を呼ぶ（ロック判定がここで発動）
        LoginUser user = (LoginUser) userDetailsService.loadUserByUsername(username);

        // パスワード比較(user.getPassword() → DB の BCrypt ハッシュ)
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }

        // ★ 成功したら失敗カウントをリセット
        userService.resetLoginFail(user.getId());

        // null（パスワードは消す）
        return new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        // SecurityContext に保存される。
    }

    // 「この Provider はどの種類の Authentication を処理できるか？」
    // を Spring Security に教えるためのスイッチ
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
