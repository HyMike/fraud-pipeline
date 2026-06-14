# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**fraud-pipeline** — a monorepo containing a Java/Spring Boot payment service, a Python fraud scoring service, and an Angular case management dashboard.

## Repository Structure

```
fraud-pipeline/
├── services/
│   ├── payment-service/     # Java 17, Spring Boot 3, Gradle
│   └── fraud-scoring/       # Python 3.11, FastAPI, XGBoost, SHAP
├── dashboard/
│   └── case-dashboard/      # Angular
├── infra/
│   ├── docker-compose.yml   # All containers
│   └── k8s/                 # Kubernetes (stretch goal)
└── docs/
    ├── ARCHITECTURE.md
    └── BUILD_ORDER.md
```

## Payment Service (services/payment-service/)

Java package: `com.fraudpipeline.payment`

### Build & Run

```bash
cd services/payment-service
./gradlew build          # compile + test
./gradlew bootRun        # run locally (requires PostgreSQL running)
./gradlew test           # run all tests
./gradlew test --tests "com.fraudpipeline.payment.SomeTest"
./gradlew test --tests "com.fraudpipeline.payment.SomeTest.methodName"
```

### Dependencies (add to build.gradle)

Spring Boot starters: `web`, `data-jpa`, `data-redis`, `actuator`
Also: `spring-kafka`, `flyway-core`, `postgresql` driver, `spring-retry`

### Flyway Migrations

SQL files in `services/payment-service/src/main/resources/db/migration/`.
Naming: `V{n}__{description}.sql`. Run automatically on `bootRun`.

Current migrations:
- `V1` — merchants, merchant_risk_config
- `V2` — accounts (with 3 seed rows)
- `V3` — payments
- `V4` — journal_entries
- `V5` — cases

## Fraud Scoring Service (services/fraud-scoring/)

Python 3.11, FastAPI. Exposes `POST /score` → `{score, risk_level, shap_values}`.

Key files (to be created in Phase 2):
- `features.py` — feature engineering (identical between training and inference)
- `train.py` — trains XGBoost on IEEE-CIS dataset, saves to `model/xgb_model.pkl`
- `api.py` — FastAPI app, loads model on startup

## Dashboard (dashboard/case-dashboard/)

Angular SPA. Built in Phase 5 against real Case Management API endpoints.

## Infrastructure

```bash
# Start all services
docker compose -f infra/docker-compose.yml up

# Start only PostgreSQL
docker compose -f infra/docker-compose.yml up -d postgres
```

## Tech Stack

### Payment Service (Java/Spring Boot)
- **Spring Web** — REST controllers (`@RestController`, `@PostMapping`)
- **Spring Data JPA** — PostgreSQL ORM
- **Spring Data Redis** — idempotency cache (`RedisTemplate`, 24h TTL)
- **Spring Kafka** — event publishing (`KafkaTemplate`) and consuming (`@KafkaListener`)
- **Spring Async** — non-blocking webhook delivery (`@Async`)
- **Spring Actuator** — health check endpoints
- **Flyway** — versioned schema migrations
- **JUnit 5** — testing

### Fraud Scoring Service (Python)
- **FastAPI** — REST API
- **XGBoost** — fraud classification (`predict_proba()`)
- **SHAP** — model explainability (`TreeExplainer`, top 10 features)
- **scikit-learn** — preprocessing
- **pandas** — feature engineering

### Infrastructure
- **PostgreSQL 16** — ledger + case queue (double-entry schema)
- **Redis** — idempotency key cache
- **Kafka + Zookeeper** — event streaming
- **Docker Compose** — local orchestration

### Dashboard (Angular)
- **Angular** — SPA framework
- **Chart.js** — SHAP value bar chart

### Training Data
- **IEEE-CIS Fraud Detection dataset** (Kaggle) — 590k transactions, ~3.5% fraud rate
