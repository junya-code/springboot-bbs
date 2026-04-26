package com.example.bbs.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
    public String handleBadRequest(IllegalArgumentException e, Model model) {
        log.warn("Bad request", e); // ★ログには詳細を残す
        model.addAttribute("errorMessage", "不正なリクエストです。");
        return "error/400";
    }

    /**
     * 404 Not Found
     * （例：存在しない投稿IDなど）
     */
    @ExceptionHandler(NoSuchElementException.class)
    public String handleNotFound(NoSuchElementException e, Model model) {
        log.warn("Resource not found", e); // ★ログには詳細
        model.addAttribute("errorMessage", "指定されたデータは見つかりませんでした。");
        return "error/404";
    }

    /**
     * 500 Internal Server Error
     * （予期しない例外はすべてここに来る）
     */
    @ExceptionHandler(Exception.class)
    public String handleServerError(Exception e, Model model) {
        log.error("Unhandled exception occurred", e); // ★ログには詳細（スタックトレース含む）
        model.addAttribute("errorMessage", "サーバーエラーが発生しました。");
        return "error/500";
    }
}