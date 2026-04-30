package com.example.bbs.dto;

// record には最初から getter が自動生成されるので、@Getter は不要
public record LikeInfo(boolean isLiked, int likeCount) {
}
