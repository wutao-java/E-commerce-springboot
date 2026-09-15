package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.Product;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsBySku(String sku);

    @Query("""
        select p from Product p
        where p.active = true
          and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%'))
               or lower(p.description) like lower(concat('%', :keyword, '%')))
          and (:category is null or p.category = :category)
        order by p.id asc
        """)
    List<Product> search(@Param("keyword") String keyword, @Param("category") String category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids")
    List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
