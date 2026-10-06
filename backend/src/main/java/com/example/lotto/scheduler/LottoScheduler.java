package com.example.lotto.scheduler;

import com.example.lotto.service.LottoSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LottoScheduler {

    private final LottoSyncService lottoSyncService;

    // 토 21:30(추첨 직후) + 토 23:00·일 09:00 재시도. 이미 받은 회차 다음부터 이어 받으므로 중복 없음.
    // 수집 성공 시 다음 회차 추천 자동 생성은 LottoSyncService가 처리
    @Scheduled(cron = "0 30 21 * * SAT", zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 23 * * SAT", zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 9 * * SUN", zone = "Asia/Seoul")
    public void scheduleLottoSync() {
        log.info("Starting Lotto Schedule Sync...");
        lottoSyncService.syncLatestDraws();
        log.info("Finished Lotto Schedule Sync");
    }
}
