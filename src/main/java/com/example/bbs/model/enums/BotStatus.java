package com.example.bbs.model.enums;

/**
 * ブラウザセッションが BOT か HUMAN かを示すステータス。
 * 
 * - HUMAN: 正常なブラウザ（署名付き Cookie が正しい）
 * - BOT: Cookie 偽装 or 新規アクセス（UUID 初回発行）
 */
public enum BotStatus {
    HUMAN,
    BOT,
    UNKNOWN
}
