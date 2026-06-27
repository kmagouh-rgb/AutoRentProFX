package com.autorentpro.ui;

import com.autorentpro.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AuditLogger {
    public static void log(String entityType, String action, String reference, String details) {
        try (Connection cn = Db.getConnection()) {
            try (PreparedStatement create = cn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS audit_log (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "username VARCHAR(100) DEFAULT 'admin'," +
                            "entity_type VARCHAR(50)," +
                            "action VARCHAR(100)," +
                            "reference VARCHAR(150)," +
                            "details TEXT" +
                            ")")) {
                create.executeUpdate();
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO audit_log(username, entity_type, action, reference, details) VALUES('admin',?,?,?,?)")) {
                ps.setString(1, entityType);
                ps.setString(2, action);
                ps.setString(3, reference);
                ps.setString(4, details);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {
        }
    }
}
