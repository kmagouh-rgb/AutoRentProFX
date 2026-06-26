-- AutoRent Pro FX V6.1 - Fleet Cards Photos
-- À exécuter si vous gardez une ancienne base de données.

ALTER TABLE vehicles
ADD COLUMN IF NOT EXISTS photo_path VARCHAR(500) NULL;

UPDATE vehicles SET photo_path='' WHERE photo_path IS NULL;
