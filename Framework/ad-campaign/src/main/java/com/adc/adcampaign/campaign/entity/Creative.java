package com.adc.adcampaign.campaign.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "creatives")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Column(name = "creative_type")
    private String type; // e.g., "image", "video", "html5"

    private String url; // Link to the hosted file (CDN or internal)

    private Integer width;
    private Integer height;

    private String status; // e.g., "ACTIVE", "INACTIVE"

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
