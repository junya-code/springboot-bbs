package com.example.bbs.config;

public class RateLimitConfig {

    // 1秒あたりの許可リクエスト数（人間）
    public static final int HUMAN_REQUESTS_PER_SECOND = 7;

    // BOT の許可リクエスト数(Googlebot などのクロールを許したいため3以上で)
    public static final int BOT_REQUESTS_PER_SECOND = 3;

    // UNKNOWN（Cookie 拒否・Cookie 壊れ・Filter 非通過など）向けの中間レート。
    // HUMAN ほど信用できないが BOT と断定もできないため、IP ベースで適度に制限する。
    // 障害時に全ユーザーが BOT 扱いになる誤BANを防ぎつつ、安全側に倒す値。
    public static final int UNKNOWN_REQUESTS_PER_SECOND = 5;

    // レート制限の時間窓（ミリ秒）
    public static final long WINDOW_MS = 1000;

    // BAN時間（ミリ秒）
    public static final long BAN_DURATION_MS = 60 * 10 * 1000; // 10分

    // HTTP ステータス
    public static final int HTTP_TOO_MANY_REQUESTS = 429;

    // BAN メッセージ
    public static final String BAN_MESSAGE = "Too many requests. You are banned for 10 minutes.";
}
