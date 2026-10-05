package com.adc.adcampaign.bidding.repository;

import com.adc.adcampaign.bidding.entity.FrequencyCapEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrequencyCapEntryRepository extends JpaRepository<FrequencyCapEntry, Long> {
}
