package com.example.bbs.model.enums;

public enum ActionType {
    // HTTP リクエスト用（Filter 専用）
    REQUEST,

    // ビジネスアクション
    PAGE_VIEW,
    LOGIN_SUCCESS,
    LOGOUT,
    CREATE_POST,
    DELETE_POST,
    EDIT_POST,
    CREATE_COMMENT,
    DELETE_COMMENT,
    SEND_CONTACT,
    ACCOUNT_CREATE,
    SESSION_EXPIRED,
    DELETE_USER_COMPLETELY,

    OTHER
}
