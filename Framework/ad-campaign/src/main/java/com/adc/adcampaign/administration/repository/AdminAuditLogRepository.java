package com.adc.adcampaign.administration.repository;

import com.adc.adcampaign.administration.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findAllByOrderByCreatedAtDesc();

    List<AdminAuditLog> findByActorTenantIdOrTargetTenantIdOrderByCreatedAtDesc(Long actorTenantId, Long targetTenantId);
}
