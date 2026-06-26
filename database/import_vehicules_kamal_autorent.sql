-- AutoRent Pro FX - Import liste véhicules de Kamal
-- À exécuter dans phpMyAdmin sur la base: autorent_pro
-- ملاحظة: إذا كانت بعض اللوحات مختلفة في الحرف الأخير صححها بعد الاستيراد من شاشة véhicules.

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
  ADD COLUMN IF NOT EXISTS photo_path VARCHAR(500) NULL;

-- Normalisation des anciennes valeurs de statut
UPDATE vehicles SET status='EN_SERVICE' WHERE status IN ('DISPONIBLE','LOUEE','RESERVEE') OR status IS NULL OR status='';

-- Insertion / mise à jour des véhicules
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('27724-45-أ', '27724-45-أ', 'RENAULT', '', 2018, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2018-08-30', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('27861-45-أ', '27861-45-أ', 'CITROEN', '', 2018, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2018-10-30', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('27862-45-أ', '27862-45-أ', 'CITROEN', '', 2018, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2018-10-30', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('27863-45-أ', '27863-45-أ', 'CITROEN', '', 2018, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2018-10-30', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('27864-45-أ', '27864-45-أ', 'CITROEN', '', 2018, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2018-10-30', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('28500-45-أ', '28500-45-أ', 'DACIA', '', 2018, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2018-10-30', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('28477-45-أ', '28477-45-أ', 'FIAT', '', 2019, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2019-05-21', 5)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('28483-45-أ', '28483-45-أ', 'FIAT', '', 2019, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2019-05-21', 5)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('28484-45-أ', '28484-45-أ', 'FIAT', '', 2019, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2019-05-21', 5)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('28528-45-أ', '28528-45-أ', 'FIAT', '', 2019, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2019-05-21', 5)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('30604-45-أ', '30604-45-أ', 'RENAULT', '', 2021, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2021-06-28', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('30605-45-أ', '30605-45-أ', 'RENAULT', '', 2021, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2021-06-28', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('31260-45-أ', '31260-45-أ', 'PEUGEOT', '', 2022, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2022-01-26', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('31533-45-أ', '31533-45-أ', 'CITROEN', '', 2022, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2022-05-09', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('31534-45-أ', '31534-45-أ', 'CITROEN', '', 2022, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2022-05-09', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('32205-45-أ', '32205-45-أ', 'RENAULT', '', 2022, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2022-10-25', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('32204-45-أ', '32204-45-أ', 'RENAULT', '', 2022, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2022-10-25', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('32556-45-أ', '32556-45-أ', 'CITROEN', '', 2023, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2023-03-16', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('32557-45-أ', '32557-45-أ', 'CITROEN', '', 2023, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2023-03-16', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('32558-45-أ', '32558-45-أ', 'CITROEN', '', 2023, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2023-03-16', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('32559-45-أ', '32559-45-أ', 'CITROEN', '', 2023, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2023-03-16', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('32623-45-أ', '32623-45-أ', 'JEEP', '', 2023, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2023-04-07', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('33638-45-أ', '33638-45-أ', 'SEAT', '', 2024, 'Essence', '', 0, 0.00, 'EN_SERVICE', '2024-02-06', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('33637-45-أ', '33637-45-أ', 'SEAT', '', 2024, 'Essence', '', 0, 0.00, 'EN_SERVICE', '2024-02-06', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('373184WW', '373184WW', 'RENAULT', '', 2024, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2024-05-10', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';
INSERT INTO vehicles
(registration, plate_number, brand, model, year, fuel, transmission, mileage, daily_price, status, purchase_date, fiscal_power)
VALUES ('373185WW', '373185WW', 'RENAULT', '', 2024, 'Diesel', '', 0, 0.00, 'EN_SERVICE', '2024-05-10', 6)
ON DUPLICATE KEY UPDATE
registration=VALUES(registration),
plate_number=VALUES(plate_number),
brand=VALUES(brand),
fuel=VALUES(fuel),
purchase_date=VALUES(purchase_date),
fiscal_power=VALUES(fiscal_power),
status='EN_SERVICE';

SELECT COUNT(*) AS total_vehicles FROM vehicles;