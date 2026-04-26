package com.example.bbs.security;

import com.example.bbs.util.HmacUtil;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CookieSignatureValidator {

    private final byte[] secret;

    public CookieSignatureValidator(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String validateAndExtract(String signedValue) {
        if (signedValue == null || !signedValue.contains(".")) {
            return null;
        }

        // value と signature に分割（2つだけ）
        String[] parts = signedValue.split("\\.", 2);
        if (parts.length != 2) {
            return null;
        }

        String value = parts[0];
        String signature = parts[1];

        // HMAC-SHA256 (32 bytes) → Base64URL (paddingなし) = 43文字
        if (signature.length() != 43) {
            return null;
        }

        // HMAC は “同じ鍵 × 同じ文字列” なら、必ず同じ署名になる。
        try {
            // サーバー側で再計算した署名（バイト列）
            byte[] expectedBytes = HmacUtil.hmacSha256(
                    secret,
                    value.getBytes(StandardCharsets.UTF_8));

            // クライアントから送られてきた署名（バイト列）
            byte[] actualBytes = Base64.getUrlDecoder().decode(signature);

            // // constant-time 比較をする理由は
            // 先頭のバイトが違う → すぐ false を返す（高速）
            // 途中まで一致 → 少し時間がかかる
            // 全部一致 → 最後まで比較するので最も時間がかかる
            // 攻撃者はこの「処理時間の差」を観測して、
            // どこまで一致しているか
            // 署名の何バイト目が正しいか
            // を推測できてしまう事を防ぐ。
            // とはいえ1msよりはるかに短い。

            if (!MessageDigest.isEqual(expectedBytes, actualBytes)) {
                return null;
            }

            return value;

        } catch (IllegalArgumentException e) {
            // Base64 decode に失敗 → 偽装
            return null;
        }
    }
}
