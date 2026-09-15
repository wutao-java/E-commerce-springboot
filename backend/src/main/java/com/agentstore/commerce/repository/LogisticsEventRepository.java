package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.LogisticsEvent;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogisticsEventRepository extends JpaRepository<LogisticsEvent, Long> {

    List<LogisticsEvent> findByOrderIdOrderByOccurredAtAsc(Long orderId);

    List<LogisticsEvent> findByOrderIdInOrderByOccurredAtAsc(Collection<Long> orderIds);
}
