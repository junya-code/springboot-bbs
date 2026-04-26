package com.example.bbs.security;

import com.example.bbs.util.HmacUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Cookie の値に HMAC-SHA256 署名を付与するクラス。
 *
 * value → HMAC(secret, value) を計算し、
 * 「value.signature」という形式の文字列を返す。
 *
 * signature は Base64URL（padding なし）でエンコードされるため、
 * Cookie や URL に安全に載せられる。
 */
public class CookieSigner {

    private final byte[] secret;

    public CookieSigner(String secret) {
        // HMAC の鍵として UTF-8 のバイト列に変換して保持
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 値に対して HMAC-SHA256 を計算し、
     * 「value.signature」形式の署名付き文字列を返す。
     */
    public String sign(String value) {
        // HMAC-SHA256 の生バイト列を計算
        byte[] rawHmac = HmacUtil.hmacSha256(
                secret,
                value.getBytes(StandardCharsets.UTF_8));

        // Base64URL（padding なし）で文字列化
        String signature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawHmac);

        // value と signature を結合して返す
        return value + "." + signature;
    }
}
