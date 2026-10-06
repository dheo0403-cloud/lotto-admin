package com.example.lotto.scheduler;

import com.example.lotto.entity.LottoDraw;
import com.example.lotto.repository.LottoDrawRepository;
import com.example.lotto.repository.LottoRecommendationRepository;
import com.example.lotto.service.LottoRecommendationService;
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
    private final LottoRecommendationService lottoRecommendationService;
    private final LottoDrawRepository lottoDrawRepository;
    private final LottoRecommendationRepository recommendationRepository;

    // 매주 토요일 오후 9시 30분에 실행 (추첨 직후)
    @Scheduled(cron = "0 30 21 * * 6", zone = "Asia/Seoul")
    public void scheduleLottoSync() {
        log.info("Starting Lotto Schedule Sync...");
        lottoSyncService.syncLatestDraws();
        generateNextIfMissing();
        log.info("Finished Lotto Schedule Sync");
    }

    // 다음 회차 추천이 아직 없을 때만 5세트 생성 (재실행·수집 실패 시 중복 생성 방지)
    private void generateNextIfMissing() {
        int nextDraw = lottoDrawRepository.findTopByOrderByIdDesc().map(LottoDraw::getId).orElse(0) + 1;
        if (nextDraw == 1 || recommendationRepository.existsByTargetDraw(nextDraw)) return;
        lottoRecommendationService.generateRecommendations(5);
        log.info("Auto-generated recommendations for draw {}", nextDraw);
    }
}
