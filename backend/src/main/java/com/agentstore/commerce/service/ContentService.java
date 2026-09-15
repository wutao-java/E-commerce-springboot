package com.agentstore.commerce.service;

import com.agentstore.commerce.dto.ApiModels.AfterSalePolicyResponse;
import com.agentstore.commerce.dto.ApiModels.FaqResponse;
import com.agentstore.commerce.repository.AfterSalePolicyRepository;
import com.agentstore.commerce.repository.FaqEntryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    private final AfterSalePolicyRepository policyRepository;
    private final FaqEntryRepository faqRepository;

    public ContentService(AfterSalePolicyRepository policyRepository, FaqEntryRepository faqRepository) {
        this.policyRepository = policyRepository;
        this.faqRepository = faqRepository;
    }

    @Transactional(readOnly = true)
    public List<AfterSalePolicyResponse> listPolicies() {
        return policyRepository.findAllByOrderByIdAsc().stream()
            .map(policy -> new AfterSalePolicyResponse(policy.getId(), policy.getSceneKey(), policy.getTitle(),
                policy.getContent(), policy.getApplicableConditions(), policy.getExclusionConditions(),
                policy.getRequiredEvidence(), policy.getRequiresManualReview()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<FaqResponse> listFaqs() {
        return faqRepository.findAllByOrderByIdAsc().stream()
            .map(faq -> new FaqResponse(faq.getId(), faq.getCategory(), faq.getQuestion(), faq.getAnswer()))
            .toList();
    }
}
