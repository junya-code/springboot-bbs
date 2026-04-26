package com.example.bbs.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "comment_histories")
public class CommentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "comment_author_id", nullable = false)
    private Long commentAuthorId;

    @Column(columnDefinition = "TEXT")
    private String oldContent;

    private LocalDateTime deletedAt;

    private Long deletedBy;
}
