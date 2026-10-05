package com.adc.adcampaign.bidding.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bid_decision_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidDecisionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_request_id", nullable = false)
    private String bidRequestId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "exchange", nullable = false)
    private String exchange;

    @Column(name = "outcome", nullable = false)
    private String outcome; // "BID" or "NO_BID"

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(precision = 12, scale = 4)
    private BigDecimal price;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
