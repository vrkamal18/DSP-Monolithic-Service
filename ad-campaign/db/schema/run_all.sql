-- Creates every table in dependency order. Run with:
--   psql -d <database> -f db/schema/run_all.sql
-- \ir resolves relative to this file's own location, so it works regardless
-- of the caller's current directory.

\ir 01_identity.sql
\ir 02_campaign.sql
\ir 03_bidding.sql
\ir 04_finance_billing.sql
\ir 05_finance_payments.sql
\ir 06_finance_invoicing.sql
\ir 07_finance_pricing.sql
\ir 08_channels.sql
\ir 09_connectors.sql
\ir 10_measurement.sql
\ir 11_administration.sql
