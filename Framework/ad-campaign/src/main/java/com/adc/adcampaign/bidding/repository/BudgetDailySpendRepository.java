package com.adc.adcampaign.bidding.repository;

import com.adc.adcampaign.bidding.entity.BudgetDailySpend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetDailySpendRepository extends JpaRepository<BudgetDailySpend, Long> {
}
