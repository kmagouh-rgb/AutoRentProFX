DROP DATABASE IF EXISTS autorent_pro;
CREATE DATABASE autorent_pro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE autorent_pro;

CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(150) DEFAULT '',
  role VARCHAR(50) DEFAULT 'ADMIN',
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicles (
  id INT AUTO_INCREMENT PRIMARY KEY,
  registration VARCHAR(50) NOT NULL UNIQUE,
  brand VARCHAR(80) NOT NULL,
  model VARCHAR(80) NOT NULL,
  year INT DEFAULT NULL,
  fuel VARCHAR(40) DEFAULT 'Diesel',
  transmission VARCHAR(40) DEFAULT 'Manuelle',
  mileage INT DEFAULT 0,
  daily_price DECIMAL(10,2) DEFAULT 0,
  status VARCHAR(30) DEFAULT 'EN_SERVICE',
  insurance_end DATE DEFAULT NULL,
  technical_visit_end DATE DEFAULT NULL,
  vignette_end DATE DEFAULT NULL,
  notes TEXT,
  photo_path VARCHAR(500),
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customers (
  id INT AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(150) NOT NULL,
  sex VARCHAR(20),
  birth_date DATE DEFAULT NULL,
  birth_place VARCHAR(120),
  nationality VARCHAR(80),
  address VARCHAR(255),
  city VARCHAR(100),
  phone VARCHAR(40),
  email VARCHAR(150),
  cin VARCHAR(40),
  cin_expiry DATE DEFAULT NULL,
  driving_license VARCHAR(80),
  license_issue_date DATE DEFAULT NULL,
  license_issue_place VARCHAR(120),
  license_expiry DATE DEFAULT NULL,
  passport_number VARCHAR(80),
  passport_expiry DATE DEFAULT NULL,
  entry_number VARCHAR(80),
  profession VARCHAR(120),
  emergency_contact_name VARCHAR(150),
  emergency_contact_phone VARCHAR(50),
  observations TEXT,
  doc_cin_recto VARCHAR(500),
  doc_cin_verso VARCHAR(500),
  doc_permis_recto VARCHAR(500),
  doc_permis_verso VARCHAR(500),
  doc_passport VARCHAR(500),
  photo_path VARCHAR(500),
  active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  vehicle_id INT,
  customer_id INT,
  start_date DATE,
  end_date DATE,
  status VARCHAR(30) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE contracts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  contract_number VARCHAR(40) UNIQUE,
  vehicle_id INT,
  customer_id INT,
  start_date DATE,
  end_date DATE,
  daily_price DECIMAL(10,2) DEFAULT 0,
  total_amount DECIMAL(10,2) DEFAULT 0,
  paid_amount DECIMAL(10,2) DEFAULT 0,
  status VARCHAR(30) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  contract_id INT,
  amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  payment_date DATE,
  method VARCHAR(40) DEFAULT 'ESPECES',
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE maintenance (
  id INT AUTO_INCREMENT PRIMARY KEY,
  vehicle_id INT NULL,
  maintenance_date DATE,
  type VARCHAR(80) DEFAULT 'VIDANGE',
  mileage INT DEFAULT 0,
  amount DECIMAL(10,2) DEFAULT 0,
  status VARCHAR(30) DEFAULT 'TERMINEE',
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expenses (
  id INT AUTO_INCREMENT PRIMARY KEY,
  vehicle_id INT NULL,
  expense_date DATE,
  category VARCHAR(80) DEFAULT 'AUTRE',
  label VARCHAR(150),
  amount DECIMAL(10,2) DEFAULT 0,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vehicle_documents (
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
  INDEX idx_vehicle_documents_expiry(expiry_date),
  CONSTRAINT fk_vehicle_documents_vehicle FOREIGN KEY(vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
);

CREATE TABLE audit_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50),
  action VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE settings (
  setting_key VARCHAR(100) PRIMARY KEY,
  setting_value TEXT
);

INSERT INTO users(username,password,full_name,role,active) VALUES
('admin','admin','Administrateur','ADMIN',1);

INSERT INTO vehicles(registration,brand,model,year,fuel,transmission,mileage,daily_price,status,insurance_end,technical_visit_end,vignette_end,notes,photo_path) VALUES
('12345-A-6','Dacia','Logan',2022,'Diesel','Manuelle',45000,250,'EN_SERVICE','2026-12-31','2026-10-30','2026-12-31','Voiture économique',''),
('67890-B-6','Renault','Clio 5',2023,'Diesel','Manuelle',23000,350,'EN_SERVICE','2026-09-15','2026-09-20','2026-12-31','Très demandée',''),
('55555-C-6','Hyundai','Tucson',2021,'Diesel','Automatique',70000,650,'MAINTENANCE','2026-07-10','2026-08-01','2026-12-31','SUV familial','');

INSERT INTO customers(full_name,sex,birth_date,birth_place,nationality,address,city,phone,email,cin,cin_expiry,driving_license,license_issue_date,license_issue_place,license_expiry,profession,emergency_contact_name,emergency_contact_phone,observations) VALUES
('Client Démo','Homme','1985-01-01','Imzouren','Marocaine','Imzouren','Imzouren','0600000000','client.demo@email.com','AA123456','2030-12-31','P-123456','2020-01-01','Al Hoceima','2030-12-31','Commerçant','Contact urgence','0611111111','Client de démonstration');

INSERT INTO vehicle_documents(vehicle_id,document_type,document_number,issue_date,expiry_date,file_path,notes) VALUES
(1,'ASSURANCE','ASS-LOGAN-2026',CURDATE(),DATE_ADD(CURDATE(), INTERVAL 10 MONTH),'','Assurance annuelle'),
(1,'VISITE TECHNIQUE','VT-LOGAN-2026',CURDATE(),DATE_ADD(CURDATE(), INTERVAL 25 DAY),'','À renouveler bientôt'),
(2,'VIGNETTE','VIG-CLIO-2026',CURDATE(),DATE_ADD(CURDATE(), INTERVAL 8 MONTH),'','Vignette 2026');

INSERT INTO maintenance(vehicle_id,maintenance_date,type,mileage,amount,status,notes) VALUES
(1,CURDATE(),'VIDANGE',45500,350,'TERMINEE','Vidange moteur + filtre'),
(3,CURDATE(),'REPARATION',70200,1200,'EN_COURS','Contrôle suspension');

INSERT INTO expenses(vehicle_id,expense_date,category,label,amount,notes) VALUES
(NULL,CURDATE(),'BUREAU','Frais bureau',250,'Papeterie'),
(1,CURDATE(),'ENTRETIEN','Nettoyage véhicule',80,'Lavage complet');
