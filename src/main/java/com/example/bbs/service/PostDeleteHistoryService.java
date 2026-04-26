package com.example.bbs.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.bbs.model.Post;
import com.example.bbs.model.PostDeleteHistory;
import com.example.bbs.repository.PostDeleteHistoryRepository;

@Service
public class PostDeleteHistoryService {

    private final PostDeleteHistoryRepository repo;

    public PostDeleteHistoryService(PostDeleteHistoryRepository repo) {
        this.repo = repo;
    }

    public void saveDeleteHistory(Post post, Long deletedBy) {
        PostDeleteHistory history = new PostDeleteHistory();
        history.setPostId(post.getId());
        history.setPostAuthorId(post.getUser().getId());
        history.setOldTitle(post.getTitle());
        history.setOldContent(post.getContent());
        history.setCreatedAt(post.getCreatedAt());
        history.setDeletedAt(LocalDateTime.now());
        history.setDeletedBy(deletedBy);

        repo.save(history);
    }
}
