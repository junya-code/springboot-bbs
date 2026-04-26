package com.example.bbs.config;

import java.time.Duration;

public class CookieConfig {
    /** ブラウザ識別用 Cookie の名前 */
    public static final String BROWSER_SESSION_COOKIE_NAME = "BROWSER_SESSION_ID";

    /** Cookie の寿命（秒）。365日分。 */
    public static final int BROWSER_SESSION_MAX_AGE = (int) Duration.ofDays(365).getSeconds();
}
