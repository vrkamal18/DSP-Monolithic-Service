# AdCampaign — features and implementation status

A Spring Boot modular-monolith DSP (Demand-Side Platform) backend, built to
full functional parity with the 15-module microservice design in
`service/documents/`. This document is the single source of truth for what's
actually built, what's deliberately deferred (and why), and what remains —
kept current as of this writing: **125 tests across 27 test classes, all
passing**; every feature below has been exercised live against a real local
Postgres instance, not just unit-tested.

## What this is

An ad-tech backend covering the full lifecycle of running programmatic
advertising campaigns: tenant/user management, campaign and creative
management, real-time OpenRTB bid decisioning against a live exchange
(BidsCube), budget pacing and billing, advertiser payments, invoicing,
multi-channel distribution, third-party platform connectors, measurement
tracking pixels, performance analytics, and platform administration — all
under strict multi-tenant isolation.

## Architecture at a glance

- **Modular monolith**, one Spring Boot deployable, packages enforcing the
  same module boundaries and dependency directions the original
  microservice design specified (`service/documents/01-service-architecture.md
  §6`) — e.g. `bidding` may depend on `campaign`/`finance`, but `campaign`
  may never depend on `finance` or `channels`, even where that would be
  convenient. This discipline was held even when it meant a feature had to
  be built in a different module than first expected (see "Payments fund
  check" below).
- **Multi-tenancy**: every tenant-scoped table carries a `tenant_id`;
  `foundation.security.TenantContext` resolves the caller's tenant from the
  JWT, never from a client-supplied request field.
- **Auth**: JWT bearer tokens, role-based `@PreAuthorize` on every
  authenticated endpoint. Roles: `PLATFORM_ADMIN`, `TENANT_ADMIN`,
  `CAMPAIGN_MANAGER`, `CREATIVE_REVIEWER`, `FINANCE_USER`, `ANALYST`,
  `SERVICE_PRINCIPAL`.
- **Errors**: RFC 9457 `application/problem+json` everywhere, via a single
  `foundation.error.ApiExceptionHandler`.
- **Idempotency**: every financial or externally-triggered write
  (`LedgerEntry`, `Transaction`, `MeasurementEvent`) uses the same pattern —
  a raw `INSERT ... ON CONFLICT (tenant_id, source_event_id) DO NOTHING
  RETURNING id`, reused five times across Billing, Payments, and
  Measurement rather than inventing a new mechanism each time.
- **No Flyway** — schema is hand-maintained SQL under `db/` (see
  `db/README.md`), verified byte-identical to what the JPA entities
  actually produce.

---

## Module-by-module status

### 1. Identity — ✅ complete
JWT register/login, tenant + user + role-assignment model.
`POST /api/v1/auth/register`, `POST /api/v1/auth/login`.

### 2. Advertiser — ✅ complete (not yet split into its own package)
Full CRUD. Lives inside the `campaign` package rather than its own
top-level module — functionally complete, architecturally not separated
from spec's module boundary.
`POST|GET|PUT|DELETE /api/v1/advertisers[/{id}]`.

### 3. Campaign — ✅ complete
Full lifecycle state machine (`DRAFT → ACTIVE ⇄ PAUSED → COMPLETED/ARCHIVED`),
with activation eligibility validation (advertiser set, positive budget,
≥1 approved creative, valid schedule) and a full audit trail
(`campaign_status_changes`).
`POST|GET|PUT|DELETE /api/v1/campaigns[/{id}]`,
`GET /api/v1/campaigns/advertiser/{advertiserId}`,
`POST /api/v1/campaigns/{id}/{activate|pause|resume|complete|archive}`.
Budget sub-resource: `POST|GET|DELETE /api/v1/budgets[/{id}]`,
`GET /api/v1/budgets/campaign/{campaignId}`.

### 4. Audience — ⚠️ partial
Only flat location/device include-exclude `TargetingRule`s exist — no
segment or DMP-style audience definitions. This is the one module where
"built" meaningfully understates the spec's ambition for it.
`POST|GET|PUT|DELETE /api/v1/targeting-rules[/{id}]`,
`GET /api/v1/targeting-rules/campaign/{campaignId}`.

### 5. Creative — ✅ complete (not yet split into its own package)
Full review workflow (`DRAFT → PENDING_REVIEW → APPROVED/REJECTED →
ARCHIVED`), role-gated so only `CREATIVE_REVIEWER`/`TENANT_ADMIN` can
approve/reject. Same packaging caveat as Advertiser.
`POST|GET|PUT|DELETE /api/v1/creatives[/{id}]`,
`GET /api/v1/creatives/campaign/{campaignId}`,
`POST /api/v1/creatives/{id}/{submit|approve|reject|archive}`.

### 6. Finance — ✅ complete (four sub-areas)
- **Billing** (`finance.billing`): append-only ledger (`CHARGE`/
  `ADJUSTMENT`), idempotent win-charging from the bidding path, win-notice
  reconciliation against the real clearing price.
  `GET /api/v1/billing/entries[/campaign/{id}]`,
  `POST /api/v1/billing/adjustments`.
- **Payments** (`finance.payments`): advertiser funding accounts, saved
  payment methods (behind a mock provider adapter — no real processor
  chosen yet), top-up/refund, and **`AD_SPEND` debits applied automatically
  on every bidding win** (see "Payments fund check," below).
  `GET /api/v1/accounts/{advertiserId}/{balance|transactions}`,
  `POST /api/v1/accounts/{advertiserId}/{topup|refund}`,
  `POST|GET|DELETE /api/v1/payment-methods[/{id}]`.
- **Invoicing** (`finance.invoicing`): generates an invoice + line items
  from a billing period's ledger entries, real PDF rendering (Apache
  PDFBox), full state machine (`DRAFT → ISSUED → SENT → PAID`/`VOID`),
  mock email delivery adapter.
  `POST|GET /api/v1/invoices[/{id}]`, `GET /api/v1/invoices/{id}/document`,
  `POST /api/v1/invoices/{id}/{issue|send|mark-paid|void}`.
