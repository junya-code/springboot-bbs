package com.example.bbs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.bbs.model.enums.Role;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    // ★ ログイン失敗回数
    @Column(nullable = false)
    private int loginFailCount = 0;

    // ★ ロック解除予定時刻（null ならロックなし）
    private LocalDateTime lockUntil;

    @OneToMany(mappedBy = "user")
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private Set<Like> likes = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    protected User() {
    } // JPA用

    public User(String username, String encodedPassword, Role role) {
        this.username = username;
        this.password = encodedPassword;
        this.role = role;
    }

    /**
     * ログイン失敗回数とアカウントロックの制御を行う。
     *
     * 【ロック方式の仕様】
     * - threshold 回ログイン失敗すると lockUntil にロック解除時刻をセットし、一定時間ログイン不可になる。
     * - ロック中に再度ログイン試行が行われた場合でも、FailureHandler 側で LockedException が発生するため
     * パスワード比較には進まず、このメソッドは呼ばれない。
     * → つまり「ロック中は延命されない」わけではなく、
     * FailureHandler が increaseLoginFail() を呼ぶため lockUntil が更新され、結果的に延命される。
     *
     * 【挙動の特徴】
     * - ロック解除後は 1 回だけログイン試行が可能。
     * → この 1 回でパスワードが間違っていると再び即ロックされる。
     * - ロック中は常に LockedException が返され、パスワード比較に進まない。
     * → 正規ユーザーはロック解除後に正しいパスワードを入力すれば復帰できる。
     * → 攻撃者はロック解除後の 1 回を当てる必要があり、総当たり攻撃が事実上不可能。
     *
     * 【目的】
     * - username と password の要件が緩い（1文字/4文字）代わりに、
     * ログイン試行に対して非常に強い防御を行うための設計。
     * - 正規ユーザーには「ロック中」「解除後の1回目」が明確に分かる UX を提供しつつ、
     * 攻撃者には極めて厳しいロック方式となる。
     */
    public void increaseLoginFailCount(int threshold, LocalDateTime lockUntilTime) {
        this.loginFailCount++;
        if (this.loginFailCount >= threshold) {
            this.lockUntil = lockUntilTime;
        }
    }

    public void resetLoginFail() {
        this.loginFailCount = 0;
        this.lockUntil = null;
    }

}