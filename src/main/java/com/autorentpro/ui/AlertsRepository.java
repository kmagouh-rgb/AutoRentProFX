package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class AlertsRepository {
    public ObservableList<String[]> findAlerts() {
        ObservableList<String[]> alerts = FXCollections.observableArrayList();

        String docs = "SELECT CONCAT(v.registration,' - ',v.brand,' ',v.model) vehicle, d.document_type, d.expiry_date, " +
                "DATEDIFF(d.expiry_date, CURDATE()) days_left " +
                "FROM vehicle_documents d LEFT JOIN vehicles v ON v.id=d.vehicle_id " +
                "WHERE d.expiry_date IS NOT NULL AND d.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                "ORDER BY d.expiry_date ASC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(docs); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int days = rs.getInt("days_left");
                String level = days < 0 ? "CRITIQUE" : (days <= 7 ? "URGENT" : "INFO");
                String msg = rs.getString("document_type") + " - " + rs.getString("vehicle") + " expire " +
                        (days < 0 ? "depuis " + Math.abs(days) + " jour(s)" : "dans " + days + " jour(s)");
                alerts.add(new String[]{level, "DOCUMENT", msg});
            }
        } catch (Exception e) { e.printStackTrace(); }

        String contracts = "SELECT c.contract_number, c.end_date, CONCAT(v.registration,' - ',v.brand,' ',v.model) vehicle, cu.full_name customer, " +
                "DATEDIFF(c.end_date, CURDATE()) days_left " +
                "FROM contracts c LEFT JOIN vehicles v ON v.id=c.vehicle_id LEFT JOIN customers cu ON cu.id=c.customer_id " +
                "WHERE c.status='ACTIVE' AND c.end_date <= DATE_ADD(CURDATE(), INTERVAL 3 DAY) ORDER BY c.end_date ASC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(contracts); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int days = rs.getInt("days_left");
                String level = days < 0 ? "CRITIQUE" : (days == 0 ? "URGENT" : "INFO");
                String msg = "Contrat " + rs.getString("contract_number") + " / " + rs.getString("customer") + " / " + rs.getString("vehicle") + " " +
                        (days < 0 ? "en retard de " + Math.abs(days) + " jour(s)" : (days == 0 ? "se termine aujourd'hui" : "se termine dans " + days + " jour(s)"));
                alerts.add(new String[]{level, "CONTRAT", msg});
            }
        } catch (Exception e) { e.printStackTrace(); }

        String maintenance = "SELECT registration, brand, model, mileage FROM vehicles WHERE active=1 AND mileage >= 100000 ORDER BY mileage DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(maintenance); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String msg = rs.getString("registration") + " - " + rs.getString("brand") + " " + rs.getString("model") +
                        " a un kilométrage élevé: " + rs.getInt("mileage") + " km";
                alerts.add(new String[]{"INFO", "MAINTENANCE", msg});
            }
        } catch (Exception e) { e.printStackTrace(); }
        return alerts;
    }

    public int countAlerts() { return findAlerts().size(); }
}
