package com.autorentpro.db;

import java.sql.*;

public final class Db {
    private static final String URL = "jdbc:mysql://localhost:3306/autorent_pro?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=utf8";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private Db() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }


    public static void ensureSchemaCompatibility() {
        try (Connection cn = getConnection()) {
            // Users
            addColumnIfMissing(cn, "users", "active", "TINYINT(1) NOT NULL DEFAULT 1");
            addColumnIfMissing(cn, "users", "full_name", "VARCHAR(150) DEFAULT ''");
            addColumnIfMissing(cn, "users", "role", "VARCHAR(50) DEFAULT 'ADMIN'");

            // Vehicles: compatibility between old plate_number and new registration
            addColumnIfMissing(cn, "vehicles", "registration", "VARCHAR(50) NULL");
            addColumnIfMissing(cn, "vehicles", "plate_number", "VARCHAR(50) NULL");
            addColumnIfMissing(cn, "vehicles", "active", "TINYINT(1) NOT NULL DEFAULT 1");
            addColumnIfMissing(cn, "vehicles", "fuel", "VARCHAR(40) DEFAULT 'Diesel'");
            addColumnIfMissing(cn, "vehicles", "transmission", "VARCHAR(40) DEFAULT 'Manuelle'");
            addColumnIfMissing(cn, "vehicles", "mileage", "INT DEFAULT 0");
            addColumnIfMissing(cn, "vehicles", "daily_price", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(cn, "vehicles", "insurance_end", "DATE NULL");
            addColumnIfMissing(cn, "vehicles", "technical_visit_end", "DATE NULL");
            addColumnIfMissing(cn, "vehicles", "vignette_end", "DATE NULL");
            addColumnIfMissing(cn, "vehicles", "notes", "TEXT NULL");
            addColumnIfMissing(cn, "vehicles", "photo_path", "VARCHAR(500) NULL");
            safeUpdate(cn, "UPDATE vehicles SET registration=plate_number WHERE (registration IS NULL OR registration='') AND plate_number IS NOT NULL");
            safeUpdate(cn, "UPDATE vehicles SET plate_number=registration WHERE (plate_number IS NULL OR plate_number='') AND registration IS NOT NULL");

            // Customers - fiche complète V6.0
            addColumnIfMissing(cn, "customers", "active", "TINYINT(1) NOT NULL DEFAULT 1");
            addColumnIfMissing(cn, "customers", "sex", "VARCHAR(20) NULL");
            addColumnIfMissing(cn, "customers", "birth_date", "DATE NULL");
            addColumnIfMissing(cn, "customers", "birth_place", "VARCHAR(120) NULL");
            addColumnIfMissing(cn, "customers", "nationality", "VARCHAR(80) NULL");
            addColumnIfMissing(cn, "customers", "address", "VARCHAR(255) NULL");
            addColumnIfMissing(cn, "customers", "city", "VARCHAR(100) NULL");
            addColumnIfMissing(cn, "customers", "email", "VARCHAR(150) NULL");
            addColumnIfMissing(cn, "customers", "cin_expiry", "DATE NULL");
            addColumnIfMissing(cn, "customers", "driving_license", "VARCHAR(80) NULL");
            addColumnIfMissing(cn, "customers", "license_issue_date", "DATE NULL");
            addColumnIfMissing(cn, "customers", "license_issue_place", "VARCHAR(120) NULL");
            addColumnIfMissing(cn, "customers", "license_expiry", "DATE NULL");
            addColumnIfMissing(cn, "customers", "passport_number", "VARCHAR(80) NULL");
            addColumnIfMissing(cn, "customers", "passport_expiry", "DATE NULL");
            addColumnIfMissing(cn, "customers", "entry_number", "VARCHAR(80) NULL");
            addColumnIfMissing(cn, "customers", "profession", "VARCHAR(120) NULL");
            addColumnIfMissing(cn, "customers", "emergency_contact_name", "VARCHAR(150) NULL");
            addColumnIfMissing(cn, "customers", "emergency_contact_phone", "VARCHAR(50) NULL");
            addColumnIfMissing(cn, "customers", "observations", "TEXT NULL");
            addColumnIfMissing(cn, "customers", "doc_cin_recto", "VARCHAR(500) NULL");
            addColumnIfMissing(cn, "customers", "doc_cin_verso", "VARCHAR(500) NULL");
            addColumnIfMissing(cn, "customers", "doc_permis_recto", "VARCHAR(500) NULL");
            addColumnIfMissing(cn, "customers", "doc_permis_verso", "VARCHAR(500) NULL");
            addColumnIfMissing(cn, "customers", "doc_passport", "VARCHAR(500) NULL");
            addColumnIfMissing(cn, "customers", "photo_path", "VARCHAR(500) NULL");

            // Contracts: compatibility between number and contract_number
            addColumnIfMissing(cn, "contracts", "contract_number", "VARCHAR(50) NULL");
            addColumnIfMissing(cn, "contracts", "number", "VARCHAR(50) NULL");
            addColumnIfMissing(cn, "contracts", "daily_price", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(cn, "contracts", "total_amount", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(cn, "contracts", "paid_amount", "DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(cn, "contracts", "status", "VARCHAR(30) DEFAULT 'ACTIVE'");
            safeUpdate(cn, "UPDATE contracts SET contract_number=number WHERE (contract_number IS NULL OR contract_number='') AND number IS NOT NULL");
            safeUpdate(cn, "UPDATE contracts SET number=contract_number WHERE (number IS NULL OR number='') AND contract_number IS NOT NULL");
            safeUpdate(cn, "UPDATE contracts SET contract_number=CONCAT('CTR-', id) WHERE contract_number IS NULL OR contract_number='' ");
            safeUpdate(cn, "UPDATE contracts SET number=contract_number WHERE number IS NULL OR number='' ");

            // Other modules
            addColumnIfMissing(cn, "reservations", "status", "VARCHAR(30) DEFAULT 'ACTIVE'");
            ensureVehicleDocumentsTable(cn);
            ensureMaintenanceTable(cn);
            ensureExpensesTable(cn);
            ensurePaymentsTable(cn);
        } catch (Exception ignored) {
            // Login screen will show any real database connection error later.
        }
    }

    private static boolean columnExists(Connection cn, String table, String column) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static void addColumnIfMissing(Connection cn, String table, String column, String definition) {
        try {
            if (!columnExists(cn, table, column)) {
                try (Statement st = cn.createStatement()) {
                    st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void safeUpdate(Connection cn, String sql) {
        try (Statement st = cn.createStatement()) { st.executeUpdate(sql); } catch (Exception ignored) {}
    }

    private static void ensureVehicleDocumentsTable(Connection cn) {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS vehicle_documents (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "vehicle_id INT NOT NULL," +
                    "document_type VARCHAR(80) NOT NULL," +
                    "document_number VARCHAR(100)," +
                    "issue_date DATE NULL," +
                    "expiry_date DATE NULL," +
                    "file_path VARCHAR(500)," +
                    "notes TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");
        } catch (Exception ignored) {}
    }

    private static void ensureMaintenanceTable(Connection cn) {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS maintenance (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, vehicle_id INT NULL, maintenance_date DATE, type VARCHAR(80) DEFAULT 'VIDANGE'," +
                    "mileage INT DEFAULT 0, amount DECIMAL(10,2) DEFAULT 0, status VARCHAR(30) DEFAULT 'TERMINEE', notes TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}
    }

    private static void ensureExpensesTable(Connection cn) {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, vehicle_id INT NULL, expense_date DATE, category VARCHAR(80) DEFAULT 'AUTRE'," +
                    "label VARCHAR(150), amount DECIMAL(10,2) DEFAULT 0, notes TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}
    }

    private static void ensurePaymentsTable(Connection cn) {
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS payments (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, contract_id INT, amount DECIMAL(10,2) NOT NULL DEFAULT 0," +
                    "payment_date DATE, method VARCHAR(40) DEFAULT 'ESPECES', notes TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ignored) {}
    }

    public static int count(String table, String where) {
        String sql = "SELECT COUNT(*) FROM " + table + (where == null || where.isBlank() ? "" : " WHERE " + where);
        try (Connection cn = getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public static double sum(String table, String column, String where) {
        String sql = "SELECT COALESCE(SUM(" + column + "),0) FROM " + table + (where == null || where.isBlank() ? "" : " WHERE " + where);
        try (Connection cn = getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
