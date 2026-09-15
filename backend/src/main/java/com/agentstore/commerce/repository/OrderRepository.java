package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.CustomerOrder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CustomerOrder> findByOrderNoAndUserId(String orderNo, Long userId);

    Optional<CustomerOrder> findByOrderNo(String orderNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.orderNo = :orderNo and o.userId = :userId")
    Optional<CustomerOrder> findByOrderNoAndUserIdForUpdate(
        @Param("orderNo") String orderNo, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.orderNo = :orderNo")
    Optional<CustomerOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
