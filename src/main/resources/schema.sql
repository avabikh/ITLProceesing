CREATE TABLE IF NOT EXISTS POSITIONS_SAI
(
  firm VARCHAR(120)  NOT NULL,
  account_number VARCHAR(120),
  adp_number VARCHAR(120) NOT NULL,
  account_type VARCHAR(120),
  quantity VARCHAR(120),
  holding_type VARCHAR(120),
  quantity_date VARCHAR(120),
  MIPS_client_number VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS POSITIONS_RESULT
(
  firm VARCHAR(120),
  adp_number VARCHAR(120),
  account_number VARCHAR(120),
  holding_type VARCHAR(120),
  balance VARCHAR(120),
  fdic_value VARCHAR(120),
  cash_value VARCHAR(120)
);