package com.example.bbs.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bbs.dto.LikeInfo;
import com.example.bbs.model.Like;
import com.example.bbs.model.Post;
import com.example.bbs.model.User;
import com.example.bbs.repository.LikeRepository;
import com.example.bbs.repository.PostRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LikeService {
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    public LikeService(LikeRepository likeRepository, PostRepository postRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public List<Like> findByUserId(Long userId) {
        return likeRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Like> findByPostId(Long postId) {
        return likeRepository.findByPostId(postId);
    }

    /**
     * 非同期（JavaScript / fetch）用】
     * いいねボタンを押したときに呼ばれるメソッド。
     * 
     * JS の fetch("/posts/{id}/like") から呼ばれる
     * DB の更新（いいね追加/削除）を行う
     * 更新後の状態（isLiked, likeCount）を返す
     * 
     * Transactional で同期的に DB 保存
     */
    @Transactional
    public LikeInfo toggleAndGetStatus(User user, Long postId) {

        log.info("ToggleLike called. postId={}, userId={}", postId, user.getId());

        try {
            // 1行ラムダではないので → return 省略できない
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> {
                        log.warn("Post not found. postId={}", postId);
                        return new NoSuchElementException("Post not found with id: " + postId);
                    });

            likeRepository.findByUserAndPost(user, post)
                    .ifPresentOrElse(
                            like -> {
                                log.debug("Like exists. Deleting. postId={}, userId={}", postId, user.getId());
                                likeRepository.delete(like);
                            },
                            () -> {
                                log.debug("Like not found. Creating. postId={}, userId={}", postId, user.getId());
                                Like like = new Like(post, user);
                                likeRepository.save(like);
                            });

            boolean isLiked = likeRepository.existsByUserAndPost(user, post);
            int likeCount = likeRepository.countByPost(post);

            log.info("ToggleLike result. postId={}, userId={}, isLiked={}, likeCount={}",
                    postId, user.getId(), isLiked, likeCount);

            return new LikeInfo(isLiked, likeCount);

        } catch (Exception e) {
            log.error("ToggleLike failed. postId={}, userId={}", postId, user.getId(), e);
            throw e;
        }
    }

    /**
     * 【HTML（Thymeleaf 初期表示）用】
     * 投稿詳細ページを開いたときに呼ばれるメソッド。
     * 
     * ページ初期表示のために「現在のいいね状態」を取得する
     * DB の更新はしない（readOnly = true）
     * Thymeleaf の ${likeInfo.xxx} に使われる
     */
    @Transactional(readOnly = true)
    public LikeInfo getLikeInfo(User user, Post post) {
        boolean isLiked = likeRepository.existsByUserAndPost(user, post);
        int likeCount = likeRepository.countByPost(post);
        return new LikeInfo(isLiked, likeCount);
    }
}
