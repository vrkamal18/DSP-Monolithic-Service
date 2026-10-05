package com.adc.adcampaign.campaign.entity;

import com.adc.adcampaign.identity.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "total_budget", nullable = false, precision = 14, scale = 4)
    private BigDecimal totalBudget;

    @Column(name = "daily_cap", precision = 14, scale = 4)
    private BigDecimal dailyCap;

    @Column(name = "spent_amount", precision = 14, scale = 4)
    private BigDecimal spentAmount;

    @Column(name = "pacing_mode")
    private String pacingMode; // STANDARD, ACCELERATED
}

