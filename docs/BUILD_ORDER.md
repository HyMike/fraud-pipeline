# Build Order

## The Principle
Build dependencies before the things that depend on them. Each phase has a clear deliverable you can verify before moving on.

---

## Phase 1 — PostgreSQL Schema + Flyway
**Start here. Everything depends on the database.**

- Write Flyway SQL migration files for `accounts`, `journal_entries`, `cases` tables
- Run PostgreSQL in Docker
- Verify tables exist with `psql`

No application code in this phase — just SQL.

---

## Phase 2 — Fraud Scoring Service (Python/FastAPI)
**Build before the Payment Service because Payment Service calls it.**

1. Download IEEE-CIS Fraud Detection dataset from Kaggle
2. Write `features.py` — feature engineering pipeline
3. Write `train.py` — train XGBoost, save model to `model/xgb_model.pkl`
4. Write `api.py` — FastAPI app exposing `POST /score`
5. Containerize with Docker

**Verify:**
```bash
curl -X POST http://localhost:8001/score \
  -H "Content-Type: application/json" \
  -d '{"TransactionAmt": 4200, "card1": 1234, ...}'
# Should return: {score, risk_level, shap_values}
```

---

## Phase 3 — Payment Service Core (Java/Spring Boot)
**The heart of the system. Build in layers.**

1. Convert Gradle project to Spring Boot — update `build.gradle` with dependencies
2. Create JPA entities — `Payment`, `JournalEntry`, `Case`
3. Wire Flyway — migrations run automatically on startup
4. Build `PaymentController` — `POST /api/payments` skeleton
5. Build `IdempotencyService` — Redis check at start of every request
6. Build `FraudScoringClient` — HTTP call to Python `/score`
7. Build `LedgerService` — write double-entry journal entries to PostgreSQL
8. Build `KafkaProducer` — publish `payment.settled` / `payment.flagged`

**Verify:**
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Idempotency-Key: test-001" \
  -H "Content-Type: application/json" \
  -d '{"amount": 250.00, "currency": "USD", "merchantId": "m-123"}'
# Check PostgreSQL for journal entries
# Check Kafka for published event
```

---

## Phase 4 — Case Management API
**Extend the Payment Service — same deployment, same database.**

1. Build `CaseController` — `GET /api/cases`, `GET /api/cases/{id}`, `POST /api/cases/{id}/decision`
2. Build `CaseService` — APPROVE writes to ledger, BLOCK marks transaction rejected
3. Build `KafkaConsumer` — consumes `payment.flagged`, creates case record

**Verify:**
```bash
# Submit a high-risk payment, then:
curl http://localhost:8080/api/cases
# Should show the flagged case

curl -X POST http://localhost:8080/api/cases/{id}/decision \
  -d '{"decision": "APPROVE", "analystId": "analyst-1"}'
# Should write journal entry to ledger
```

---

## Phase 5 — Angular Dashboard
**Build frontend against real API endpoints — not mocks.**

1. `ng new case-dashboard` — scaffold project
2. `CaseListComponent` — table of pending cases, calls `GET /api/cases`
3. `CaseDetailComponent` — transaction detail + SHAP bar chart (Chart.js)
4. `DecisionComponent` — Approve/Block form, calls `POST /api/cases/{id}/decision`

**Verify:**
- Open `http://localhost:4200`
- See live flagged cases
- Approve one, confirm ledger updates in PostgreSQL

---

## Phase 6 — Docker Compose
**Wire all 8 containers together with one command.**

Services: `postgres`, `redis`, `zookeeper`, `kafka`, `fraud-scoring`, `payment-service`, `case-dashboard`

- Add health checks so services wait for dependencies before starting
- Use environment variables for all connection strings

**Verify:**
```bash
docker compose up
# Entire system starts, full end-to-end flow works
```

---

## Phase 7 — Webhooks
**Last — requires an external HTTP endpoint to test.**

- Add `WebhookService` with `@Async` and Spring Retry
- Exponential backoff on failure
- Use `webhook.site` for local testing

**Verify:**
- Settle a payment
- Confirm webhook POST fired to merchant URL
- Simulate merchant server being down — confirm retries with backoff

---

## Phase 8 — Merchant Risk Configuration
**Per-merchant fraud thresholds — each merchant controls their own sensitivity.**

Right now the fraud thresholds are hardcoded globally in `application.properties`. This phase gives each merchant their own configurable thresholds via an API.

### New tables (add to Flyway migrations)

```sql
-- merchant accounts and API keys
CREATE TABLE merchants (
  id          UUID PRIMARY KEY,
  name        TEXT NOT NULL,
  api_key     TEXT UNIQUE NOT NULL,
  callback_url TEXT,
  created_at  TIMESTAMPTZ DEFAULT now()
);

-- per-merchant fraud threshold config
CREATE TABLE merchant_risk_config (
  merchant_id        TEXT PRIMARY KEY REFERENCES merchants(id),
  auto_approve_below NUMERIC(3,2) DEFAULT 0.30,
  review_above       NUMERIC(3,2) DEFAULT 0.50,
  auto_block_above   NUMERIC(3,2) DEFAULT 0.90,
  updated_at         TIMESTAMPTZ DEFAULT now()
);
```

### New classes

- `Merchant` — JPA entity
- `MerchantRiskConfig` — JPA entity
- `MerchantController` — REST endpoints for merchants to manage their config
- `MerchantRiskConfigService` — looks up config per merchant during payment processing

### New endpoints

```
POST /api/merchants                          — register a new merchant
GET  /api/merchants/{merchantId}/risk-config — get current thresholds
PUT  /api/merchants/{merchantId}/risk-config — update thresholds
```

### Update PaymentService

Replace the global threshold lookup with a per-merchant lookup:

```java
MerchantRiskConfig config = merchantRiskConfigService.getConfig(merchantId);

if (score < config.getAutoApproveBelow()) {
    // settle immediately
} else if (score > config.getAutoBlockAbove()) {
    // block immediately, no review
} else if (score > config.getReviewAbove()) {
    // flag for human review
}
```

**Verify:**
```bash
# Register a merchant
curl -X POST http://localhost:8080/api/merchants \
  -d '{"name": "Test Store", "callbackUrl": "https://webhook.site/xxx"}'

# Update their risk thresholds
curl -X PUT http://localhost:8080/api/merchants/{id}/risk-config \
  -H "Authorization: API-Key sk_merchant_123" \
  -d '{"autoApproveBelow": 0.20, "reviewAbove": 0.40, "autoBlockAbove": 0.85}'

# Submit a payment — confirm it uses the merchant's thresholds, not the global ones
curl -X POST http://localhost:8080/api/payments \
  -H "Idempotency-Key: test-002" \
  -H "Authorization: API-Key sk_merchant_123" \
  -d '{"amount": 500.00, "currency": "USD"}'
```

---

## Summary

| Phase | What | Why This Order |
|---|---|---|
| 1 | PostgreSQL schema | Everything writes to the DB |
| 2 | Fraud Scoring Service (Python) | Payment Service calls it |
| 3 | Payment Service (Java) | Core orchestration |
| 4 | Case Management API | Extends Payment Service |
| 5 | Angular Dashboard | Built against real endpoints |
| 6 | Docker Compose | Wire it all together |
| 7 | Webhooks | Needs external endpoint to test |
| 8 | Merchant Risk Config | Per-merchant fraud thresholds |
