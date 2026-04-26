package com.example.bbs.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 を計算するためのユーティリティクラス。
 *
 * - CookieSigner（署名生成）
 * - CookieSignatureValidator（署名検証）
 *
 * の両方で共通して使われる「HMAC 計算の中核」。
 *
 * HMAC の計算は以下の流れ：
 * 1. 秘密鍵（key）をバイト列として SecretKeySpec に包む
 * 2. Mac(HmacSHA256) に鍵をセット
 * 3. value（メッセージ）をバイト列で渡して HMAC を計算
 *
 * 戻り値は「生の HMAC バイト列（32 bytes）」。
 * Base64URL への変換は呼び出し側が行う。
 */
public final class HmacUtil {

    private HmacUtil() {
        // インスタンス化禁止（ユーティリティクラス）
    }

    /**
     * HMAC-SHA256 を計算して生のバイト列を返す。
     *
     * @param key   秘密鍵（UTF-8 で byte[] 化されたもの）
     * @param value 署名対象のデータ（UTF-8 で byte[] 化されたもの）
     * @return HMAC-SHA256 の生バイト列（32 bytes）
     */
    public static byte[] hmacSha256(byte[] key, byte[] value) {
        try {
            // HMAC-SHA256 の Mac インスタンスを取得
            Mac mac = Mac.getInstance("HmacSHA256");

            // 秘密鍵を HMAC 用の鍵として設定
            SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");
            mac.init(keySpec);

            // value に対する HMAC を計算（生のバイト列）
            return mac.doFinal(value);

        } catch (Exception e) {
            throw new RuntimeException("HMAC calculation failed", e);
        }
    }
}
