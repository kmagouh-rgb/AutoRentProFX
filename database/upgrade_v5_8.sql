-- AutoRent Pro FX V5.8 - Workflow location + véhicules Kamal
-- Exécuter dans phpMyAdmin sur la base autorent_pro si vous avez une ancienne base.

ALTER TABLE vehicles
  ADD COLUMN IF NOT EXISTS registration VARCHAR(50) NULL,
  ADD COLUMN IF NOT EXISTS plate_number VARCHAR(50) NULL,
  ADD COLUMN IF NOT EXISTS brand VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS model VARCHAR(100) NULL,
  ADD COLUMN IF NOT EXISTS year INT NULL,
  ADD COLUMN IF NOT EXISTS fuel VARCHAR(50) NULL,
  ADD COLUMN IF NOT EXISTS transmission VARCHAR(50) NULL,
  ADD COLUMN IF NOT EXISTS mileage INT NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS daily_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'EN_SERVICE',
  ADD COLUMN IF NOT EXISTS purchase_date DATE NULL,
  ADD COLUMN IF NOT EXISTS fiscal_power INT NULL,
  ADD COLUMN IF NOT EXISTS active TINYINT(1) NOT NULL DEFAULT 1;

UPDATE vehicles SET status='EN_SERVICE' WHERE status IN ('DISPONIBLE','LOUEE','RESERVEE') OR status IS NULL OR status='';
UPDATE vehicles SET registration=plate_number WHERE (registration IS NULL OR registration='') AND plate_number IS NOT NULL;
UPDATE vehicles SET plate_number=registration WHERE (plate_number IS NULL OR plate_number='') AND registration IS NOT NULL;

ALTER TABLE contracts
  ADD COLUMN IF NOT EXISTS contract_number VARCHAR(50) NULL,
  ADD COLUMN IF NOT EXISTS start_date DATE NULL,
  ADD COLUMN IF NOT EXISTS end_date DATE NULL,
  ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE';

ALTER TABLE reservations
  ADD COLUMN IF NOT EXISTS start_date DATE NULL,
  ADD COLUMN IF NOT EXISTS end_date DATE NULL,
  ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE';