- **Pricing** (`finance.pricing`): per-tenant platform-fee percentage.
  `GET|PUT /api/v1/fees`.

### 7. Bidding — ✅ complete for banner; video/native/loss-notices not built
Vendor-neutral decisioning: eligibility (active/scheduled, targeting match,
budget pacing, **funds check**, approved creative), frequency capping,
highest-bid-wins selection, idempotent charge, full decision audit log —
all exchange-agnostic (`bidding.BiddingService` takes only the normalized
`AuctionRequest`, so a second exchange adapter could reuse it unchanged).
No dedicated REST surface — consumed internally by `openrtb`/`exchange`.

### 8. OpenRTB — ✅ complete for banner
Generic OpenRTB 2.5/2.6-ish request/response DTOs, validation, and
mapping to/from the internal `AuctionRequest`/`BidDecision` model — no
vendor-specific types leak past this layer.

### 9. Exchange (BidsCube adapter) — ✅ complete for banner
The one real exchange integration: shared-secret header auth, `tmax`
deadline enforcement via a dedicated bounded executor (timeouts/overload
resolve to a plain no-bid, never an error), win-notice callback.
`POST /exchange/bidscube/openrtb2/bidrequest` (header-authenticated),
`GET /exchange/bidscube/win` (public win-notice pixel, unguessable
embedded event id).

### 10. Channels — ✅ complete standalone; not wired into live bids
Per-tenant channel-type enablement (`DISPLAY`/`NATIVE`/`VIDEO`/`AUDIO`/
`OTT_CTV`/`DOOH`/`SOCIAL`) and campaign-to-channel assignment, plus a
markup-preview calculator. **Not** wired into the live OpenRTB bid
response — see "Known, deliberate gaps" below.
`GET|PUT /api/v1/channels[/{type}]`,
`GET /api/v1/channels/markup-preview/{creativeId}`,
`POST|GET|DELETE /api/v1/campaigns/{campaignId}/channels[/{channelType}]`.

### 11. Connectors — ✅ complete for its defined scope
Connect/disconnect/test/sync lifecycle for `SOCIAL`/`DMP`/`OTT` platforms
(behind a mock adapter — no real platform chosen yet), campaign push/
pause/resume with external-id mapping, full activity/sync audit log. No
structured per-platform performance metrics yet (flagged during the
Analytics build as a real but not-yet-needed gap).
`POST|GET /api/v1/connectors[/{id}]`, `GET /api/v1/connectors/{id}/activity`,
`POST /api/v1/connectors/{id}/{disconnect|test|sync}`,
`POST /api/v1/connectors/{id}/campaigns/{campaignId}/{push|pause|resume}`.

### 12. Measurement — ✅ complete
Public, unauthenticated tracking endpoints under `/t/**`: impression/click/
conversion pixels (real 1×1 GIF response), viewability and VAST video-
quartile beacons. Idempotent per event, click-redirect destination
validated against open-redirect abuse, and a dedicated rate-limiting
filter (fixed-window, per-client).
`GET /t/impression`, `GET /t/click`, `GET /t/conversion`,
`POST /t/viewability`, `POST /t/video-quartile`,
`GET /api/v1/measurement/campaigns/{campaignId}/summary` (authenticated).

### 13. Analytics — ✅ complete for its defined scope
On-demand (not materialized/pre-aggregated — no event pipeline exists yet)
campaign and advertiser performance: CTR, CVR, CPM, CPC, CPA, ROAS, ROI,
VCR, engagement, CPE, CPL, computed from real Measurement + Billing data.
Cross-channel summary is connector sync-health only (see Connectors'
metrics gap above). CSV export.
`GET /api/v1/analytics/campaigns/{id}[/export.csv]`,
`GET /api/v1/analytics/advertisers/{id}`,
`GET /api/v1/analytics/channels/summary`.

