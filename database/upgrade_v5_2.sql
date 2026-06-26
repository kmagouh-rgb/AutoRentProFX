-- AutoRent Pro FX V5.2 - continue depuis V5.1
-- A exécuter seulement si votre base est ancienne.

ALTER TABLE users
ADD COLUMN IF NOT EXISTS active TINYINT(1) NOT NULL DEFAULT 1,
ADD COLUMN IF NOT EXISTS full_name VARCHAR(150) DEFAULT '',
ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'ADMIN';

ALTER TABLE vehicles
ADD COLUMN IF NOT EXISTS registration VARCHAR(50) NULL,
ADD COLUMN IF NOT EXISTS plate_number VARCHAR(50) NULL,
ADD COLUMN IF NOT EXISTS active TINYINT(1) NOT NULL DEFAULT 1,
ADD COLUMN IF NOT EXISTS fuel VARCHAR(40) DEFAULT 'Diesel',
ADD COLUMN IF NOT EXISTS transmission VARCHAR(40) DEFAULT 'Manuelle',
ADD COLUMN IF NOT EXISTS mileage INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS daily_price DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'DISPONIBLE';

UPDATE vehicles SET registration=plate_number WHERE (registration IS NULL OR registration='') AND plate_number IS NOT NULL;
UPDATE vehicles SET plate_number=registration WHERE (plate_number IS NULL OR plate_number='') AND registration IS NOT NULL;

ALTER TABLE customers
ADD COLUMN IF NOT EXISTS active TINYINT(1) NOT NULL DEFAULT 1,
ADD COLUMN IF NOT EXISTS driving_license VARCHAR(80) NULL,
ADD COLUMN IF NOT EXISTS address VARCHAR(255) NULL;

ALTER TABLE contracts
ADD COLUMN IF NOT EXISTS contract_number VARCHAR(50) NULL,
ADD COLUMN IF NOT EXISTS number VARCHAR(50) NULL,
ADD COLUMN IF NOT EXISTS daily_price DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS total_amount DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS paid_amount DECIMAL(10,2) DEFAULT 0,
ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE';

UPDATE contracts SET contract_number=number WHERE (contract_number IS NULL OR contract_number='') AND number IS NOT NULL;
UPDATE contracts SET number=contract_number WHERE (number IS NULL OR number='') AND contract_number IS NOT NULL;
UPDATE contracts SET contract_number=CONCAT('CTR-', id) WHERE contract_number IS NULL OR contract_number='';
UPDATE contracts SET number=contract_number WHERE number IS NULL OR number='';
