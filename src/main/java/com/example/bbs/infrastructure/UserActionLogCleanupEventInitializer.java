package com.example.bbs.infrastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.bbs.config.LogConfig;

@Component
public class UserActionLogCleanupEventInitializer {

    private final JdbcTemplate jdbcTemplate;

    public UserActionLogCleanupEventInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void createEvent() {
        String sql = """
                CREATE EVENT IF NOT EXISTS delete_old_user_action_logs
                ON SCHEDULE EVERY 1 DAY
                DO
                  DELETE FROM user_action_logs
                  WHERE created_at < NOW() - INTERVAL %d DAY;
                """.formatted(LogConfig.USER_ACTION_LOG_RETAIN_DAYS);

        jdbcTemplate.execute(sql);
    }
}
