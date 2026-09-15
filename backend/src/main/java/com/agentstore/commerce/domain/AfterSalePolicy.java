package com.agentstore.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "after_sale_policies")
public class AfterSalePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_key", nullable = false, unique = true, length = 60)
    private String sceneKey;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "applicable_conditions", nullable = false, length = 500)
    private String applicableConditions;

    @Column(name = "exclusion_conditions", nullable = false, length = 500)
    private String exclusionConditions;

    @Column(name = "required_evidence", nullable = false, length = 500)
    private String requiredEvidence;

    @Column(name = "requires_manual_review", nullable = false)
    private Boolean requiresManualReview;

    protected AfterSalePolicy() {
    }

    public AfterSalePolicy(String sceneKey, String title, String content, String applicableConditions,
                           String exclusionConditions, String requiredEvidence, Boolean requiresManualReview) {
        this.sceneKey = sceneKey;
        this.title = title;
        this.content = content;
        this.applicableConditions = applicableConditions;
        this.exclusionConditions = exclusionConditions;
        this.requiredEvidence = requiredEvidence;
        this.requiresManualReview = requiresManualReview;
    }

    public Long getId() { return id; }
    public String getSceneKey() { return sceneKey; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getApplicableConditions() { return applicableConditions; }
    public String getExclusionConditions() { return exclusionConditions; }
    public String getRequiredEvidence() { return requiredEvidence; }
    public Boolean getRequiresManualReview() { return requiresManualReview; }
}
