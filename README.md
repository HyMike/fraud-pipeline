# fraud-pipeline

A unified payment processing pipeline with embedded AI fraud detection and a case management dashboard — a small-scale version of what Stripe Radar or a bank's internal fraud platform does.

## What It Does

A payment request comes in, gets checked against an idempotency cache to prevent duplicate charges, then gets scored for fraud risk by an ML model. Low-risk transactions settle automatically to a ledger and trigger a webhook to the merchant. High-risk transactions get routed to a case review queue, where an analyst dashboard shows the transaction details along with an explanation of why it was flagged — and the analyst's decision feeds back into the ledger.

## Architecture

```
Client
  │
  ▼
[Payment Service — Java/Spring Boot]  ←── REST POST /api/payments
  │
  ├─► Redis              (idempotency check, TTL 24h)
  ├─► Fraud Scoring      (XGBoost score 0–1 + SHAP explanation)
  │
  ├─ score < 0.3  →  PostgreSQL (journal entry) + Kafka + Webhook
  ├─ score 0.3–0.9 →  PostgreSQL (case queue) + Kafka
  └─ score ≥ 0.9  →  auto-blocked + Webhook

[Case Management API] ←── analyst reviews flagged payments
[Angular Dashboard]   ←── SHAP bar chart + Approve / Block
```

See [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) for full component breakdown and data flow.

## Tech Stack

| Layer | Technology |
|---|---|
| Payment API | Java 17, Spring Boot 3, REST |
| Idempotency cache | Redis |
| Fraud scoring | Python, FastAPI, XGBoost, SHAP |
| Event streaming | Apache Kafka |
| Ledger / database | PostgreSQL (double-entry bookkeeping) |
| Case dashboard | Angular, Chart.js |
| Containerization | Docker Compose |
| Training data | IEEE-CIS Fraud Detection dataset (Kaggle) |

## Project Structure

```
fraud-pipeline/
├── services/
│   ├── payment-service/        # Java/Spring Boot — payment orchestration
│   └── fraud-scoring/          # Python/FastAPI — XGBoost fraud model
├── dashboard/
│   └── case-dashboard/         # Angular — analyst review UI
├── infra/
│   ├── docker-compose.yml      # All services
│   └── k8s/                    # Kubernetes manifests (stretch goal)
├── docs/
│   ├── ARCHITECTURE.md         # Full architecture reference
│   └── BUILD_ORDER.md          # Step-by-step build guide
├── CLAUDE.md
├── README.md
└── .gitignore
```

## Getting Started

### Prerequisites

- Docker Desktop (running)

That's it — everything else runs inside containers.

### Start the full system

```bash
docker compose -f infra/docker-compose.yml up --build
```

This starts all 7 containers in order: PostgreSQL → Redis → Zookeeper → Kafka → Fraud Scoring → Payment Service → Case Dashboard.

First run takes a few minutes to pull and build images. Subsequent runs are faster.

### Access the application

| Service | URL |
|---|---|
| Case Dashboard (UI) | http://localhost:4200 |
| Payment Service API | http://localhost:8080 |
| Fraud Scoring API docs | http://localhost:8001/docs |

### Stop the system

```bash
# Stop containers, keep database
docker compose -f infra/docker-compose.yml down

# Stop containers and wipe the database
docker compose -f infra/docker-compose.yml down -v
```

---

## Walkthrough

### 1. Register a merchant

Every payment requires a merchant API key. Register one first:

```bash
curl -X POST http://localhost:8080/api/merchants \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Store", "callbackUrl": "https://webhook.site/test"}'
```

Response:
```json
{
  "id": "abc-123-...",
  "name": "Test Store",
  "apiKey": "sk_abc123..."
}
```

Save the `apiKey` — you'll need it for every payment request.

### 2. Submit a low-risk payment (auto-settles)

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Idempotency-Key: order-001" \
  -d '{"amount": 25.00, "currency": "USD"}'
```

Expected: `"status": "SETTLED"` — written to the ledger immediately, webhook fired to the merchant.

### 3. Submit a high-risk payment (flagged for review)

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "X-API-Key: YOUR_API_KEY" \
  -H "Idempotency-Key: order-002" \
  -d '{"amount": 99999.99, "currency": "USD"}'
```

Expected: `"status": "FLAGGED"` — creates a case visible in the dashboard at http://localhost:4200.

### 4. Review the case in the dashboard

Open http://localhost:4200 to see the flagged case. Click into it to see:
- Transaction amount and fraud score
- SHAP bar chart explaining which features drove the score
- Approve / Block buttons

### 5. Check cases via API

```bash
curl http://localhost:8080/api/cases
```

### 6. Make a decision via API

```bash
curl -X POST http://localhost:8080/api/cases/{caseId}/decision \
  -H "Content-Type: application/json" \
  -d '{"decision": "APPROVE", "analystId": "analyst-1", "notes": "Verified with customer"}'
```

An `APPROVE` writes the journal entry to the ledger and fires a webhook to the merchant. A `BLOCK` marks the payment rejected.

---

## Per-Merchant Risk Configuration

Each merchant can configure their own fraud thresholds:

| Threshold | Default | Meaning |
|---|---|---|
| `autoApproveBelow` | 0.30 | Score under this → auto-settle |
| `reviewAbove` | 0.50 | Score over this → flag for review |
| `autoBlockAbove` | 0.90 | Score over this → auto-block |

```bash
# View current thresholds
curl http://localhost:8080/api/merchants/{merchantId}/risk-config \
  -H "X-API-Key: YOUR_API_KEY"

# Update thresholds
curl -X PUT http://localhost:8080/api/merchants/{merchantId}/risk-config \
  -H "Content-Type: application/json" \
  -H "X-API-Key: YOUR_API_KEY" \
  -d '{"autoApproveBelow": 0.20, "reviewAbove": 0.40, "autoBlockAbove": 0.85}'
```

---

## Build Order

The project was built in 8 phases. See [docs/BUILD_ORDER.md](./docs/BUILD_ORDER.md) for the full guide.

| Phase | What | Status |
|---|---|---|
| 1 | PostgreSQL schema + Flyway migrations | ✓ Done |
| 2 | Fraud Scoring Service (Python/XGBoost) | ✓ Done |
| 3 | Payment Service core (Spring Boot) | ✓ Done |
| 4 | Case Management API | ✓ Done |
| 5 | Angular Dashboard | ✓ Done |
| 6 | Docker Compose (all services) | ✓ Done |
| 7 | Webhooks | ✓ Done |
| 8 | Per-merchant risk configuration | ✓ Done |
