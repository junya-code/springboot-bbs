package com.example.bbs.service;

import com.example.bbs.config.SpamConfig;
import com.example.bbs.dto.PostForm;
import com.example.bbs.model.Post;
import com.example.bbs.model.PostHistory;
import com.example.bbs.model.User;
import com.example.bbs.repository.PostHistoryRepository;
import com.example.bbs.repository.PostRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final PostHistoryService postHistoryService;
    private final PostDeleteHistoryService postDeleteHistoryService;
    private final PostHistoryRepository historyRepo;

    public PostService(PostRepository postRepository,
            PostHistoryService postHistoryService,
            PostDeleteHistoryService postDeleteHistoryService,
            PostHistoryRepository historyRepo) {
        this.postRepository = postRepository;
        this.postHistoryService = postHistoryService;
        this.postDeleteHistoryService = postDeleteHistoryService;
        this.historyRepo = historyRepo;
    }

    public Page<Post> findAll(String sortBy, String sortOrder, int page, int size) {
        Sort sort = Sort.by(
                Sort.Direction.fromString(sortOrder),
                sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return postRepository.findAll(pageable);
    }

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    public List<Post> findByUserId(Long userId) {
        return postRepository.findByUserId(userId);
    }

    public LocalDateTime getLastEditedAt(Long postId) {
        PostHistory postHistoryTop = historyRepo.findTop1ByPostIdOrderByEditedAtDesc(postId);
        return postHistoryTop == null ? null : postHistoryTop.getEditedAt();
    }

    private boolean verifyOwnership(Post post, User user) {
        return post.getUser() != null && post.getUser().getId().equals(user.getId());
    }

    @Transactional
    public void updatePost(Long id, PostForm postForm, User user) {
        log.info("UpdatePost called. postId={}, userId={}", id, user.getId());

        try {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Post not found. postId={}", id);
                        return new NoSuchElementException("Post not found with id: " + id);
                    });

            if (!verifyOwnership(post, user)) {
                log.warn("Update denied. postId={}, userId={}", id, user.getId());
                throw new AccessDeniedException("Not authorized");
            }

            // ★ 冪等化：内容が変わっていなければスキップ
            if (post.getTitle().equals(postForm.getTitle()) &&
                    post.getContent().equals(postForm.getContent())) {

                log.warn("Update skipped. No changes. postId={}, userId={}", id, user.getId());
                return;
            }

            String oldTitle = post.getTitle();
            String newTitle = postForm.getTitle();
            String oldContent = post.getContent();
            String newContent = postForm.getContent();

            postHistoryService.saveHistory(post, oldTitle, newTitle,
                    oldContent, newContent, user.getId());

            post.update(newTitle, newContent);

            // JPA の永続化コンテキストにより、取得したエンティティは管理状態（managed）となる。
            // setter で値を変更すると、トランザクション終了時に自動で flush され DB に反映されるため、save() は不要。

            log.info("Post updated (pending commit). postId={}, userId={}", id, user.getId());

        } catch (Exception e) {
            log.error("UpdatePost failed. postId={}, userId={}", id, user.getId(), e);
            throw e;
        }
    }

    @Transactional
    public Post createPost(PostForm postForm, User user) {

        log.info("CreatePost called. userId={}, title={}", user.getId(), postForm.getTitle());

        try {
            // 投稿ボタン連打やBOTによる短時間の同じ投稿を制限する
            boolean exists = postRepository.existsByUserIdAndTitleAndContentAndCreatedAtAfter(
                    user.getId(),
                    postForm.getTitle(),
                    postForm.getContent(),
                    LocalDateTime.now().minusSeconds(SpamConfig.POST_DUPLICATE_WINDOW_SECONDS));

            if (exists) {
                log.warn("Duplicate post skipped. userId={}, title={}", user.getId(), postForm.getTitle());
                return null;
            }

            Post post = new Post(
                    postForm.getTitle(),
                    postForm.getContent(),
                    user);

            Post saved = postRepository.save(post);

            log.info("Post created. postId={}, userId={}", saved.getId(), user.getId());

            return saved;

        } catch (Exception e) {
            log.error("CreatePost failed. userId={}, title={}", user.getId(), postForm.getTitle(), e);
            throw e;
        }
    }

    public PostForm getEditForm(Long id, User user) {

        log.info("GetEditForm called. postId={}, userId={}", id, user.getId());

        Post post = postRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Post not found. postId={}", id);
                    return new NoSuchElementException("Post not found with id: " + id);
                });

        if (!verifyOwnership(post, user)) {
            log.warn("Edit denied. postId={}, userId={}", id, user.getId());
            throw new AccessDeniedException("Not authorized");
        }

        PostForm form = new PostForm();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());

        log.info("GetEditForm success. postId={}, userId={}", id, user.getId());

        return form;
    }

    @Transactional
    public void deletePost(Long id, User user) {

        log.info("DeletePost called. postId={}, userId={}", id, user.getId());

        try {
            // ★ まず投稿を取得（ここで存在しなければ例外 → 今まで通り）
            Optional<Post> optionalPost = postRepository.findById(id);
            if (optionalPost.isEmpty()) {
                log.warn("Delete skipped. Already deleted. postId={}", id);
                return; // ← 冪等化ポイント（重複削除を無視）
            }

            Post post = optionalPost.get();

            if (!verifyOwnership(post, user)) {
                log.warn("Delete denied. postId={}, userId={}", id, user.getId());
                throw new AccessDeniedException("Not authorized");
            }

            postDeleteHistoryService.saveDeleteHistory(post, user.getId());

            postRepository.delete(post);

            log.info("Delete command issued for Post. postId={}, userId={}", id, user.getId());

        } catch (Exception e) {
            log.error("DeletePost failed. postId={}, userId={}", id, user.getId(), e);
            throw e;
        }
    }

    public Page<Post> searchPosts(String keyword, String matchType, String sortBy, String sortOrder, int page,
            int size) {
        Sort sort = Sort.by(
                Sort.Direction.fromString(sortOrder),
                sortBy);

        Pageable pageable = PageRequest.of(page, size, sort);

        if (keyword == null || keyword.trim().isEmpty()) {
            return postRepository.findAll(pageable);
        }

        switch (matchType) {
            case "startswith":
                return postRepository.findByTitleStartingWithOrContentStartingWith(keyword, keyword, pageable);
            case "endswith":
                return postRepository.findByTitleEndingWithOrContentEndingWith(keyword, keyword, pageable);
            case "contains":
            default:
                return postRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
        }
    }

    public Page<Post> getSafePostsPage(
            String keyword,
            String matchType,
            String sortBy,
            String sortOrder,
            int page,
            int size) {

        Page<Post> posts = searchPosts(keyword, matchType, sortBy, sortOrder, page, size);
        int totalPages = posts.getTotalPages();

        // 投稿0件 → page=0 のまま返す
        if (totalPages == 0) {
            return posts;
        }

        // 範囲外ページ → 最終ページへ補正
        if (page < 0 || page >= totalPages) {
            int lastPage = totalPages - 1;

            return searchPosts(keyword, matchType, sortBy, sortOrder, lastPage, size);
        }

        return posts;
    }

}
