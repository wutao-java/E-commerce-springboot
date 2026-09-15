package com.agentstore.commerce.repository;

import com.agentstore.commerce.domain.FaqEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqEntryRepository extends JpaRepository<FaqEntry, Long> {

    List<FaqEntry> findAllByOrderByIdAsc();
}
