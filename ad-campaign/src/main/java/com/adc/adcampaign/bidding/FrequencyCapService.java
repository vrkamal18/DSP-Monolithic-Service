package com.adc.adcampaign.bidding;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Durable, cross-instance frequency capping backed by Postgres instead of an
 * in-process map. The increment-and-fetch is a single atomic upsert so
 * concurrent bid requests for the same user/campaign/day can't race.
 */
@Service
public class FrequencyCapService {

    private static final String UPSERT_AND_RETURN_COUNT_SQL = """
            INSERT INTO frequency_cap_entries (campaign_id, user_id, cap_date, impression_count)
            VALUES (?, ?, ?, 1)
            ON CONFLICT (campaign_id, user_id, cap_date)
            DO UPDATE SET impression_count = frequency_cap_entries.impression_count + 1
            RETURNING impression_count
            """;

    private final JdbcTemplate jdbcTemplate;

    public FrequencyCapService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns true if, after crediting this impression, the user is still
     * within {@code maxImpressionsPerDay} for the given campaign today.
     * The credit is applied unconditionally; callers that decide not to bid
     * after all should not call this method for that impression.
     */
    public boolean tryConsume(Long campaignId, String userId, int maxImpressionsPerDay) {
        if (userId == null || userId.isBlank()) {
            // No stable user identifier to cap against; do not block the bid.
            return true;
        }
        Integer newCount = jdbcTemplate.queryForObject(
                UPSERT_AND_RETURN_COUNT_SQL,
                Integer.class,
                campaignId, userId, Date.valueOf(LocalDate.now()));
        return newCount != null && newCount <= maxImpressionsPerDay;
    }
}
