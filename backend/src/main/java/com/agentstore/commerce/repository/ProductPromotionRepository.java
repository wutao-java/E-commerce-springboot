package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.ProductPromotion;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPromotionRepository extends JpaRepository<ProductPromotion, Long> {

    List<ProductPromotion> findByProductIdInAndActiveTrue(Collection<Long> productIds);

    List<ProductPromotion> findAllByOrderByIdDesc();

    void deleteByProductId(Long productId);
}
