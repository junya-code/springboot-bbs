package com.example.bbs.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

/**
 * アプリ全体の例外を一括で処理するクラス。
 * 各コントローラーでキャッチされなかった例外はここに集約され、
 * 適切なエラーページへ振り分けられる。
 *
 * - IllegalArgumentException → 400 ページ
 * - NoSuchElementException → 404 ページ
 * - その他の例外 → 500 ページ
 *
 * ※ 例えば投稿削除ボタンのAccessDeniedException は Spring Security が処理するためここには来ない。
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 400 Bad Request
     * （例：不正なパラメータ、バリデーションエラーなど）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException e) {
        log.warn("Bad request", e); // ★ログには詳細を残す
        return "error/400";
    }

    /**
     * 404 Not Found
     * （例：存在しない投稿IDなど）
     */
    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException e) {
        log.warn("Resource not found", e); // ★ログには詳細
        return "error/404";
    }

    // 静的リソースは warn ではなく debug にするのが自然
    // （ログ汚染を避けるため）
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleStaticResourceNotFound(NoResourceFoundException e) {
        log.debug("Static resource not found: {}", e.getResourcePath());
        return "error/404";
    }

    /**
     * 500 Internal Server Error
     * （予期しない例外はすべてここに来る）
     */
    @ExceptionHandler(Exception.class)
    public String handleServerError(Exception e) {
        log.error("Unhandled exception occurred", e); // ★ログには詳細（スタックトレース含む）
        return "error/500";
    }
}