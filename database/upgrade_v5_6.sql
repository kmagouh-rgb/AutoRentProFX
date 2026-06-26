-- AutoRent Pro FX V5.6 - Remove manual availability from Gestion des véhicules
-- Disponibilité is calculated only from Contrats + Réservations by date range.

ALTER TABLE vehicles
ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'EN_SERVICE';

UPDATE vehicles
SET status = 'EN_SERVICE'
WHERE status IS NULL OR status = '' OR status IN ('DISPONIBLE','LOUEE','RESERVEE');

UPDATE vehicles
SET status = 'MAINTENANCE'
WHERE status IN ('REPARATION','SERVICE');

-- From now on allowed technical statuses are:
-- EN_SERVICE, MAINTENANCE, HORS_SERVICE, VENDUE
