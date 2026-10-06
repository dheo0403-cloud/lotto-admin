package com.example.lotto.repository;

import com.example.lotto.entity.BatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchLogRepository extends JpaRepository<BatchLog, Long> {
}
