package com.adc.adcampaign.bidding;

import com.adc.adcampaign.campaign.entity.Budget;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Budget pacing and spend accounting backed by Postgres. Replaces the
 * per-instance, never-persisted {@code dailySpendMap}s that previously lived
 * in {@code OpenRTBBidderService} and {@code AdImpressionService}. Money is
 * {@link BigDecimal} throughout, per
 * service/documents/03-domain-and-data-specification.md §5.
 */
@Service
public class PacingService {

    private static final String UPSERT_DAILY_SPEND_SQL = """
            INSERT INTO budget_daily_spend (campaign_id, spend_date, spent_amount)
            VALUES (?, ?, ?)
            ON CONFLICT (campaign_id, spend_date)
            DO UPDATE SET spent_amount = budget_daily_spend.spent_amount + EXCLUDED.spent_amount
            RETURNING spent_amount
            """;

    private static final String SELECT_DAILY_SPEND_SQL = """
            SELECT spent_amount FROM budget_daily_spend WHERE campaign_id = ? AND spend_date = ?
            """;

    private static final String ATOMIC_TOTAL_SPEND_INCREMENT_SQL = """
            UPDATE budgets SET spent_amount = COALESCE(spent_amount, 0) + ? WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PacingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasBudget(Budget budget) {
        BigDecimal spent = budget.getSpentAmount() == null ? BigDecimal.ZERO : budget.getSpentAmount();
        BigDecimal total = budget.getTotalBudget() == null ? BigDecimal.ZERO : budget.getTotalBudget();
        if (spent.compareTo(total) >= 0) {
            return false;
        }

        if (!"STANDARD".equalsIgnoreCase(budget.getPacingMode())) {
            // ACCELERATED or unset: only the total budget matters.
            return true;
        }

        LocalDate start = budget.getCampaign().getStartDate().toLocalDate();
        LocalDate end = budget.getCampaign().getEndDate().toLocalDate();
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        if (totalDays <= 0) {
            return true;
        }

        BigDecimal dailyCap = total.divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);
        BigDecimal spentToday = jdbcTemplate.query(
                SELECT_DAILY_SPEND_SQL,
                rs -> rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO,
                budget.getCampaign().getId(), Date.valueOf(LocalDate.now()));

        return (spentToday == null ? BigDecimal.ZERO : spentToday).compareTo(dailyCap) < 0;
    }

    /**
     * Atomically records that {@code price} was spent by {@code campaignId} just now,
     * updating both the campaign's total spend and today's pacing bucket.
     */
    public void recordSpend(Long budgetId, Long campaignId, BigDecimal price) {
        jdbcTemplate.update(ATOMIC_TOTAL_SPEND_INCREMENT_SQL, price, budgetId);
        jdbcTemplate.queryForObject(
                UPSERT_DAILY_SPEND_SQL,
                BigDecimal.class,
                campaignId, Date.valueOf(LocalDate.now()), price);
    }
}
