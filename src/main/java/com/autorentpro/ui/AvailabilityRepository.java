package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.AvailabilityRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;

public class AvailabilityRepository {
    public ObservableList<AvailabilityRow> find(LocalDate startDate, LocalDate endDate, String keyword, boolean onlyAvailable) throws SQLException {
        ObservableList<AvailabilityRow> list = FXCollections.observableArrayList();
        String q = keyword == null ? "" : keyword.trim();
        String sql = "SELECT v.id, v.registration, v.brand, v.model, v.daily_price, v.status, " +
                "(SELECT CONCAT('Contrat ', c.contract_number, ' du ', c.start_date, ' au ', c.end_date, IFNULL(CONCAT(' / ', cu.full_name), '')) " +
                " FROM contracts c LEFT JOIN customers cu ON cu.id=c.customer_id " +
                " WHERE c.vehicle_id=v.id " +
                " AND UPPER(c.status) IN ('ACTIVE','RESERVE','RESERVED','OPEN','EN_COURS') " +
                " AND c.start_date <= ? AND c.end_date >= ? ORDER BY c.start_date LIMIT 1) contract_conflict, " +
                "(SELECT CONCAT('Réservation RES-', r.id, ' du ', r.start_date, ' au ', r.end_date, IFNULL(CONCAT(' / ', cu2.full_name), '')) " +
                " FROM reservations r LEFT JOIN customers cu2 ON cu2.id=r.customer_id " +
                " WHERE r.vehicle_id=v.id " +
                " AND UPPER(r.status) IN ('ACTIVE','RESERVE','RESERVED','OPEN','CONFIRMED','EN_COURS') " +
                " AND r.start_date <= ? AND r.end_date >= ? ORDER BY r.start_date LIMIT 1) reservation_conflict " +
                "FROM vehicles v WHERE v.active=1 " +
                "AND (v.registration LIKE ? OR v.brand LIKE ? OR v.model LIKE ? OR v.status LIKE ?) " +
                "ORDER BY v.brand, v.model, v.registration";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(endDate));
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            ps.setDate(4, Date.valueOf(startDate));
            String like = "%" + q + "%";
            ps.setString(5, like);
            ps.setString(6, like);
            ps.setString(7, like);
            ps.setString(8, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String contractConflict = rs.getString("contract_conflict");
                    String reservationConflict = rs.getString("reservation_conflict");
                    String conflict = contractConflict != null && !contractConflict.isBlank() ? contractConflict : reservationConflict;
                    String technicalStatus = rs.getString("status");
                    boolean technicalBlocked = technicalStatus != null && (
                            technicalStatus.equalsIgnoreCase("MAINTENANCE")
                            || technicalStatus.equalsIgnoreCase("HORS_SERVICE")
                            || technicalStatus.equalsIgnoreCase("VENDUE")
                    );
                    boolean available = !technicalBlocked && (conflict == null || conflict.isBlank());
                    if (onlyAvailable && !available) continue;
                    String dateStatus = available ? "DISPONIBLE" : "OCCUPÉE";
                    String reason = available ? "Aucun contrat/réservation sur cette période" : conflict;
                    if (technicalBlocked) {
                        reason = "Indisponible: état technique " + technicalStatus;
                    }
                    list.add(new AvailabilityRow(
                            rs.getInt("id"),
                            rs.getString("registration"),
                            rs.getString("brand") + " " + rs.getString("model"),
                            rs.getDouble("daily_price"),
                            technicalStatus,
                            dateStatus,
                            reason
                    ));
                }
            }
        }
        return list;
    }
}
