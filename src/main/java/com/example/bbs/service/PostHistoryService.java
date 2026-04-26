package com.example.bbs.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.bbs.model.Post;
import com.example.bbs.model.PostHistory;
import com.example.bbs.repository.PostHistoryRepository;

@Service
public class PostHistoryService {

    private final PostHistoryRepository repo;

    public PostHistoryService(PostHistoryRepository repo) {
        this.repo = repo;
    }

    public void saveHistory(Post post,
            String oldTitle,
            String newTitle,
            String oldContent,
            String newContent,
            Long editedBy) {
        PostHistory history = new PostHistory();
        history.setPostId(post.getId());

        history.setOldTitle(oldTitle);
        history.setNewTitle(newTitle);

        history.setOldContent(oldContent);
        history.setNewContent(newContent);
        history.setEditedAt(LocalDateTime.now());
        history.setEditedBy(editedBy);

        repo.save(history);
    }
}