package com.example.lotto.repository;

import com.example.lotto.entity.LottoRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LottoRecommendationRepository extends JpaRepository<LottoRecommendation, Long> {
    List<LottoRecommendation> findAllByTargetDraw(Integer targetDraw);

    boolean existsByTargetDraw(Integer targetDraw);

    // 채점 전('미추첨') 행은 match_count가 0이라 평균을 왜곡하므로 제외
    @Query("select coalesce(r.strategy, '기본') as strategy, count(r) as total, avg(r.matchCount) as avgMatch, " +
           "sum(case when r.matchCount >= 3 then 1 else 0 end) as wins " +
           "from LottoRecommendation r where r.resultLabel <> '미추첨' " +
           "group by coalesce(r.strategy, '기본') order by count(r) desc")
    List<StrategyStat> findStrategyStats();
}
