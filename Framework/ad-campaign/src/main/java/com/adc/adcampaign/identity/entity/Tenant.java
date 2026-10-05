package com.adc.adcampaign.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Nullable, not primitive: lets this column be added via Hibernate
     * ddl-auto=update against existing rows with no default value needed.
     * {@code null} or {@code true} both mean active; only {@code false}
     * means deactivated.
     */
    private Boolean active;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
