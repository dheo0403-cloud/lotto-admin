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
    private final LottoRecommendationService lottoRecommendationService;

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

    /**
     * 저장된 최신 회차 다음부터 API에 있는 회차까지 수집한다. 여러 번 실행해도 이어서 받으므로 중복이 없다.
     * 결과는 batch_log에 SUCCESS / NO_NEW(새 회차 아직 없음) / FAIL(API 오류)로 남기고,
     * 오류 없이 새 회차를 저장했을 때만 다음 회차 추천을 자동 생성한다.
     */
    @Transactional
    public void syncLatestDraws() {
        int latestSavedDraw = lottoDrawRepository.findTopByOrderByIdDesc()
                .map(LottoDraw::getId)
                .orElse(0);

        int nextDraw = latestSavedDraw + 1;
        int savedCount = 0;
        String failReason = null;
        List<LottoDraw> batchList = new ArrayList<>();

        while (true) {
            try {
                DhLottoResponse response = lottoClient.fetchLottoDraw(nextDraw);
                // LottoClient는 HTTP 오류·예외를 빈 응답으로 돌려준다
                if (!response.isSuccess()) {
                    failReason = "Draw " + nextDraw + ": API returned no data";
                    break;
                }
                Integer returned = response.getFirstDraw().getLtEpsd();
                if (returned == null) {
                    failReason = "Draw " + nextDraw + ": response has no draw number";
                    break;
                }
                // 요청 회차가 아직 없으면 API가 최신 회차를 돌려준다 → 정상 종료
                if (returned != nextDraw) {
                    log.info("Requested draw {} but got draw {}. Stopping sync.", nextDraw, returned);
                    break;
                }
                batchList.add(mapToEntity(response.getFirstDraw()));
                log.info("Fetched draw {}", nextDraw);
                nextDraw++;

                // 100건이 모이면 JDBC Batch로 한 번에 INSERT
                if (batchList.size() >= 100) {
                    savedCount += flush(batchList, nextDraw - 1);
                }
            } catch (Exception e) {
                log.error("Stop syncing due to error at draw {}", nextDraw, e);
                failReason = "Draw " + nextDraw + " error: " + e.getMessage();
                break;
            }
        }
        savedCount += flush(batchList, nextDraw - 1);

        if (failReason != null) {
            batchLogRepository.save(BatchLog.of("SYNC_LOTTO_BATCH", "FAIL", failReason));
            return;
        }
        if (savedCount == 0) {
            batchLogRepository.save(BatchLog.of("SYNC_LOTTO_BATCH", "NO_NEW", "No new draw after " + latestSavedDraw));
            return;
        }
        // nextDraw = 마지막 저장 회차 + 1 = 아직 추첨 전인 회차
        generateNextIfMissing(nextDraw);
    }

    private int flush(List<LottoDraw> batch, int lastDraw) {
        if (batch.isEmpty()) return 0;
        lottoDrawJdbcRepository.saveAllBatch(batch);
        batch.forEach(this::evaluateRecommendations);
        batchLogRepository.save(BatchLog.of("SYNC_LOTTO_BATCH", "SUCCESS", "Synced up to draw: " + lastDraw));
        int count = batch.size();
        batch.clear();
        return count;
    }

    // 다음 회차 추천이 아직 없을 때만 5세트 생성 (재실행해도 중복 생성 안 됨)
    private void generateNextIfMissing(int nextDraw) {
        if (recommendationRepository.existsByTargetDraw(nextDraw)) return;
        lottoRecommendationService.generateRecommendations(5);
        log.info("Auto-generated recommendations for draw {}", nextDraw);
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
