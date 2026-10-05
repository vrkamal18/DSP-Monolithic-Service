package com.adc.adcampaign.identity.repository;

import com.adc.adcampaign.identity.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    List<AppUser> findByTenantId(Long tenantId);

    Optional<AppUser> findByIdAndTenantId(Long id, Long tenantId);
}
