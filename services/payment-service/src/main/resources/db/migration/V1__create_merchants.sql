CREATE TABLE merchants (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name         TEXT NOT NULL,
  api_key      TEXT UNIQUE NOT NULL,
  callback_url TEXT,
  created_at   TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE merchant_risk_config (
  merchant_id        UUID PRIMARY KEY REFERENCES merchants(id),
  auto_approve_below NUMERIC(3,2) DEFAULT 0.30,
  review_above       NUMERIC(3,2) DEFAULT 0.50,
  auto_block_above   NUMERIC(3,2) DEFAULT 0.90,
  updated_at         TIMESTAMPTZ DEFAULT now()
);
