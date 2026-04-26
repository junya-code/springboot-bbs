package com.example.bbs.config;

public class SpamConfig {

    // クールダウン時間（時間）
    public static final int CREATE_POST_COOLDOWN_HOURS = 1;
    public static final int EDIT_POST_COOLDOWN_HOURS = 1;
    public static final int CREATE_COMMENT_COOLDOWN_HOURS = 1;
    public static final int SEND_CONTACT_COOLDOWN_HOURS = 1;
    public static final int ACCOUNT_CREATE_COOLDOWN_HOURS = 1;

    // 上限回数
    public static final int CREATE_POST_LIMIT = 12;
    public static final int CREATE_COMMENT_LIMIT = 24;
    public static final int EDIT_POST_LIMIT = 12;
    public static final int SEND_CONTACT_LIMIT = 2;
    public static final int ACCOUNT_CREATE_LIMIT = 2;

    // 重複投稿を許容しない時間（秒）
    public static final int COMMENT_DUPLICATE_WINDOW_SECONDS = 10;
    public static final int POST_DUPLICATE_WINDOW_SECONDS = 10;

    /** ログイン失敗がこの回数を超えたらアカウントをロックする */
    public static final int LOGIN_FAIL_THRESHOLD = 5;
    /** ロックする時間（分） */
    public static final int LOGIN_LOCK_MINUTES = 10;

}