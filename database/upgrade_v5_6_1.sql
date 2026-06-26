-- AutoRent Pro FX V5.6.1 - Correction réelle: suppression de Disponible/Louée/Réservée de Gestion des véhicules
-- Exécuter sans supprimer les données.

ALTER TABLE vehicles
ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'EN_SERVICE';

UPDATE vehicles
SET status = 'EN_SERVICE'
WHERE status IS NULL OR status = '' OR status IN ('DISPONIBLE','LOUEE','RESERVEE');

UPDATE vehicles
SET status = 'MAINTENANCE'
WHERE status IN ('REPARATION','SERVICE');

-- Statuts techniques autorisés dans Gestion des véhicules:
-- EN_SERVICE, MAINTENANCE, HORS_SERVICE, VENDUE
