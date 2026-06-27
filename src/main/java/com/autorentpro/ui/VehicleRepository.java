package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.Vehicle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;

public class VehicleRepository {
    public ObservableList<Vehicle> findAll(String keyword) {
        Db.ensureSchemaCompatibility();
        ObservableList<Vehicle> list = FXCollections.observableArrayList();
        String filter = keyword == null ? "" : keyword.trim();
        String sql = "SELECT * FROM vehicles WHERE active=1 AND (registration LIKE ? OR brand LIKE ? OR model LIKE ? OR status LIKE ?) ORDER BY id DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            String q = "%" + filter + "%";
            for (int i = 1; i <= 4; i++) ps.setString(i, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Vehicle(
                            rs.getInt("id"),
                            rs.getString("registration"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getInt("year"),
                            rs.getString("fuel"),
                            rs.getString("transmission"),
                            rs.getInt("mileage"),
                            rs.getDouble("daily_price"),
                            rs.getString("status"),
                            safeString(rs, "photo_path")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void save(Vehicle v) throws SQLException {
        Db.ensureSchemaCompatibility();
        if (v.getId() == 0) {
            String sql = "INSERT INTO vehicles(registration,brand,model,year,fuel,transmission,mileage,daily_price,status,photo_path,active) VALUES(?,?,?,?,?,?,?,?,?,?,1)";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
                fill(ps, v);
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE vehicles SET registration=?,brand=?,model=?,year=?,fuel=?,transmission=?,mileage=?,daily_price=?,status=?,photo_path=? WHERE id=?";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
                fill(ps, v);
                ps.setInt(11, v.getId());
                ps.executeUpdate();
            }
        }
    }

    private void fill(PreparedStatement ps, Vehicle v) throws SQLException {
        ps.setString(1, v.getRegistration());
        ps.setString(2, v.getBrand());
        ps.setString(3, v.getModel());
        ps.setInt(4, v.getYear());
        ps.setString(5, v.getFuel());
        ps.setString(6, v.getTransmission());
        ps.setInt(7, v.getMileage());
        ps.setDouble(8, v.getDailyPrice());
        ps.setString(9, v.getStatus());
        ps.setString(10, v.getPhotoPath());
    }

    private String safeString(ResultSet rs, String column) {
        try {
            String value = rs.getString(column);
            return value == null ? "" : value;
        } catch (Exception ignored) {
            return "";
        }
    }



    public VehicleUsage currentUsage(int vehicleId) {
        Db.ensureSchemaCompatibility();
        try (Connection cn = Db.getConnection()) {
            // 1) Contrat couvrant aujourd'hui = OCCUPEE
            String contractSql = "SELECT c.contract_number ref, c.end_date, c.status, cu.full_name customer " +
                    "FROM contracts c LEFT JOIN customers cu ON cu.id=c.customer_id " +
                    "WHERE c.vehicle_id=? AND UPPER(c.status) IN ('ACTIVE','OPEN','CONFIRME','VEHICULE_LIVRE','EN_COURS','RETOUR_AUJOURD_HUI') " +
                    "AND c.start_date <= CURDATE() AND c.end_date >= CURDATE() ORDER BY c.end_date LIMIT 1";
            try (PreparedStatement ps = cn.prepareStatement(contractSql)) {
                ps.setInt(1, vehicleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new VehicleUsage("OCCUPEE", rs.getString("ref"), rs.getString("customer"), toLocal(rs.getDate("end_date")), rs.getString("status"));
                    }
                }
            }

            // 2) Réservation couvrant aujourd'hui = RESERVEE
            String reservationTodaySql = "SELECT CONCAT('RES-', r.id) ref, r.end_date, r.status, cu.full_name customer " +
                    "FROM reservations r LEFT JOIN customers cu ON cu.id=r.customer_id " +
                    "WHERE r.vehicle_id=? AND UPPER(r.status) IN ('ACTIVE','RESERVE','RESERVED','OPEN','CONFIRMED','CONFIRME') " +
                    "AND r.start_date <= CURDATE() AND r.end_date >= CURDATE() ORDER BY r.end_date LIMIT 1";
            try (PreparedStatement ps = cn.prepareStatement(reservationTodaySql)) {
                ps.setInt(1, vehicleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new VehicleUsage("RESERVEE", rs.getString("ref"), rs.getString("customer"), toLocal(rs.getDate("end_date")), rs.getString("status"));
                    }
                }
            }

            // 3) Prochaine réservation = information future sans bloquer aujourd'hui
            String nextReservationSql = "SELECT CONCAT('RES-', r.id) ref, r.start_date, r.status, cu.full_name customer " +
                    "FROM reservations r LEFT JOIN customers cu ON cu.id=r.customer_id " +
                    "WHERE r.vehicle_id=? AND UPPER(r.status) IN ('ACTIVE','RESERVE','RESERVED','OPEN','CONFIRMED','CONFIRME') " +
                    "AND r.start_date > CURDATE() ORDER BY r.start_date LIMIT 1";
            try (PreparedStatement ps = cn.prepareStatement(nextReservationSql)) {
                ps.setInt(1, vehicleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new VehicleUsage("DISPONIBLE", rs.getString("ref"), rs.getString("customer"), toLocal(rs.getDate("start_date")), "PROCHAINE_RESERVATION");
                    }
                }
            }
        } catch (Exception ignored) {
            // Si une ancienne base n'a pas encore toutes les tables/colonnes, ne pas bloquer l'affichage des cartes.
        }
        return new VehicleUsage("DISPONIBLE", "", "", null, "");
    }

    private LocalDate toLocal(Date d) {
        return d == null ? null : d.toLocalDate();
    }

    public static class VehicleUsage {
        public final String state;
        public final String reference;
        public final String customer;
        public final LocalDate date;
        public final String status;

        public VehicleUsage(String state, String reference, String customer, LocalDate date, String status) {
            this.state = state == null ? "DISPONIBLE" : state;
            this.reference = reference == null ? "" : reference;
            this.customer = customer == null ? "" : customer;
            this.date = date;
            this.status = status == null ? "" : status;
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("UPDATE vehicles SET active=0 WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