### 14. Administration — ✅ complete
Tenant-scoped user management (invite, list, grant/revoke role, deactivate/
reactivate — with guardrails against removing a tenant's last admin or
deactivating yourself), platform-wide cross-tenant administration (list
all tenants, deactivate/reactivate a tenant — which genuinely blocks
login, not cosmetic), a cross-tenant operational health summary, and a
real append-only audit log (actor/tenant/action/target/time/correlation
id) for every administrative action. `PLATFORM_ADMIN` is granted only via
an explicit, documented startup bootstrap (`administration.
bootstrap-admin-email`) — there is no other way to obtain it, by design.
`POST|GET /api/v1/admin/users`, `POST /api/v1/admin/users/{id}/roles`,
`DELETE /api/v1/admin/users/{id}/roles/{role}`,
`POST /api/v1/admin/users/{id}/{deactivate|reactivate}`,
`GET /api/v1/admin/tenants`,
`POST /api/v1/admin/tenants/{id}/{deactivate|reactivate}`,
`GET /api/v1/admin/platform/health`, `GET /api/v1/admin/audit-log`.

### 15. Foundation — ✅ complete, grows as needed
Shared kernel: `TenantContext`/`TenantPrincipal`, `ApiExceptionHandler`,
`CorrelationIdFilter`, `FilterResponses` (shared filter-rejection helper —
see below), Jackson/Hibernate wiring. Not a module with its own features;
extended whenever a genuinely shared cross-cutting need appeared.

---

## Cross-module correctness work (beyond individual modules)

- **Payments fund check, wired into bidding, not campaign activation.**
  The dependency rules don't allow `campaign` to depend on `finance`, but
  `bidding` already legitimately does — so the real fix for "don't let a
  campaign with no funds keep winning bids" lives in
  `BiddingService.decide()`'s eligibility loop, checking the advertiser's
  Payments account balance live, per bid. `BillingService.chargeForWin`
  debits that same account automatically on every genuinely new charge.
  Verified live: a drained account correctly stops winning bids even
  though budget/creative/schedule remain otherwise eligible.
- **Two real bugs found only by live testing, not unit tests:**
  - A `LazyInitializationException` in the bidding hot path (not
    `@Transactional` by design, so a lazy JPA association loaded in an
    earlier short-lived session threw when touched later) — fixed with a
    scalar JPQL projection instead of lazy navigation
    (`CampaignRepository.findAdvertiserIdById`).
  - `BidsCubeAuthFilter`'s request-too-large rejection (413) was silently
    overwritten to 401 in the real servlet container, because
    `HttpServletResponse.sendError()` triggers an internal `/error`
    re-dispatch that re-enters Spring Security's filter chain a second
    time. Fixed (and centralized as `foundation.web.FilterResponses`, used
    by both this filter and Measurement's rate limiter) after reproducing
    the bug live, not just reasoning about it.

## Known, deliberate gaps (documented, not oversights)

| Gap | Why it's deferred |
|---|---|
| Channels markup not applied to the live OpenRTB bid response | Nothing upstream of `channels` (`bidding`/`openrtb`) is allowed to depend on it per the dependency rules — would require an event-based redesign, not a small wire-up |
| No video/native OpenRTB support, no loss notices (`lurl`) | Banner + win-notices only; a real scope expansion, not a bug |
| Audience is flat targeting rules only | No segment/DMP module exists; nothing currently needs more than include/exclude matching |
| Advertiser/Creative not split into their own top-level packages | Functionally complete; a pure structural refactor with no behavior change, not prioritized |
| Connectors has no structured per-platform metrics | `ConnectorActivity` is a sync/action audit log, not a metrics feed — Analytics' cross-channel summary is scoped accordingly |
| Win-notice reconciliation and manual billing adjustments don't adjust the Payments account balance | Only the first-charge (`chargeForWin`) path does; reconciliation deltas need sign-aware handling that's genuinely separate work |
| Legacy `/api/clicks`, `ad_impressions`, `ad_clicks` | Unauthenticated endpoint and tables superseded by Measurement's `/t/**`, kept only because the app still writes to them |

## Not yet built

- **UI** — `ui/` exists as an empty placeholder; no frontend wired to this
  API yet.
- **Deployment readiness** — no CORS config, no Spring profiles/
  environment-specific secrets separation beyond the env-var overrides
  already in `application.properties`, no Actuator, no Dockerfile, no CI
  pipeline.

## Developer resources

- **`db/`** — hand-written SQL schema (verified column-for-column
  identical to what the JPA entities generate), an enum-value reference
  catalog, and a realistic seed dataset built by actually driving the real
  APIs. See `db/README.md`.
- **`app-setup/`** — complete new-machine install guide (macOS and
  Windows), with automation scripts. The macOS path has been run
  end-to-end against a simulated fresh machine; see `app-setup/README.md`.
