CREATE TABLE payments (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  merchant_id     UUID NOT NULL REFERENCES merchants(id),
  amount          NUMERIC(18,2) NOT NULL,
  currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
  status          TEXT NOT NULL DEFAULT 'INITIATED'
                  CHECK (status IN ('INITIATED','FRAUD_SCORED','SETTLED','FLAGGED','APPROVED','BLOCKED')),
  idempotency_key TEXT NOT NULL,
  fraud_score     NUMERIC(5,4),
  created_at      TIMESTAMPTZ DEFAULT now(),
  updated_at      TIMESTAMPTZ DEFAULT now(),
  UNIQUE (merchant_id, idempotency_key)
);
