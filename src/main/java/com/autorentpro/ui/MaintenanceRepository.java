package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.Maintenance;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class MaintenanceRepository {
    public ObservableList<Maintenance> findAll(String keyword) {
        ObservableList<Maintenance> list = FXCollections.observableArrayList();
        String filter = keyword == null ? "" : keyword.trim();
        String sql = "SELECT m.*, CONCAT(v.registration,' - ',v.brand,' ',v.model) vehicle_label " +
                "FROM maintenance m LEFT JOIN vehicles v ON v.id=m.vehicle_id " +
                "WHERE m.type LIKE ? OR m.status LIKE ? OR m.notes LIKE ? OR v.registration LIKE ? OR v.brand LIKE ? OR v.model LIKE ? " +
                "ORDER BY m.maintenance_date DESC, m.id DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            String q = "%" + filter + "%";
            for (int i=1;i<=6;i++) ps.setString(i, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Maintenance(rs.getInt("id"), rs.getInt("vehicle_id"), rs.getString("vehicle_label"), toLocal(rs.getDate("maintenance_date")), rs.getString("type"), rs.getInt("mileage"), rs.getDouble("amount"), rs.getString("status"), rs.getString("notes")));
                }
            }
        } catch(Exception e) { e.printStackTrace(); }
        return list;
    }

    public Map<Integer,String> vehicles() {
        Map<Integer,String> map = new LinkedHashMap<>();
        String sql = "SELECT id, registration, brand, model FROM vehicles WHERE active=1 ORDER BY brand, model";
        try (Connection cn = Db.getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getInt("id"), rs.getString("registration") + " - " + rs.getString("brand") + " " + rs.getString("model"));
        } catch(Exception e) { e.printStackTrace(); }
        return map;
    }

    public void save(Maintenance m) throws SQLException {
        if (m.getId() == 0) {
            String sql = "INSERT INTO maintenance(vehicle_id,maintenance_date,type,mileage,amount,status,notes) VALUES(?,?,?,?,?,?,?)";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) { fill(ps, m); ps.executeUpdate(); }
        } else {
            String sql = "UPDATE maintenance SET vehicle_id=?,maintenance_date=?,type=?,mileage=?,amount=?,status=?,notes=? WHERE id=?";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) { fill(ps, m); ps.setInt(8, m.getId()); ps.executeUpdate(); }
        }
        syncVehicle(m);
    }

    private void fill(PreparedStatement ps, Maintenance m) throws SQLException {
        ps.setInt(1, m.getVehicleId());
        ps.setDate(2, Date.valueOf(m.getMaintenanceDate()));
        ps.setString(3, m.getType());
        ps.setInt(4, m.getMileage());
        ps.setDouble(5, m.getAmount());
        ps.setString(6, m.getStatus());
        ps.setString(7, m.getNotes());
    }

    private void syncVehicle(Maintenance m) throws SQLException {
        if ("EN_COURS".equals(m.getStatus())) {
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("UPDATE vehicles SET status='MAINTENANCE' WHERE id=?")) { ps.setInt(1, m.getVehicleId()); ps.executeUpdate(); }
        }
        if (m.getMileage() > 0) {
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("UPDATE vehicles SET mileage=GREATEST(mileage, ?) WHERE id=?")) { ps.setInt(1, m.getMileage()); ps.setInt(2, m.getVehicleId()); ps.executeUpdate(); }
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("DELETE FROM maintenance WHERE id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
    }

    private LocalDate toLocal(Date d) { return d == null ? LocalDate.now() : d.toLocalDate(); }
}
