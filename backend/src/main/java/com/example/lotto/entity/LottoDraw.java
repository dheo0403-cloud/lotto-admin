package com.example.lotto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "lotto_draw")
@Getter
@Setter
@NoArgsConstructor
public class LottoDraw {

    @Id
    @Column(name = "id")
    private Integer id; // 회차 번호 (drawNo)

    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;

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

    @Column(name = "bonus_num", nullable = false)
    private Integer bonusNum;

    @Column(name = "total_prize", nullable = false)
    private Long totalPrize;

    @Column(name = "winner_count", nullable = false)
    private Integer winnerCount;
}
