package com.adc.adcampaign.campaign.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ad_clicks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creative_id")
    private Creative creative;

    private String userId;
    private String location;
    private String deviceType;

    @Column(name = "clicked_at", updatable = false)
    private LocalDateTime clickedAt;

    @PrePersist
    public void setClickedAt() {
        this.clickedAt = LocalDateTime.now();
    }
}

