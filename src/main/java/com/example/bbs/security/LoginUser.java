package com.example.bbs.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import lombok.Getter;

import java.util.Collection;

@Getter
public class LoginUser extends User {
    private final Long id; // IDを追加

    // LoginUser は Spring Security の User を継承しているので、
    // アプリ側の User と名前が衝突する。
    // だからフルパス指定は必要。
    public LoginUser(com.example.bbs.model.User user,
            Collection<? extends GrantedAuthority> authorities) {
        super(user.getUsername(), user.getPassword(), authorities);
        this.id = user.getId();
    }
}