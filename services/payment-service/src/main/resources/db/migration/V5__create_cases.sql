CREATE TABLE cases (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_id  UUID NOT NULL REFERENCES payments(id),
  fraud_score NUMERIC(5,4) NOT NULL,
  shap_values JSONB NOT NULL,
  status      TEXT NOT NULL DEFAULT 'PENDING'
              CHECK (status IN ('PENDING','APPROVED','BLOCKED')),
  analyst_id  TEXT,
  notes       TEXT,
  decision_at TIMESTAMPTZ,
  created_at  TIMESTAMPTZ DEFAULT now()
);
