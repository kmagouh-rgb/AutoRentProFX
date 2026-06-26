USE autorent_pro;

CREATE TABLE IF NOT EXISTS vehicle_documents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  vehicle_id INT NOT NULL,
  document_type VARCHAR(80) NOT NULL,
  document_number VARCHAR(100),
  issue_date DATE DEFAULT NULL,
  expiry_date DATE DEFAULT NULL,
  file_path VARCHAR(500),
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_vehicle_documents_vehicle(vehicle_id),
  INDEX idx_vehicle_documents_expiry(expiry_date)
);

ALTER TABLE vehicle_documents
ADD COLUMN IF NOT EXISTS document_number VARCHAR(100),
ADD COLUMN IF NOT EXISTS issue_date DATE DEFAULT NULL,
ADD COLUMN IF NOT EXISTS expiry_date DATE DEFAULT NULL,
ADD COLUMN IF NOT EXISTS file_path VARCHAR(500),
ADD COLUMN IF NOT EXISTS notes TEXT;

ALTER TABLE vehicles
ADD COLUMN IF NOT EXISTS insurance_end DATE DEFAULT NULL,
ADD COLUMN IF NOT EXISTS technical_visit_end DATE DEFAULT NULL,
ADD COLUMN IF NOT EXISTS vignette_end DATE DEFAULT NULL;
