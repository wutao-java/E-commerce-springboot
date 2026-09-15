package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.AfterSale;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AfterSaleRepository extends JpaRepository<AfterSale, Long> {

    List<AfterSale> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AfterSale a where a.id = :id")
    Optional<AfterSale> findByIdForUpdate(@Param("id") Long id);
}
