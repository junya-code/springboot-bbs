package com.example.bbs.integration;

import com.example.bbs.dto.UserRegisterForm;
import com.example.bbs.infrastructure.UserActionLogCleanupEventInitializer;
import com.example.bbs.infrastructure.UserSessionCleanupEventInitializer;
import com.example.bbs.model.User;
import com.example.bbs.service.EmailService;
import com.example.bbs.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserActionLogCleanupEventInitializer userActionLogCleanupEventInitializer;

    @MockitoBean
    private UserSessionCleanupEventInitializer userSessionCleanupEventInitializer;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("ユーザー登録：Form経由で正しく保存されること")
    void testRegisterUser() {
        // 1. 準備：Formオブジェクトの作成
        UserRegisterForm form = new UserRegisterForm();
        form.setUsername("new_tester");
        form.setPassword("password123");

        // 2. 実行
        User savedUser = userService.registerUser(form);

        // 3. 検証
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("new_tester");
        // パスワードがハッシュ化されていることも確認（生パスワードではないこと）
        assertThat(savedUser.getPassword()).startsWith("$2a$");
    }

    @Test
    @DisplayName("アカウントロック：失敗を繰り返すとロック日時がセットされること")
    void testLoginFailureLock() {
        // 1. ユーザー作成
        UserRegisterForm form = new UserRegisterForm();
        form.setUsername("fail_user");
        form.setPassword("pass");
        User user = userService.registerUser(form);

        // 2. 実行：Serviceのメソッドを複数回呼ぶ（設定値の回数分）
        // SpamConfig.LOGIN_FAIL_THRESHOLD が 3 と仮定して3回
        userService.increaseLoginFail(user.getId());
        userService.increaseLoginFail(user.getId());
        userService.increaseLoginFail(user.getId());
        userService.increaseLoginFail(user.getId());
        userService.increaseLoginFail(user.getId());

        // 3. 検証：最新の状態を取得
        User updatedUser = userService.findByUsername("fail_user").orElseThrow();
        assertThat(updatedUser.getLoginFailCount()).isEqualTo(5);
        assertThat(updatedUser.getLockUntil()).isAfter(java.time.LocalDateTime.now());
    }

    @Test
    @DisplayName("ロック解除：リセットメソッドで回数と日時がクリアされること")
    void testResetLoginFailure() {
        // 1. ロックされたユーザーを準備
        UserRegisterForm form = new UserRegisterForm();
        form.setUsername("reset_user");
        form.setPassword("pass");
        User user = userService.registerUser(form);
        userService.increaseLoginFail(user.getId());

        // 2. 実行：リセット
        userService.resetLoginFail(user.getId());

        // 3. 検証
        User resetUser = userService.findByUsername("reset_user").orElseThrow();
        assertThat(resetUser.getLoginFailCount()).isEqualTo(0);
        assertThat(resetUser.getLockUntil()).isNull();
    }
}