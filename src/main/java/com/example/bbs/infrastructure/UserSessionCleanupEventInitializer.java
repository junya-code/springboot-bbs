package com.example.bbs.infrastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.bbs.config.SessionConfig;

@Component
public class UserSessionCleanupEventInitializer {

    private final JdbcTemplate jdbcTemplate;

    public UserSessionCleanupEventInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void createEvent() {
        String sql = """
                CREATE EVENT IF NOT EXISTS delete_old_user_sessions
                ON SCHEDULE EVERY 1 DAY
                DO
                  DELETE FROM user_session
                  WHERE last_seen_at < NOW() - INTERVAL %d DAY;
                """.formatted(SessionConfig.SESSION_EXPIRE_DAYS);

        jdbcTemplate.execute(sql);
    }
}
