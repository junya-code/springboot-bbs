package com.example.bbs.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Table(name = "likes")
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Like() {
    } // JPA用

    public Like(Post post, User user) {
        this.post = post;
        this.user = user;
    }

    // Post.addLike() から呼ばれる内部用
    void setPost(Post post) {
        this.post = post;
    }

    // 双方向関連(Post.addLike)を維持するための内部用メソッド。
    // 外部からLikeの所有者を書き換えるべきではないためpublicにしない。
    // 現時点ではService層から直接呼ぶことはなく、Post側のaddLike()のみが使用する。
    void setUser(User user) {
        this.user = user;
    }

}
