package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.Vehicle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

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

    public void delete(int id) throws SQLException {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("UPDATE vehicles SET active=0 WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
