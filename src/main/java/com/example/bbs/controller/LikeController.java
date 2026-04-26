package com.example.bbs.controller;

import com.example.bbs.dto.LikeInfo;
import com.example.bbs.model.User;
import com.example.bbs.service.LikeService;
import com.example.bbs.service.UserService;

import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
public class LikeController {
    private final LikeService likeService;
    private final UserService userService;
    private final MessageSource messageSource;

    public LikeController(LikeService likeService,
            UserService userService,
            MessageSource messageSource) {
        this.likeService = likeService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @PostMapping("/{id}/like")
    public Map<String, Object> toggleLike(@PathVariable Long id) {
        User user = userService.getCurrentUser();

        LikeInfo result = likeService.toggleAndGetStatus(user, id);

        String messageKey = result.isLiked()
                ? "like.flash.liked"
                : "like.flash.unliked";

        String flashMessage = messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());

        return Map.of(
                "isLiked", result.isLiked(),
                "likeCount", result.likeCount(),
                "flashMessage", flashMessage);
    }

}