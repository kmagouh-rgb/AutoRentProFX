-- AutoRent Pro FX V5.4 - Disponibilité par dates
-- Aucun effacement de données.

ALTER TABLE contracts
ADD INDEX IF NOT EXISTS idx_contract_vehicle_dates (vehicle_id, start_date, end_date, status);

-- Les statuts qui bloquent la disponibilité sont: ACTIVE, RESERVE, OPEN, EN_COURS.
