package com.example.bbs.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.bbs.model.enums.ActionType;
import com.example.bbs.model.enums.BotStatus;
import com.example.bbs.model.enums.HttpMethodType;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "user_action_logs")
public class UserActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethodType method;

    private Long userId;

    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType action;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_status", nullable = false)
    private BotStatus botStatus;

    @Column(name = "remote_port")
    private Integer remotePort;
}
