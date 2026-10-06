package com.example.lotto.repository;

/**
 * 전략별 추천 성적 집계 (채점 완료 건만)
 */
public interface StrategyStat {
    String getStrategy();
    Long getTotal();      // 채점된 세트 수
    Double getAvgMatch(); // 평균 일치 번호 수
    Long getWins();       // 5등 이상(3개 이상 일치) 건수
}
