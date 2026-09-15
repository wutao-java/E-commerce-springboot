package com.agentstore.commerce.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "faq_entries")
public class FaqEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 300)
    private String question;

    @Column(nullable = false, length = 1000)
    private String answer;

    protected FaqEntry() {
    }

    public FaqEntry(String category, String question, String answer) {
        this.category = category;
        this.question = question;
        this.answer = answer;
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
}
