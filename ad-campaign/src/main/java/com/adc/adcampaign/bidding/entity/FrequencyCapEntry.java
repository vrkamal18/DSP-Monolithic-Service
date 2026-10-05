package com.adc.adcampaign.bidding.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "frequency_cap_entries", uniqueConstraints = @UniqueConstraint(
        name = "uk_frequency_cap_entry", columnNames = {"campaign_id", "user_id", "cap_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrequencyCapEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "cap_date", nullable = false)
    private LocalDate capDate;

    @Column(name = "impression_count", nullable = false)
    private Integer impressionCount;
}
