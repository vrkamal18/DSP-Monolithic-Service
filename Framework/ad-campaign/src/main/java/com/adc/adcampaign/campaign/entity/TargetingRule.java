package com.adc.adcampaign.campaign.entity;

import com.adc.adcampaign.identity.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "targeting_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TargetingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "rule_type")
    private String ruleType; // e.g., "location", "age", "interest", "device"

    @Column(name = "rule_value")
    private String ruleValue; // e.g., "US", "25-35", "Gaming"

    @Column(name = "condition_type")
    private String conditionType; // e.g., "include", "exclude"
}
