-- AutoRent Pro FX - Sprint 1 Fleet Performance
-- Execute in phpMyAdmin on database: autorent_pro
-- الهدف: تسريع Fleet Cards / Planning / Disponibilité

CREATE INDEX IF NOT EXISTS idx_contracts_vehicle_dates_status
ON contracts(vehicle_id, start_date, end_date, status);

CREATE INDEX IF NOT EXISTS idx_contracts_status_dates
ON contracts(status, start_date, end_date);

CREATE INDEX IF NOT EXISTS idx_reservations_vehicle_dates_status
ON reservations(vehicle_id, start_date, end_date, status);

CREATE INDEX IF NOT EXISTS idx_vehicles_active_status
ON vehicles(active, status);

CREATE INDEX IF NOT EXISTS idx_vehicles_registration
ON vehicles(registration);

CREATE INDEX IF NOT EXISTS idx_vehicles_brand_model
ON vehicles(brand, model);

CREATE INDEX IF NOT EXISTS idx_customers_full_name
ON customers(full_name);

CREATE INDEX IF NOT EXISTS idx_customers_phone
ON customers(phone);
