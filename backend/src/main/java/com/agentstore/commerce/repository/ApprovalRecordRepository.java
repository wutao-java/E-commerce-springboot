package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.ApprovalRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {

    List<ApprovalRecord> findByAfterSaleIdOrderByCreatedAtAsc(Long afterSaleId);

    List<ApprovalRecord> findByAfterSaleIdInOrderByCreatedAtAsc(Collection<Long> afterSaleIds);
}
