package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.AfterSalePolicy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AfterSalePolicyRepository extends JpaRepository<AfterSalePolicy, Long> {

    List<AfterSalePolicy> findAllByOrderByIdAsc();
}
