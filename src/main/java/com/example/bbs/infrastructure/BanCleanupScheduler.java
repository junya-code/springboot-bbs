package com.example.bbs.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.bbs.service.SpamCheckService;

@Component
public class BanCleanupScheduler {

    private final SpamCheckService spamCheckService;

    public BanCleanupScheduler(SpamCheckService spamCheckService) {
        this.spamCheckService = spamCheckService;
    }

    // BAN（投稿・コメント・アカウント作成など）の期限切れエントリを
    // 24時間ごとにまとめてクリーンアップする
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    public void cleanupBanMaps() {
        spamCheckService.cleanupUserBanMap();
        spamCheckService.cleanupSessionBanMap();
    }

}
