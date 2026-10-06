package com.example.lotto.controller;

import com.example.lotto.dto.DashboardStatsDto;
import com.example.lotto.entity.BatchLog;
import com.example.lotto.entity.LottoDraw;
import com.example.lotto.entity.LottoRecommendation;
import com.example.lotto.repository.BatchLogRepository;
import com.example.lotto.repository.LottoRecommendationRepository;
import com.example.lotto.repository.StrategyStat;
import com.example.lotto.service.LottoDrawService;
import com.example.lotto.service.LottoRecommendationService;
import com.example.lotto.service.LottoSyncService;
import com.example.lotto.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LottoController {

    private final LottoDrawService lottoDrawService;
    private final LottoRecommendationService lottoRecommendationService;
    private final LottoSyncService lottoSyncService;
    private final BatchLogRepository batchLogRepository;
    private final StatsService statsService;
    private final LottoRecommendationRepository recommendationRepository;

    @GetMapping("/lotto/draws")
    public ResponseEntity<Page<LottoDraw>> getDraws(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok(lottoDrawService.getDraws(pageRequest));
    }

    @GetMapping("/lotto/draws/{drawNo}")
    public ResponseEntity<LottoDraw> getDraw(@PathVariable int drawNo) {
        return lottoDrawService.getDraw(drawNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/lotto/recommend")
    public ResponseEntity<List<LottoRecommendation>> recommend(@RequestParam(defaultValue = "5") int count) {
        // 대량 생성 방지: 1~10세트로 제한
        int safeCount = Math.max(1, Math.min(count, 10));
        return ResponseEntity.ok(lottoRecommendationService.generateRecommendations(safeCount));
    }

    @PostMapping("/lotto/sync/{drawNo}")
    public ResponseEntity<String> syncDraw(@PathVariable int drawNo) {
        if (drawNo == 0) {
            new Thread(lottoSyncService::syncLatestDraws).start();
            return ResponseEntity.ok("Latest draws sync initiated.");
        } else {
            lottoSyncService.syncLottoDraw(drawNo);
            return ResponseEntity.ok("Sync initiated for draw: " + drawNo);
        }
    }

    @GetMapping("/logs/batch")
    public ResponseEntity<Page<BatchLog>> getBatchLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "executedAt"));
        return ResponseEntity.ok(batchLogRepository.findAll(pageRequest));
    }
    
    @GetMapping("/stats/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(statsService.getDashboardStats());
    }

    @GetMapping("/stats/strategies")
    public ResponseEntity<List<StrategyStat>> getStrategyStats() {
        return ResponseEntity.ok(recommendationRepository.findStrategyStats());
    }
}
