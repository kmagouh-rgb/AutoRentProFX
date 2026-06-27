-- AutoRent Pro FX V7.7 AUDIT LOG
CREATE TABLE IF NOT EXISTS audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    username VARCHAR(100) DEFAULT 'admin',
    entity_type VARCHAR(50),
    action VARCHAR(100),
    reference VARCHAR(150),
    details TEXT
);
