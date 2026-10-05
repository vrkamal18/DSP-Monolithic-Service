package com.adc.adcampaign.bidding.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budget_daily_spend", uniqueConstraints = @UniqueConstraint(
        name = "uk_budget_daily_spend", columnNames = {"campaign_id", "spend_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetDailySpend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "spend_date", nullable = false)
    private LocalDate spendDate;

    @Column(name = "spent_amount", nullable = false, precision = 14, scale = 4)
    private BigDecimal spentAmount;
}
