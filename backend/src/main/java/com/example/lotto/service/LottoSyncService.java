package com.example.lotto.service;

import com.example.lotto.client.LottoClient;
import com.example.lotto.dto.DhLottoResponse;
import com.example.lotto.entity.BatchLog;
import com.example.lotto.entity.LottoDraw;
import com.example.lotto.entity.LottoRecommendation;
import com.example.lotto.repository.BatchLogRepository;
import com.example.lotto.repository.LottoDrawJdbcRepository;
import com.example.lotto.repository.LottoDrawRepository;
import com.example.lotto.repository.LottoRecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LottoSyncService {

    private final LottoClient lottoClient;
    private final LottoDrawRepository lottoDrawRepository;
    private final BatchLogRepository batchLogRepository;
    private final LottoDrawJdbcRepository lottoDrawJdbcRepository;
    private final LottoRecommendationRepository recommendationRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public void syncLottoDraw(int drawNo) {
        try {
            DhLottoResponse response = lottoClient.fetchLottoDraw(drawNo);

            if (response.isSuccess()) {
                LottoDraw draw = mapToEntity(response.getFirstDraw());
                lottoDrawRepository.save(draw);
                evaluateRecommendations(draw);
                batchLogRepository.save(BatchLog.of("SYNC_LOTTO", "SUCCESS", "Draw No: " + drawNo + " synced successfully."));
                log.info("Successfully synced draw {}", drawNo);
            } else {
                batchLogRepository.save(BatchLog.of("SYNC_LOTTO", "FAIL", "Draw No: " + drawNo + " failed. No data returned."));
            }
        } catch (Exception e) {
            log.error("Failed to sync draw {}", drawNo, e);
            batchLogRepository.save(BatchLog.of("SYNC_LOTTO", "ERROR", "Draw No: " + drawNo + " error: " + e.getMessage()));
        }
    }

    /**
     * 신규 API 응답(DrawInfo)을 LottoDraw 엔티티로 변환
     */
    private LottoDraw mapToEntity(DhLottoResponse.DrawInfo info) {
        LottoDraw draw = new LottoDraw();
        draw.setId(info.getLtEpsd());
        draw.setNum1(info.getTm1WnNo());
        draw.setNum2(info.getTm2WnNo());
        draw.setNum3(info.getTm3WnNo());
        draw.setNum4(info.getTm4WnNo());
        draw.setNum5(info.getTm5WnNo());
        draw.setNum6(info.getTm6WnNo());
        draw.setBonusNum(info.getBnsWnNo());

        // 추첨일 파싱 (yyyyMMdd → LocalDate)
        if (info.getLtRflYmd() != null && info.getLtRflYmd().length() == 8) {
            draw.setDrawDate(LocalDate.parse(info.getLtRflYmd(), DATE_FMT));
        } else {
            draw.setDrawDate(LocalDate.now());
        }

        // 1등 당첨금 총액 (1인당 금액 × 당첨자 수)
        long totalPrize = 0;
        if (info.getRnk1WnAmt() != null && info.getRnk1WnNope() != null) {
            totalPrize = info.getRnk1WnAmt() * info.getRnk1WnNope();
        }
        draw.setTotalPrize(totalPrize);
        draw.setWinnerCount(info.getRnk1WnNope() != null ? info.getRnk1WnNope() : 0);

        return draw;
    }

    @Transactional
    public void syncLatestDraws() {
        // DB에 저장된 가장 최신 회차를 가져옵니다.
        int latestSavedDraw = lottoDrawRepository.findTopByOrderByIdDesc()
                .map(LottoDraw::getId)
                .orElse(0);

        int nextDraw = latestSavedDraw + 1;
        boolean hasMore = true;
        List<LottoDraw> batchList = new ArrayList<>();

        while (hasMore) {
            try {
                DhLottoResponse response = lottoClient.fetchLottoDraw(nextDraw);
                if (response.isSuccess()) {
                    DhLottoResponse.DrawInfo info = response.getFirstDraw();
                    // 응답은 됐지만, 요청한 회차와 다른 회차가 반환되면 미래 회차이므로 중단
                    if (info.getLtEpsd() != null && info.getLtEpsd() != nextDraw) {
                        log.info("Requested draw {} but got draw {}. Stopping sync.", nextDraw, info.getLtEpsd());
                        hasMore = false;
                    } else {
                        batchList.add(mapToEntity(info));
                        log.info("Fetched draw {}", nextDraw);

                        // 100건이 모이면 JDBC Batch를 통해 한 번에 INSERT
                        if (batchList.size() >= 100) {
                            lottoDrawJdbcRepository.saveAllBatch(batchList);
                            for (LottoDraw draw : batchList) {
                                evaluateRecommendations(draw);
                            }
                            batchLogRepository.save(BatchLog.of("SYNC_LOTTO_BATCH", "SUCCESS", "Synced batch up to draw: " + nextDraw));
                            batchList.clear();
                        }
                        nextDraw++;
                    }
                } else {
                    hasMore = false; // 응답 실패 시 중단
                }
            } catch (Exception e) {
                hasMore = false;
                log.error("Stop syncing due to error", e);
            }
        }

        // 100건 단위로 묶고 남은 자투리 데이터 INSERT
        if (!batchList.isEmpty()) {
            lottoDrawJdbcRepository.saveAllBatch(batchList);
            for (LottoDraw draw : batchList) {
                evaluateRecommendations(draw);
            }
            batchLogRepository.save(BatchLog.of("SYNC_LOTTO_BATCH", "SUCCESS", "Synced final batch, ending at draw: " + (nextDraw - 1)));
            batchList.clear();
        }
    }

    private void evaluateRecommendations(LottoDraw draw) {
        List<LottoRecommendation> recs = recommendationRepository.findAllByTargetDraw(draw.getId());
        if (recs.isEmpty()) return;

        Set<Integer> winNums = new HashSet<>(Arrays.asList(
                draw.getNum1(), draw.getNum2(), draw.getNum3(),
                draw.getNum4(), draw.getNum5(), draw.getNum6()
        ));

        for (LottoRecommendation rec : recs) {
            List<Integer> recNums = Arrays.asList(
                    rec.getNum1(), rec.getNum2(), rec.getNum3(),
                    rec.getNum4(), rec.getNum5(), rec.getNum6()
            );
            int matchCount = (int) recNums.stream().filter(winNums::contains).count();
            boolean bonusMatch = recNums.contains(draw.getBonusNum());

            rec.updateResult(matchCount, bonusMatch);
            recommendationRepository.save(rec);
        }
        log.info("Evaluated {} recommendations for draw {}", recs.size(), draw.getId());
    }
}
