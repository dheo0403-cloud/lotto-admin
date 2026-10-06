package com.example.lotto.service;

import com.example.lotto.dto.DashboardStatsDto;
import com.example.lotto.entity.LottoDraw;
import com.example.lotto.entity.LottoRecommendation;
import com.example.lotto.repository.LottoDrawRepository;
import com.example.lotto.repository.LottoRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final LottoDrawRepository lottoDrawRepository;
    private final LottoRecommendationRepository recommendationRepository;

    public DashboardStatsDto getDashboardStats() {
        List<LottoDraw> allDraws = lottoDrawRepository.findAll();

        // 최신 회차
        Optional<LottoDraw> latestOpt = lottoDrawRepository.findTopByOrderByIdDesc();

        // 번호별 빈도 (1~45)
        Map<Integer, Integer> frequency = new LinkedHashMap<>();
        for (int i = 1; i <= 45; i++) {
            frequency.put(i, 0);
        }

        int oddCount = 0;
        int evenCount = 0;

        // 구간별 분포
        Map<String, Integer> zoneDistribution = new LinkedHashMap<>();
        zoneDistribution.put("1-10", 0);
        zoneDistribution.put("11-20", 0);
        zoneDistribution.put("21-30", 0);
        zoneDistribution.put("31-40", 0);
        zoneDistribution.put("41-45", 0);

        // 끝자리 분포 (0~9)
        Map<Integer, Integer> lastDigit = new LinkedHashMap<>();
        for (int i = 0; i <= 9; i++) {
            lastDigit.put(i, 0);
        }

        // 합계 구간
        Map<String, Integer> sumBins = new LinkedHashMap<>();
        sumBins.put("21-80", 0);
        sumBins.put("81-100", 0);
        sumBins.put("101-120", 0);
        sumBins.put("121-140", 0);
        sumBins.put("141-160", 0);
        sumBins.put("161-180", 0);
        sumBins.put("181-255", 0);

        for (LottoDraw draw : allDraws) {
            List<Integer> nums = List.of(
                    draw.getNum1(), draw.getNum2(), draw.getNum3(),
                    draw.getNum4(), draw.getNum5(), draw.getNum6()
            );

            int sum = 0;
            for (int num : nums) {
                // 빈도
                frequency.merge(num, 1, Integer::sum);

                // 홀짝
                if (num % 2 == 0) evenCount++;
                else oddCount++;

                // 구간
                if (num <= 10) zoneDistribution.merge("1-10", 1, Integer::sum);
                else if (num <= 20) zoneDistribution.merge("11-20", 1, Integer::sum);
                else if (num <= 30) zoneDistribution.merge("21-30", 1, Integer::sum);
                else if (num <= 40) zoneDistribution.merge("31-40", 1, Integer::sum);
                else zoneDistribution.merge("41-45", 1, Integer::sum);

                // 끝자리
                lastDigit.merge(num % 10, 1, Integer::sum);

                sum += num;
            }

            // 합계 구간
            if (sum <= 80) sumBins.merge("21-80", 1, Integer::sum);
            else if (sum <= 100) sumBins.merge("81-100", 1, Integer::sum);
            else if (sum <= 120) sumBins.merge("101-120", 1, Integer::sum);
            else if (sum <= 140) sumBins.merge("121-140", 1, Integer::sum);
            else if (sum <= 160) sumBins.merge("141-160", 1, Integer::sum);
            else if (sum <= 180) sumBins.merge("161-180", 1, Integer::sum);
            else sumBins.merge("181-255", 1, Integer::sum);
        }

        // 최신 회차 번호
        int latestNo = latestOpt.map(LottoDraw::getId).orElse(0);

        // 추천 데이터 조회 (전체 조회 후 메모리에서 필터링하거나 쿼리로 분할)
        List<LottoRecommendation> allRecs = recommendationRepository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        List<DashboardStatsDto.RecommendationInfo> latestRecs = allRecs.stream()
                .filter(r -> r.getTargetDraw() == latestNo)
                .map(this::mapToRecInfo)
                .collect(Collectors.toList());

        List<DashboardStatsDto.RecommendationInfo> nextRecs = allRecs.stream()
                .filter(r -> r.getTargetDraw() == latestNo + 1)
                .map(this::mapToRecInfo)
                .collect(Collectors.toList());

        DashboardStatsDto.DashboardStatsDtoBuilder builder = DashboardStatsDto.builder()
                .totalDraws((long) allDraws.size())
                .numberFrequency(frequency)
                .oddCount(oddCount)
                .evenCount(evenCount)
                .zoneDistribution(zoneDistribution)
                .sumBins(sumBins)
                .lastDigitDistribution(lastDigit)
                .latestDrawRecommendations(latestRecs)
                .nextDrawRecommendations(nextRecs);

        latestOpt.ifPresent(latest -> {
            builder.latestDrawNo(latest.getId())
                    .latestDrawDate(latest.getDrawDate())
                    .latestNumbers(List.of(
                            latest.getNum1(), latest.getNum2(), latest.getNum3(),
                            latest.getNum4(), latest.getNum5(), latest.getNum6()))
                    .latestBonus(latest.getBonusNum())
                    .latestPrize(latest.getTotalPrize())
                    .latestWinnerCount(latest.getWinnerCount());
        });

        return builder.build();
    }

    private DashboardStatsDto.RecommendationInfo mapToRecInfo(LottoRecommendation r) {
        List<Integer> recNums = List.of(r.getNum1(), r.getNum2(), r.getNum3(),
                r.getNum4(), r.getNum5(), r.getNum6());

        return DashboardStatsDto.RecommendationInfo.builder()
                .targetDraw(r.getTargetDraw())
                .numbers(recNums)
                .createdAt(r.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .strategy(r.getStrategy() != null ? r.getStrategy() : "기본")
                .matchCount(r.getMatchCount())
                .bonusMatch(r.getBonusMatch())
                .resultLabel(r.getResultLabel())
                .build();
    }

}
