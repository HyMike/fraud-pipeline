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
  └─ score ≥ 0.5  →  PostgreSQL (case queue) + Kafka

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
- Docker Desktop
- Java 17+
- Python 3.11+ (for fraud scoring service)
- Node 18+ / Angular CLI (for dashboard)

### Run the database

```bash
docker compose -f infra/docker-compose.yml up -d postgres
```

### Run the Payment Service

```bash
cd services/payment-service
./gradlew bootRun
```

Flyway migrations run automatically on startup — all tables are created in PostgreSQL.

### Verify

```bash
# Submit a test payment
curl -X POST http://localhost:8080/api/payments \
  -H "Idempotency-Key: test-001" \
  -H "Content-Type: application/json" \
  -d '{"amount": 250.00, "currency": "USD", "merchantId": "m-123"}'

# View flagged cases
curl http://localhost:8080/api/cases
```

## Build Order

The project is built in 8 phases. See [docs/BUILD_ORDER.md](./docs/BUILD_ORDER.md) for the full guide.

| Phase | What | Status |
|---|---|---|
| 1 | PostgreSQL schema + Flyway migrations | ✓ Done |
| 2 | Fraud Scoring Service (Python/XGBoost) | |
| 3 | Payment Service core (Spring Boot) | |
| 4 | Case Management API | |
| 5 | Angular Dashboard | |
| 6 | Docker Compose (all services) | |
| 7 | Webhooks | |
| 8 | Per-merchant risk configuration | |
