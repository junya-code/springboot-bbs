package com.example.bbs.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "post_histories")
public class PostHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(length = 50)
    private String oldTitle;

    @Column(length = 50)
    private String newTitle;

    // TEXT にするのは「履歴は制限なく丸ごと残すべき」という思想
    // PostHistory はユーザーが検索しない前提のテーブル。
    // そのため TEXT 型でも検索負荷の心配は不要。
    @Column(columnDefinition = "TEXT")
    private String oldContent;

    @Column(columnDefinition = "TEXT")
    private String newContent;

    private LocalDateTime editedAt;

    private Long editedBy;
}