package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.BalanceRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRecordRepository extends JpaRepository<BalanceRecord, Long> {

    List<BalanceRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
