USE autorent_pro;

ALTER TABLE contracts
ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE';

ALTER TABLE payments
ADD COLUMN IF NOT EXISTS notes TEXT;

CREATE INDEX IF NOT EXISTS idx_contract_vehicle ON contracts(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_contract_customer ON contracts(customer_id);
CREATE INDEX IF NOT EXISTS idx_payment_contract ON payments(contract_id);
