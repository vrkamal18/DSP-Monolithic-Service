package com.adc.adcampaign.administration.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Append-only audit record for administrative actions, per
 * service/documents/03-domain-and-data-specification.md §8 ("actor, tenant,
 * action, target, time, correlation ID") and
 * service/documents/06-security-and-multitenancy.md §11. No update/delete
 * anywhere in this codebase — protected from tenant modification by
 * construction, same append-only discipline as {@code LedgerEntry}.
 */
@Entity
@Table(name = "admin_audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "actor_tenant_id", nullable = false)
    private Long actorTenantId;

    @Column(name = "target_tenant_id")
    private Long targetTenantId;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminAuditAction action;

    private String details;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void setCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
