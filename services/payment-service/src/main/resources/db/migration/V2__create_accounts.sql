CREATE TABLE accounts (
  id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name     TEXT NOT NULL,
  type     TEXT NOT NULL CHECK (type IN ('ASSET','LIABILITY','EQUITY','FEE')),
  currency CHAR(3) NOT NULL
);

INSERT INTO accounts (name, type, currency) VALUES
  ('Customer Funds',   'LIABILITY', 'USD'),
  ('Merchant Payable', 'LIABILITY', 'USD'),
  ('Fee Income',       'FEE',       'USD');
