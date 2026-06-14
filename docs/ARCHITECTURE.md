    # Architecture

## Overview

A unified payment processing pipeline with embedded AI fraud detection and a case management dashboard — approximating what Stripe Radar or a bank's internal fraud platform does.

**Goals:** demonstrate payments (transaction lifecycle, idempotency, ledger), AI fraud detection (real-time scoring with explainability), and KYC/compliance-style case management (review workflows, audit trails) in one cohesive system.

---

## Data Flow

```
Client
  │
  ▼
[Payment Service — Java/Spring Boot]  ←── REST POST /api/payments
  │
  ├─1─► Redis          (idempotency key check, TTL 24h)
  │
  ├─2─► [Fraud Scoring Service — Python/FastAPI]
  │         XGBoost.predict_proba()  →  risk score 0–1
  │         SHAP.explain()           →  top N feature contributions
  │
  ├─ if score < 0.3 (low risk):
  │     ├─► PostgreSQL  (write double-entry journal entry)
  │     ├─► Kafka       (publish payment.settled)
  │     └─► Webhook     (notify merchant)
  │
  └─ if score ≥ 0.5 (high risk):
        ├─► PostgreSQL  (write case to cases table)
        └─► Kafka       (publish payment.flagged)

[Case Management API — Java/Spring Boot]
  │  Consumes payment.flagged from Kafka
  │  Exposes REST for Angular dashboard
  │
  ▼
[Angular Dashboard]
  Case list → Transaction detail + SHAP bar chart → Approve / Block

Analyst decision → POST /api/cases/{id}/decision
  │
  └─► PostgreSQL (write to ledger) + Webhook (notify merchant)
```

---

## Happy Path (low-risk payment)

1. Merchant sends `POST /api/payments` with an `Idempotency-Key` header
2. Payment Service checks Redis — if the key was seen before, returns the same response immediately (no duplicate charge)
3. Payment Service calls the Fraud Scoring Service synchronously — XGBoost scores 0–1, SHAP explains why
4. Score below threshold → write a balanced debit/credit pair to PostgreSQL, publish `payment.settled` to Kafka, fire webhook to merchant
5. Merchant receives 200 OK with payment ID and settlement confirmation

## Flagged Path (high-risk payment)

1. Same steps 1–3 above
2. Score above threshold → write a row to `cases` table (SHAP values stored as JSONB), publish `payment.flagged` to Kafka, return 202 Accepted
3. Angular dashboard surfaces the case — analyst sees transaction details and a bar chart showing which features drove the fraud score
4. Analyst clicks Approve or Block → writes the journal entry (or blocks it) and fires the merchant webhook

---

## Components

### Payment Service — `fraud_ai/` (this repo, Java/Spring Boot)

Orchestrates the full payment lifecycle.

| Class | Responsibility |
|---|---|
| `PaymentController` | REST: `POST /api/payments`, `GET /api/payments/{id}` |
| `PaymentService` | Orchestration: idempotency → score → route |
| `IdempotencyService` | Redis key `SHA256(merchantId + idempotencyKey)`, TTL 24h |
| `FraudScoringClient` | HTTP client to Python `/score` endpoint |
| `LedgerService` | Writes double-entry journal entries to PostgreSQL |
| `WebhookService` | Async `POST` to merchant callback URL with retry |
| `KafkaProducer` | Publishes `payment.settled` / `payment.flagged` |
| `Payment`, `JournalEntry`, `Case` | JPA entities |

Spring Boot starters: `web`, `data-jpa`, `data-redis`, `spring-kafka`, `actuator`, `flyway`

### Fraud Scoring Service — `fraud-scoring/` (Python/FastAPI)

Loads a trained XGBoost model at startup and scores transactions in real time.

| File | Responsibility |
|---|---|
| `train.py` | Load IEEE-CIS dataset, engineer features, train XGBoost, serialize to `model/xgb_model.pkl` |
| `api.py` | FastAPI app: `POST /score` → `{score, risk_level, shap_values}` |
| `features.py` | Shared feature engineering — must be identical between training and inference |

SHAP: `TreeExplainer` is fast enough for real-time use; cache the explainer object on startup. Return top 10 features by absolute contribution.

### Merchant Risk Configuration (extension of Payment Service)

| Class | Responsibility |
|---|---|
| `MerchantController` | `POST /api/merchants`, `GET /api/merchants/{id}/risk-config`, `PUT /api/merchants/{id}/risk-config` |
| `MerchantRiskConfigService` | Looks up per-merchant thresholds during payment processing |
| `Merchant`, `MerchantRiskConfig` | JPA entities |

### Case Management API (extension of Payment Service)

| Endpoint | Purpose |
|---|---|
| `GET /api/cases` | Paginated list, filterable by status (`PENDING`/`APPROVED`/`BLOCKED`) |
| `GET /api/cases/{id}` | Full detail including stored SHAP values |
| `POST /api/cases/{id}/decision` | Body: `{decision, analystId, notes}` |

