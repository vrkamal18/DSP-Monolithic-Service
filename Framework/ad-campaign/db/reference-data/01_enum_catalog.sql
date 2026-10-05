-- Reference data: enum catalog.
--
-- This schema has no separate lookup/reference tables — every fixed set of
-- values (roles, statuses, connector types, etc.) is enforced directly as a
-- CHECK constraint on the owning table's column (see db/schema/*.sql), the
-- way Hibernate's @Enumerated(EnumType.STRING) generates them. There is
-- therefore nothing to INSERT here; this file is a single living catalog of
-- every valid value, kept in sync with the CHECK constraints, so an
-- integrator (UI, BI tool, ops script) has one place to look instead of
-- reading Java enums or grepping constraint definitions.
--
-- If a real reference/lookup table (e.g. to drive a UI dropdown from the
-- database instead of a hardcoded list) becomes genuinely needed, add it
-- here as an actual table + INSERT statements at that point — don't treat
-- this comment-only file as a substitute once that need is real.

-- identity.user_role_assignments.role
--   PLATFORM_ADMIN, TENANT_ADMIN, CAMPAIGN_MANAGER, CREATIVE_REVIEWER,
--   FINANCE_USER, ANALYST, SERVICE_PRINCIPAL

-- campaign.campaigns.status / campaign.campaign_status_changes.from_status,to_status
--   DRAFT, ACTIVE, PAUSED, COMPLETED, ARCHIVED

-- campaign.creatives.status
--   DRAFT, PENDING_REVIEW, APPROVED, REJECTED, ARCHIVED

-- channels.channels.type / channels.campaign_channels.channel_type
--   DISPLAY, NATIVE, VIDEO, AUDIO, OTT_CTV, DOOH, SOCIAL

-- connectors.connectors.type
--   SOCIAL, DMP, OTT

-- connectors.connectors.status
--   DISCONNECTED, CONNECTED, ERROR

-- connectors.connector_activities.activity_type
--   CONNECT, DISCONNECT, TEST, SYNC, PUSH_CAMPAIGN, PAUSE_CAMPAIGN, RESUME_CAMPAIGN

-- finance.billing.ledger_entries.entry_type
--   CHARGE, ADJUSTMENT

-- finance.payments.payment_transactions.type
--   TOP_UP, REFUND, AD_SPEND

-- finance.invoicing.invoices.status
--   DRAFT, ISSUED, SENT, PAID, VOID

-- measurement.measurement_events.event_type
--   IMPRESSION, CLICK, CONVERSION, VIEWABILITY, VIDEO_QUARTILE

-- measurement.measurement_events.video_quartile (nullable; only set when event_type = VIDEO_QUARTILE)
--   START, FIRST_QUARTILE, MIDPOINT, THIRD_QUARTILE, COMPLETE

-- administration.admin_audit_logs.action
--   USER_CREATED, USER_ROLE_GRANTED, USER_ROLE_REVOKED, USER_DEACTIVATED,
--   USER_REACTIVATED, TENANT_DEACTIVATED, TENANT_REACTIVATED
