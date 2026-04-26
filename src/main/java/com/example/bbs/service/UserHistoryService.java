package com.example.bbs.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.bbs.model.User;
import com.example.bbs.model.UserHistory;
import com.example.bbs.repository.UserHistoryRepository;

@Service
public class UserHistoryService {

    private final UserHistoryRepository repo;

    public UserHistoryService(UserHistoryRepository repo) {
        this.repo = repo;
    }

    public void saveDeleteHistory(User user, Long deletedBy) {
        UserHistory history = new UserHistory();
        history.setUserId(user.getId());
        history.setUsername(user.getUsername());
        history.setDeletedAt(LocalDateTime.now());
        history.setDeletedBy(deletedBy);

        repo.save(history);
    }
}
