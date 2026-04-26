package com.example.bbs.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String content;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ★ これが無いと Hibernate が Comment を生成できず落ちる
    protected Comment() {
    }

    public Comment(String content, Post post, User user) {
        this.content = content;
        this.post = post;
        this.user = user;
    }

    // Post.addComment() から呼ばれる内部用
    void setPost(Post post) {
        this.post = post;
    }

    // 双方向関連(Post.addComment)を維持するための内部用メソッド。
    // 外部からコメントの所有者を書き換えるべきではないためpublicにしない。
    // 現在の実装ではService層から直接呼ばれないが、関連同期のために残している。
    void setUser(User user) {
        this.user = user;
    }

}
