package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.SearchResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class GlobalSearchRepository {
    public ObservableList<SearchResult> search(String keyword) {
        ObservableList<SearchResult> results = FXCollections.observableArrayList();
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) return results;
        String like = "%" + q + "%";
        searchVehicles(results, like);
        searchCustomers(results, like);
        searchContracts(results, like);
        return results;
    }

    private void searchVehicles(ObservableList<SearchResult> results, String like) {
        String sql = "SELECT registration, brand, model, status, daily_price FROM vehicles " +
                "WHERE active=1 AND (registration LIKE ? OR plate_number LIKE ? OR brand LIKE ? OR model LIKE ? OR status LIKE ?) " +
                "ORDER BY id DESC LIMIT 20";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("registration") + " - " + rs.getString("brand") + " " + rs.getString("model");
                    String subtitle = "Prix/jour: " + rs.getDouble("daily_price") + " DH";
                    results.add(new SearchResult("Véhicule", title, subtitle, rs.getString("status")));
                }
            }
        } catch (Exception ignored) {}
    }

    private void searchCustomers(ObservableList<SearchResult> results, String like) {
        String sql = "SELECT full_name, cin, phone, driving_license FROM customers " +
                "WHERE active=1 AND (full_name LIKE ? OR cin LIKE ? OR phone LIKE ? OR driving_license LIKE ?) " +
                "ORDER BY id DESC LIMIT 20";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String subtitle = "CIN: " + safe(rs.getString("cin")) + " | Tel: " + safe(rs.getString("phone")) + " | Permis: " + safe(rs.getString("driving_license"));
                    results.add(new SearchResult("Client", rs.getString("full_name"), subtitle, "ACTIF"));
                }
            }
        } catch (Exception ignored) {}
    }

    private void searchContracts(ObservableList<SearchResult> results, String like) {
        String sql = "SELECT c.contract_number, c.status, c.total_amount, c.paid_amount, " +
                "CONCAT(v.registration,' - ',v.brand,' ',v.model) vehicle_label, cu.full_name customer_label " +
                "FROM contracts c LEFT JOIN vehicles v ON v.id=c.vehicle_id LEFT JOIN customers cu ON cu.id=c.customer_id " +
                "WHERE c.contract_number LIKE ? OR c.number LIKE ? OR v.registration LIKE ? OR cu.full_name LIKE ? OR c.status LIKE ? " +
                "ORDER BY c.id DESC LIMIT 20";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String title = safe(rs.getString("contract_number")) + " - " + safe(rs.getString("customer_label"));
                    String subtitle = safe(rs.getString("vehicle_label")) + " | Total: " + rs.getDouble("total_amount") + " DH | Payé: " + rs.getDouble("paid_amount") + " DH";
                    results.add(new SearchResult("Contrat", title, subtitle, rs.getString("status")));
                }
            }
        } catch (Exception ignored) {}
    }

    private String safe(String v) { return v == null ? "" : v; }
}
