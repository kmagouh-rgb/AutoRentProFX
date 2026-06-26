-- AutoRent Pro FX V6.7 - Contrats Workflow PRO
-- Ne supprime aucune donnée. Normalise seulement les anciens statuts.

UPDATE contracts SET status='EN_COURS' WHERE UPPER(status) IN ('ACTIVE','OPEN');
UPDATE contracts SET status='RESERVE' WHERE UPPER(status) IN ('RESERVED');
UPDATE contracts SET status='CLOTURE' WHERE UPPER(status) IN ('FERME');

-- Les véhicules restent avec statut technique uniquement.
UPDATE vehicles SET status='EN_SERVICE' WHERE UPPER(status) IN ('DISPONIBLE','LOUEE','RESERVEE');
