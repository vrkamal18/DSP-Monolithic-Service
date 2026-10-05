package com.adc.adcampaign.measurement.repository;

import com.adc.adcampaign.measurement.entity.MeasurementEvent;
import com.adc.adcampaign.measurement.entity.MeasurementEventType;
import com.adc.adcampaign.measurement.entity.VideoQuartile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface MeasurementEventRepository extends JpaRepository<MeasurementEvent, Long> {
    long countByTenantIdAndCampaignIdAndEventType(Long tenantId, Long campaignId, MeasurementEventType eventType);

    long countByTenantIdAndCampaignIdAndEventTypeAndVideoQuartile(
            Long tenantId, Long campaignId, MeasurementEventType eventType, VideoQuartile videoQuartile);

    @Query("SELECT COALESCE(SUM(e.conversionValue), 0) FROM MeasurementEvent e "
            + "WHERE e.tenantId = :tenantId AND e.campaignId = :campaignId AND e.eventType = 'CONVERSION'")
    BigDecimal sumConversionValueByTenantIdAndCampaignId(@Param("tenantId") Long tenantId,
                                                          @Param("campaignId") Long campaignId);
}
