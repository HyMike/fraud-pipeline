CREATE TABLE journal_entries (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_id UUID NOT NULL REFERENCES payments(id),
  account_id UUID NOT NULL REFERENCES accounts(id),
  direction  TEXT NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
  amount     NUMERIC(18,2) NOT NULL,
  currency   VARCHAR(3) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);
