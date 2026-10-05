# Database scripts

Hand-written SQL, not Flyway/Liquibase — generated from and verified against
the actual schema the application's JPA entities produce (via a throwaway
database, `hibernate.ddl-auto=update`, and `pg_dump --schema-only`), then
replayed end-to-end against a fresh empty database to confirm it's correct.
Going forward, this directory is the source of truth for schema changes in
any environment that isn't using `ddl-auto=update` for convenience (i.e.
anywhere other than local dev) — if you add/change a JPA entity, update the
matching file here too.

## Layout

- **`schema/`** — one `CREATE TABLE` file per module, in dependency order,
  plus `run_all.sql` to create everything in one shot:
  ```
  psql -d <database> -f db/schema/run_all.sql
  ```
- **`reference-data/`** — see its own header comment. This schema has no
  separate lookup tables (every enum is a `CHECK` constraint on the owning
  column, matching how Hibernate generates them), so there's nothing to
  `INSERT` — the one file here is a living catalog of valid values instead,
  for anyone integrating directly with the database.
- **`seed-data/`** — one realistic demo tenant ("Acme Demo"), built by
  actually driving the application's real APIs against a fresh database and
  capturing the result (not hand-typed), so bcrypt hashes and every
  business-rule-dependent value are exactly what the app itself produced.
  Run only against a freshly-created schema:
  ```
  psql -d <database> -f db/schema/run_all.sql
  psql -d <database> -f db/seed-data/01_demo_tenant.sql
  ```
  Demo login (**demo only** — never reuse these credentials anywhere real):
  `admin@acmedemo.test` / `ChangeMe123!` (`TENANT_ADMIN`), and
  `manager@acmedemo.test` / `ChangeMe123!` (`CAMPAIGN_MANAGER`).

## Design notes carried over from the entities

- **Plain ids, not foreign keys, almost everywhere.** Most tables
  (`ledger_entries`, `payment_transactions`, `measurement_events`,
  `connector_activities`, `admin_audit_logs`, `bid_decision_log`,
  `budget_daily_spend`, `frequency_cap_entries`, `channels`,
  `campaign_channels`, `connectors`, `external_id_mappings`,
  `campaign_status_changes`, `invoice_line_items`) store `tenant_id` /
  `campaign_id` / etc. as plain columns with **no** foreign key — this
  mirrors the JPA entities exactly (plain `Long` fields, not `@ManyToOne`)
  and is deliberate: those modules are intentionally decoupled from
  `campaign`'s/`identity`'s schema per the module dependency rules in
  `service/documents/01-service-architecture.md §6`. Only the older
  `campaign`-package tables (`campaigns`, `budgets`, `creatives`,
  `targeting_rules`, `advertisers`, `app_users`, `user_role_assignments`,
  `tenant_fee_configs`) use real foreign keys.
- **Enums are `CHECK` constraints**, not lookup tables — see
  `reference-data/01_enum_catalog.sql`.
- **`ad_impressions`/`ad_clicks`** are legacy: the former is bidding's
  internal "bid won" audit record (not a real render/click pixel), the
  latter sits behind the superseded, unauthenticated `/api/clicks`
  endpoint. Real tracking is `measurement_events` via `/t/**`. Both are kept
  only because the application still writes to them; don't build anything
  new against them.
- **No `PLATFORM_ADMIN` row is seeded.** The only way that role is ever
  granted is `administration.PlatformAdminBootstrap`, reading
  `administration.bootstrap-admin-email` from `application.properties` /
  the `ADMIN_BOOTSTRAP_EMAIL` env var against an already-registered user —
  set it to one of the seeded demo emails and restart the app to exercise
  it, rather than faking the row here.
- **Not seeded at all**: `bid_decision_log`, `ad_impressions`, `ad_clicks`,
  `frequency_cap_entries`, `budget_daily_spend`, `invoices`,
  `invoice_line_items` — these only make sense as byproducts of live OpenRTB
  bidding traffic or a real billing-period invoice run; fabricating
  historical-looking rows for them would be noise, not a useful demo.
