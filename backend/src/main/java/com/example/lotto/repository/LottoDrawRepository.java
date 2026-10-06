package com.example.lotto.repository;

import com.example.lotto.entity.LottoDraw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LottoDrawRepository extends JpaRepository<LottoDraw, Integer> {
    Optional<LottoDraw> findTopByOrderByIdDesc();
}
