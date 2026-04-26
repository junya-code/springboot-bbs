package com.example.bbs.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "post_delete_histories")
public class PostDeleteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "post_author_id", nullable = false)
    private Long postAuthorId;

    @Column(length = 50)
    private String oldTitle;

    @Column(columnDefinition = "TEXT")
    private String oldContent;

    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private Long deletedBy;
}
