package com.adc.adcampaign.finance.invoicing.repository;

import com.adc.adcampaign.finance.invoicing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByTenantId(Long tenantId);
    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);
    Optional<Invoice> findByTenantIdAndAdvertiserIdAndPeriodStartAndPeriodEnd(
            Long tenantId, Long advertiserId, LocalDate periodStart, LocalDate periodEnd);
}