### Angular Dashboard — `case-dashboard/`

| Component | Purpose |
|---|---|
| `CaseListComponent` | Table with fraud score, merchant, amount, timestamp |
| `CaseDetailComponent` | Transaction detail + horizontal SHAP bar chart (Chart.js) |
| `DecisionComponent` | Approve/Block buttons + notes → calls Case Management API |

---

## Database Schema (PostgreSQL + Flyway)

```sql
-- merchant accounts and API keys
CREATE TABLE merchants (
  id           UUID PRIMARY KEY,
  name         TEXT NOT NULL,
  api_key      TEXT UNIQUE NOT NULL,
  callback_url TEXT,
  created_at   TIMESTAMPTZ DEFAULT now()
);

-- per-merchant fraud threshold configuration
CREATE TABLE merchant_risk_config (
  merchant_id        UUID PRIMARY KEY REFERENCES merchants(id),
  auto_approve_below NUMERIC(3,2) DEFAULT 0.30,
  review_above       NUMERIC(3,2) DEFAULT 0.50,
  auto_block_above   NUMERIC(3,2) DEFAULT 0.90,
  updated_at         TIMESTAMPTZ DEFAULT now()
);

-- account ledger nodes (ASSET, LIABILITY, EQUITY, FEE)
CREATE TABLE accounts (
  id       UUID PRIMARY KEY,
  name     TEXT NOT NULL,
  type     TEXT NOT NULL,
  currency CHAR(3) NOT NULL
);

-- double-entry: every payment produces exactly two rows (debit + credit)
CREATE TABLE journal_entries (
  id         UUID PRIMARY KEY,
  payment_id UUID NOT NULL,
  account_id UUID REFERENCES accounts(id),
  direction  TEXT CHECK (direction IN ('DEBIT', 'CREDIT')),
  amount     NUMERIC(18, 2) NOT NULL,
  currency   CHAR(3) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- fraud review queue
CREATE TABLE cases (
  id          UUID PRIMARY KEY,
  payment_id  UUID NOT NULL,
  fraud_score NUMERIC(5, 4),
  shap_values JSONB,
  status      TEXT DEFAULT 'PENDING',
  analyst_id  TEXT,
  decision_at TIMESTAMPTZ,
  created_at  TIMESTAMPTZ DEFAULT now()
);
```

The double-entry constraint: every payment produces exactly one DEBIT row and one CREDIT row. Summing all entries for a payment must equal zero (debit + credit cancel out). This prevents money from appearing or disappearing.

### Merchant Risk Configuration

Each merchant controls their own fraud sensitivity via `merchant_risk_config`. The Payment Service looks up the requesting merchant's config before routing a scored payment:

| Threshold | Default | Meaning |
|---|---|---|
| `auto_approve_below` | 0.30 | Settle immediately, no review |
| `review_above` | 0.50 | Flag for human review |
| `auto_block_above` | 0.90 | Block immediately, no review |

A merchant selling high-value goods tightens thresholds (flags more). A fast food app loosens them (flags less, faster checkout).

## Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| `payment.created` | Payment Service | audit log |
| `payment.settled` | Payment Service | Webhook Service |
| `payment.flagged` | Payment Service | Case Management |
| `case.decision` | Case Management | Webhook Service |

---

## Fraud Model

- **Dataset:** IEEE-CIS Fraud Detection (Kaggle) — 590k transactions, ~3.5% fraud rate
- **Key features:** `TransactionAmt`, `card1–6`, `addr1/2`, `dist1/2`, `V*` (Vesta engineered), `C*` (counting), `D*` (timedelta)
- **Class imbalance:** set `scale_pos_weight = num_negative / num_positive` in XGBoost
- **Threshold:** tune on F1 / precision-recall AUC, not accuracy; store threshold in config so it can be adjusted without redeploying the model

---

## Technology Choices

| Technology | Why |
|---|---|
| Redis for idempotency | Sub-millisecond key lookups, native TTL support |
| Python for fraud scoring | XGBoost and SHAP have far better Python ecosystems; separate service lets you retrain/swap the model without touching payment logic |
| Kafka | Decouples settlement, webhooks, and case management; slow webhooks can't stall payments; full audit log with replay |
| Double-entry ledger | Every dollar in equals every dollar out — how real payment systems prevent money from appearing or disappearing |
| SHAP explainability | Makes ML decisions auditable by analysts and defensible to regulators |

---

## Infrastructure

`docker-compose.yml` services: `postgres`, `redis`, `zookeeper`, `kafka`, `fraud-scoring`, `payment-service`, `case-dashboard` (nginx serving Angular build)

---

## Build Order

1. Database schema + Flyway migrations
2. Fraud Scoring Service (train model offline → FastAPI endpoint → Docker image)
3. Payment Service core (idempotency, fraud client, ledger, Kafka)
4. Case Management endpoints
5. Angular dashboard
6. Docker Compose wiring
7. Webhooks (async with retry)
