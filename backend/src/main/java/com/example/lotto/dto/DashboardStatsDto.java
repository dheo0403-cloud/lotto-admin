package com.example.lotto.dto;


import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardStatsDto {

    // 최신 회차 정보
    private Integer latestDrawNo;
    private LocalDate latestDrawDate;
    private List<Integer> latestNumbers;  // [num1, num2, ..., num6]
    private Integer latestBonus;
    private Long latestPrize;
    private Integer latestWinnerCount;

    // 전체 통계
    private Long totalDraws;

    // 번호별 출현 빈도 (1~45) -> count
    private Map<Integer, Integer> numberFrequency;

    // 홀짝 비율
    private Integer oddCount;
    private Integer evenCount;

    // 구간별 분포 (1-10, 11-20, 21-30, 31-40, 41-45)
    private Map<String, Integer> zoneDistribution;

    // 합계 구간 분포
    private Map<String, Integer> sumBins;

    // 끝자리 분포 (0~9)
    private Map<Integer, Integer> lastDigitDistribution;

    // 최신 회차 결과 요약 (타겟 회차가 최신 회차인 추천 데이터)
    private List<RecommendationInfo> latestDrawRecommendations;

    // 다음 회차 예측 (타겟 회차가 최신 회차 + 1인 추천 데이터)
    private List<RecommendationInfo> nextDrawRecommendations;

    @Getter
    @Builder
    public static class RecommendationInfo {
        private Integer targetDraw;
        private List<Integer> numbers;
        private String createdAt;
        private String strategy;
        private Integer matchCount;   // 일치 번호 수
        private boolean bonusMatch;   // 보너스 번호 일치 여부
        private String resultLabel;   // "1등", "2등", ... "꽝"
    }
}
