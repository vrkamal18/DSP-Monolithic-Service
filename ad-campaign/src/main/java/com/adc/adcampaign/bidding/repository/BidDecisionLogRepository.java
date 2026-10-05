package com.adc.adcampaign.bidding.repository;

import com.adc.adcampaign.bidding.entity.BidDecisionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BidDecisionLogRepository extends JpaRepository<BidDecisionLog, Long> {
}
