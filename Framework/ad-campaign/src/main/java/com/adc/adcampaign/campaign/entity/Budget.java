package com.adc.adcampaign.campaign.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "total_budget", nullable = false)
    private Double totalBudget;

    @Column(name = "daily_cap")
    private Double dailyCap;

    @Column(name = "spent_amount")
    private Double spentAmount;

    @Column(name = "pacing_mode")
    private String pacingMode; // STANDARD, ACCELERATED
}

