-- AutoRent Pro FX V6.8 - Dossier Location
-- No destructive change. Keeps compatibility with existing data.

CREATE TABLE IF NOT EXISTS contract_events (
  id INT AUTO_INCREMENT PRIMARY KEY,
  contract_id INT NOT NULL,
  event_date DATETIME DEFAULT CURRENT_TIMESTAMP,
  event_type VARCHAR(80) NOT NULL,
  description TEXT,
  user_name VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contract_events_contract ON contract_events(contract_id);
