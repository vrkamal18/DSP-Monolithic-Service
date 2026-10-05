-- Seed data: one complete demo tenant ("Acme Demo"), built by actually
-- driving the application's real APIs against a fresh database and
-- capturing the result — not hand-typed. This guarantees every bcrypt
-- hash, idempotency key, and business-rule-dependent value (e.g. a creative
-- that's genuinely gone through submit -> approve) is exactly what the app
-- itself would have produced, not an approximation.
--
-- Run against a FRESH, empty schema only (right after db/schema/run_all.sql).
-- Explicit ids are used throughout (1, 2, ...) and the identity sequences
-- are reset at the end — safe only because nothing else has been inserted
-- yet.
--
-- Demo login credentials (DEMO ONLY — never reuse in a real environment):
--   admin@acmedemo.test   / ChangeMe123!   (TENANT_ADMIN)
--   manager@acmedemo.test / ChangeMe123!   (CAMPAIGN_MANAGER)
--
-- Not seeded: bid_decision_log, ad_impressions, ad_clicks, frequency_cap_entries,
-- budget_daily_spend, invoices, invoice_line_items — these are pure byproducts
-- of live OpenRTB bidding traffic or a billing-period invoice run; faking
-- historical rows for them would just be noise, not a realistic demo.
-- PLATFORM_ADMIN is deliberately not seeded here either — see
-- administration.PlatformAdminBootstrap and administration.bootstrap-admin-email
-- in application.properties for the one real way that role is ever granted.

-- ============================================================
-- identity
-- ============================================================

INSERT INTO tenants (id, name, active, created_at) VALUES
    (1, 'Acme Demo', NULL, now());

INSERT INTO app_users (id, tenant_id, email, password_hash, enabled, created_at) VALUES
    (1, 1, 'admin@acmedemo.test',   '$2a$10$G7Cb/7FnHZ/dHgxGS1P80e9QiS1.OMEpiQuYaYVNP0gTvNAuqiea2', true, now()),
    (2, 1, 'manager@acmedemo.test', '$2a$10$4rxL9ucNpHysHKRIX12N1eEgyF6wfRea2H2G9k8OtBbh8sPuX8Y4e', true, now());

INSERT INTO user_role_assignments (id, user_id, tenant_id, role) VALUES
    (1, 1, 1, 'TENANT_ADMIN'),
    (2, 2, 1, 'CAMPAIGN_MANAGER');

-- ============================================================
-- campaign (advertiser, campaign, budget, targeting, creative)
-- ============================================================

INSERT INTO advertisers (id, tenant_id, name, email, company, created_at) VALUES
    (1, 1, 'Northwind Traders', 'billing@northwind-demo.test', 'Northwind Traders Inc.', now());

INSERT INTO campaigns (id, tenant_id, advertiser_id, name, bid_price, objective, start_date, end_date, status, created_at) VALUES
    (1, 1, 1, 'Spring Awareness Push', 2.5000, 'AWARENESS', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'ACTIVE', now());

INSERT INTO budgets (id, tenant_id, campaign_id, total_budget, daily_cap, spent_amount, pacing_mode) VALUES
    (1, 1, 1, 5000.0000, 200.0000, NULL, 'STANDARD');

INSERT INTO targeting_rules (id, tenant_id, campaign_id, rule_type, condition_type, rule_value) VALUES
    (1, 1, 1, 'location', 'include', 'US');

INSERT INTO creatives (id, tenant_id, campaign_id, name, creative_type, url, width, height, status, rejection_reason, reviewed_by_user_id, reviewed_at, created_at) VALUES
    (1, 1, 1, 'Spring Banner 300x250', 'image', 'https://cdn.acmedemo.test/creatives/spring-banner.jpg', 300, 250, 'APPROVED', NULL, 1, now(), now());

INSERT INTO campaign_status_changes (id, tenant_id, campaign_id, from_status, to_status, changed_by, changed_at) VALUES
    (1, 1, 1, 'DRAFT', 'ACTIVE', 1, now());

-- ============================================================
-- channels
-- ============================================================

INSERT INTO channels (id, tenant_id, type, enabled) VALUES
    (1, 1, 'DISPLAY', true);

INSERT INTO campaign_channels (id, tenant_id, campaign_id, channel_type) VALUES
    (1, 1, 1, 'DISPLAY');

-- ============================================================
-- connectors
-- ============================================================

INSERT INTO connectors (id, tenant_id, type, status, external_account_id, credential_ref, connected_at, last_synced_at, last_tested_at, last_test_successful) VALUES
    (1, 1, 'SOCIAL', 'CONNECTED', 'acme-social-handle', 'demo-oauth-token', now(), now(), NULL, NULL);

INSERT INTO connector_activities (id, tenant_id, connector_id, activity_type, success, details, created_at) VALUES
    (1, 1, 1, 'CONNECT',       true, 'Connected SOCIAL account acme-social-handle', now()),
    (2, 1, 1, 'SYNC',          true, 'Synced 10 records', now()),
    (3, 1, 1, 'PUSH_CAMPAIGN', true, 'Pushed campaign 1 as mock_ext_campaign_demo', now());

INSERT INTO external_id_mappings (id, tenant_id, connector_id, campaign_id, external_campaign_id) VALUES
    (1, 1, 1, 1, 'mock_ext_campaign_demo');

-- ============================================================
-- finance.payments / finance.pricing / finance.billing
-- ============================================================

INSERT INTO payment_methods (id, tenant_id, advertiser_id, provider, token, brand, last4, expiry_month, expiry_year, created_at) VALUES
    (1, 1, 1, 'MOCK', 'demo-card-tok', 'VISA', '4242', 12, 2030, now());

INSERT INTO accounts (id, tenant_id, advertiser_id, balance, currency) VALUES
    (1, 1, 1, 2500.0000, 'USD');

INSERT INTO payment_transactions (id, tenant_id, advertiser_id, type, amount, currency, source_event_id, provider_charge_id, created_at) VALUES
    (1, 1, 1, 'TOP_UP', 2500.0000, 'USD', 'seed-topup-1', 'mock_demo_charge', now());

INSERT INTO tenant_fee_configs (id, tenant_id, platform_fee_percent) VALUES
    (1, 1, 0.0800);

INSERT INTO ledger_entries (id, tenant_id, campaign_id, entry_type, source_event_id, media_cost, platform_fee, total_amount, currency, created_by, created_at) VALUES
    (1, 1, 1, 'ADJUSTMENT', 'seed-adj-1', NULL, NULL, 50.0000, 'USD', 1, now());

-- ============================================================
-- measurement
-- ============================================================

INSERT INTO measurement_events (id, tenant_id, campaign_id, creative_id, event_type, event_id, destination_url, conversion_type, conversion_value, viewable_duration_ms, viewable_percentage, video_quartile, recorded_at) VALUES
    (1, 1, 1, 1, 'IMPRESSION', 'seed-imp-1',  NULL,                             NULL,       NULL,  NULL, NULL, NULL, now()),
    (2, 1, 1, 1, 'IMPRESSION', 'seed-imp-2',  NULL,                             NULL,       NULL,  NULL, NULL, NULL, now()),
    (3, 1, 1, 1, 'CLICK',      'seed-clk-1',  'https://acmedemo.test/landing',  NULL,       NULL,  NULL, NULL, NULL, now()),
    (4, 1, 1, NULL, 'CONVERSION', 'seed-conv-1', NULL,                          'purchase', 75.00, NULL, NULL, NULL, now());

-- ============================================================
-- administration
-- ============================================================

INSERT INTO admin_audit_logs (id, actor_user_id, actor_tenant_id, target_tenant_id, target_user_id, action, details, correlation_id, created_at) VALUES
    (1, 1, 1, 1, 2, 'USER_CREATED', 'Created user manager@acmedemo.test with role CAMPAIGN_MANAGER', NULL, now());

-- ============================================================
-- Reset identity sequences past the explicit ids used above, so the next
-- row the application inserts doesn't collide with seeded ids.
-- ============================================================

SELECT setval('tenants_id_seq', 1, true);
SELECT setval('app_users_id_seq', 2, true);
SELECT setval('user_role_assignments_id_seq', 2, true);
SELECT setval('advertisers_id_seq', 1, true);
SELECT setval('campaigns_id_seq', 1, true);
SELECT setval('budgets_id_seq', 1, true);
SELECT setval('targeting_rules_id_seq', 1, true);
SELECT setval('creatives_id_seq', 1, true);
SELECT setval('campaign_status_changes_id_seq', 1, true);
SELECT setval('channels_id_seq', 1, true);
SELECT setval('campaign_channels_id_seq', 1, true);
SELECT setval('connectors_id_seq', 1, true);
SELECT setval('connector_activities_id_seq', 3, true);
SELECT setval('external_id_mappings_id_seq', 1, true);
SELECT setval('payment_methods_id_seq', 1, true);
SELECT setval('accounts_id_seq', 1, true);
SELECT setval('payment_transactions_id_seq', 1, true);
SELECT setval('tenant_fee_configs_id_seq', 1, true);
SELECT setval('ledger_entries_id_seq', 1, true);
SELECT setval('measurement_events_id_seq', 4, true);
SELECT setval('admin_audit_logs_id_seq', 1, true);
