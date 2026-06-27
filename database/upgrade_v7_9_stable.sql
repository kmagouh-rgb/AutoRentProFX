-- AutoRent Pro FX V7.9 STABLE CLEAN
-- À exécuter dans phpMyAdmin si nécessaire. Ne supprime pas les données.

CREATE TABLE IF NOT EXISTS audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(100) DEFAULT 'admin',
    entity_type VARCHAR(50),
    action VARCHAR(100),
    reference VARCHAR(150),
    details TEXT
);

ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS photo_path VARCHAR(500) NULL,
    ADD COLUMN IF NOT EXISTS active TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS active TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS contract_number VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'RESERVE',
    ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0;

UPDATE vehicles
SET status='EN_SERVICE'
WHERE status IN ('DISPONIBLE','LOUEE','RESERVEE') OR status IS NULL OR status='';
