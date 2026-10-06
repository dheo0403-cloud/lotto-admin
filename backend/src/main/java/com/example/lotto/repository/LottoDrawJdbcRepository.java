package com.example.lotto.repository;

import com.example.lotto.entity.LottoDraw;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LottoDrawJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void saveAllBatch(List<LottoDraw> draws) {
        String sql = "INSERT INTO lotto_draw (id, draw_date, num1, num2, num3, num4, num5, num6, bonus_num, total_prize, winner_count) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "winner_count = VALUES(winner_count), total_prize = VALUES(total_prize)"; // 중복 방지

        jdbcTemplate.batchUpdate(sql,
                draws,
                100, // batch size
                (ps, argument) -> {
                    ps.setInt(1, argument.getId());
                    ps.setDate(2, java.sql.Date.valueOf(argument.getDrawDate()));
                    ps.setInt(3, argument.getNum1());
                    ps.setInt(4, argument.getNum2());
                    ps.setInt(5, argument.getNum3());
                    ps.setInt(6, argument.getNum4());
                    ps.setInt(7, argument.getNum5());
                    ps.setInt(8, argument.getNum6());
                    ps.setInt(9, argument.getBonusNum());
                    ps.setLong(10, argument.getTotalPrize());
                    ps.setInt(11, argument.getWinnerCount());
                });
    }
}
