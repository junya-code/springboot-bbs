package com.example.bbs.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.bbs.model.Comment;
import com.example.bbs.model.CommentHistory;
import com.example.bbs.repository.CommentHistoryRepository;

@Service
public class CommentHistoryService {

    private final CommentHistoryRepository repo;

    public CommentHistoryService(CommentHistoryRepository repo) {
        this.repo = repo;
    }

    public void saveDeleteHistory(Comment comment, String oldContent, Long deletedBy) {
        CommentHistory history = new CommentHistory();
        history.setCommentId(comment.getId());
        history.setCommentAuthorId(comment.getUser().getId());
        history.setOldContent(oldContent);
        history.setDeletedAt(LocalDateTime.now());
        history.setDeletedBy(deletedBy);

        repo.save(history);
    }
}
