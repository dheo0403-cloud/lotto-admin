package com.example.lotto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lotto_recommendation")
@Getter
@Setter
@NoArgsConstructor
public class LottoRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_draw", nullable = false)
    private Integer targetDraw; // 예상 대상 회차

    @Column(name = "num1", nullable = false)
    private Integer num1;

    @Column(name = "num2", nullable = false)
    private Integer num2;

    @Column(name = "num3", nullable = false)
    private Integer num3;

    @Column(name = "num4", nullable = false)
    private Integer num4;

    @Column(name = "num5", nullable = false)
    private Integer num5;

    @Column(name = "num6", nullable = false)
    private Integer num6;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "strategy", length = 50)
    private String strategy; // 사용된 추천 전략명

    @Column(name = "is_matched", nullable = false)
    private Boolean isMatched = false;

    @Column(name = "match_count", nullable = false)
    private Integer matchCount = 0;

    @Column(name = "bonus_match", nullable = false)
    private Boolean bonusMatch = false;

    @Column(name = "result_label", length = 20)
    private String resultLabel = "미추첨";
    public void updateResult(Integer mCount, Boolean bMatch) {
        this.matchCount = mCount;
        this.bonusMatch = bMatch;
        this.isMatched = mCount >= 3;
        this.resultLabel = calcRank(mCount, bMatch);
    }

    private String calcRank(int mCount, boolean bMatch) {
        if (mCount == 6) return "1등";
        if (mCount == 5 && bMatch) return "2등";
        if (mCount == 5) return "3등";
        if (mCount == 4) return "4등";
        if (mCount == 3) return "5등";
        return "꽝";
    }
}
