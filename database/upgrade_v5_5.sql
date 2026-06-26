-- AutoRent Pro FX V5.5 - Disponibilité avancée
-- Aucun effacement de données. Ce script garantit seulement les colonnes utilisées par la disponibilité.

USE autorent_pro;

CREATE TABLE IF NOT EXISTS reservations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  vehicle_id INT,
  customer_id INT,
  start_date DATE,
  end_date DATE,
  status VARCHAR(30) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE reservations
  ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE';

ALTER TABLE contracts
  ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'ACTIVE';
