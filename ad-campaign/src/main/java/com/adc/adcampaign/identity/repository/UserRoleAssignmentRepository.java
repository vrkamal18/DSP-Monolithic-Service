package com.adc.adcampaign.identity.repository;

import com.adc.adcampaign.identity.entity.Role;
import com.adc.adcampaign.identity.entity.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, Long> {
    List<UserRoleAssignment> findByUserId(Long userId);

    List<UserRoleAssignment> findByTenantId(Long tenantId);

    Optional<UserRoleAssignment> findByUserIdAndTenantIdAndRole(Long userId, Long tenantId, Role role);

    long countByTenantIdAndRole(Long tenantId, Role role);

    Optional<UserRoleAssignment> findByUserIdAndTenantIsNullAndRole(Long userId, Role role);
}
