package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExpenseRepository {
    public ObservableList<Expense> findAll(String keyword) {
        ObservableList<Expense> list = FXCollections.observableArrayList();
        String filter = keyword == null ? "" : keyword.trim();
        String sql = "SELECT e.*, CONCAT(v.registration,' - ',v.brand,' ',v.model) vehicle_label " +
                "FROM expenses e LEFT JOIN vehicles v ON v.id=e.vehicle_id " +
                "WHERE e.label LIKE ? OR e.category LIKE ? OR e.notes LIKE ? OR v.registration LIKE ? OR v.brand LIKE ? OR v.model LIKE ? " +
                "ORDER BY e.expense_date DESC, e.id DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            String q = "%" + filter + "%";
            for (int i=1;i<=6;i++) ps.setString(i, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Expense(rs.getInt("id"), rs.getInt("vehicle_id"), rs.getString("vehicle_label"), toLocal(rs.getDate("expense_date")), rs.getString("category"), rs.getString("label"), rs.getDouble("amount"), rs.getString("notes")));
                }
            }
        } catch(Exception e) { e.printStackTrace(); }
        return list;
    }

    public Map<Integer,String> vehicles() {
        Map<Integer,String> map = new LinkedHashMap<>();
        map.put(0, "Générale / sans véhicule");
        String sql = "SELECT id, registration, brand, model FROM vehicles WHERE active=1 ORDER BY brand, model";
        try (Connection cn = Db.getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getInt("id"), rs.getString("registration") + " - " + rs.getString("brand") + " " + rs.getString("model"));
        } catch(Exception e) { e.printStackTrace(); }
        return map;
    }

    public void save(Expense e) throws SQLException {
        if (e.getId() == 0) {
            String sql = "INSERT INTO expenses(vehicle_id,expense_date,category,label,amount,notes) VALUES(?,?,?,?,?,?)";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) { fill(ps, e); ps.executeUpdate(); }
        } else {
            String sql = "UPDATE expenses SET vehicle_id=?,expense_date=?,category=?,label=?,amount=?,notes=? WHERE id=?";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) { fill(ps, e); ps.setInt(7, e.getId()); ps.executeUpdate(); }
        }
    }

    private void fill(PreparedStatement ps, Expense e) throws SQLException {
        if (e.getVehicleId() <= 0) ps.setNull(1, Types.INTEGER); else ps.setInt(1, e.getVehicleId());
        ps.setDate(2, Date.valueOf(e.getExpenseDate()));
        ps.setString(3, e.getCategory());
        ps.setString(4, e.getLabel());
        ps.setDouble(5, e.getAmount());
        ps.setString(6, e.getNotes());
    }

    public void delete(int id) throws SQLException {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("DELETE FROM expenses WHERE id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
    }

    private LocalDate toLocal(Date d) { return d == null ? LocalDate.now() : d.toLocalDate(); }
}
