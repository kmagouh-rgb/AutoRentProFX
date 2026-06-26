package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.VehicleDocument;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class DocumentsRepository {
    public ObservableList<VehicleDocument> findAll(String search) {
        ObservableList<VehicleDocument> data = FXCollections.observableArrayList();
        String q = search == null ? "" : search.trim();
        String sql = "SELECT d.*, CONCAT(v.registration,' - ',v.brand,' ',v.model) AS vehicle_label " +
                "FROM vehicle_documents d LEFT JOIN vehicles v ON v.id=d.vehicle_id " +
                "WHERE (?='' OR d.document_type LIKE ? OR d.document_number LIKE ? OR v.registration LIKE ? OR v.brand LIKE ? OR v.model LIKE ?) " +
                "ORDER BY COALESCE(d.expiry_date,'2999-12-31') ASC, d.id DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            String like = "%" + q + "%";
            ps.setString(1, q); ps.setString(2, like); ps.setString(3, like); ps.setString(4, like); ps.setString(5, like); ps.setString(6, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) data.add(map(rs));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    public void save(VehicleDocument d) throws SQLException {
        if (d.getId() == 0) insert(d); else update(d);
    }

    private void insert(VehicleDocument d) throws SQLException {
        String sql = "INSERT INTO vehicle_documents(vehicle_id,document_type,document_number,issue_date,expiry_date,file_path,notes) VALUES(?,?,?,?,?,?,?)";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            fill(ps, d); ps.executeUpdate();
        }
    }

    private void update(VehicleDocument d) throws SQLException {
        String sql = "UPDATE vehicle_documents SET vehicle_id=?,document_type=?,document_number=?,issue_date=?,expiry_date=?,file_path=?,notes=? WHERE id=?";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            fill(ps, d); ps.setInt(8, d.getId()); ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("DELETE FROM vehicle_documents WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public Map<Integer,String> vehicles() {
        Map<Integer,String> map = new LinkedHashMap<>();
        try (Connection cn = Db.getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT id, CONCAT(registration,' - ',brand,' ',model) label FROM vehicles WHERE active=1 ORDER BY registration")) {
            while (rs.next()) map.put(rs.getInt("id"), rs.getString("label"));
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    private void fill(PreparedStatement ps, VehicleDocument d) throws SQLException {
        ps.setInt(1, d.getVehicleId());
        ps.setString(2, d.getDocumentType());
        ps.setString(3, d.getDocumentNumber());
        if (d.getIssueDate() == null) ps.setNull(4, Types.DATE); else ps.setDate(4, Date.valueOf(d.getIssueDate()));
        if (d.getExpiryDate() == null) ps.setNull(5, Types.DATE); else ps.setDate(5, Date.valueOf(d.getExpiryDate()));
        ps.setString(6, d.getFilePath());
        ps.setString(7, d.getNotes());
    }

    private VehicleDocument map(ResultSet rs) throws SQLException {
        Date issue = rs.getDate("issue_date");
        Date expiry = rs.getDate("expiry_date");
        return new VehicleDocument(
                rs.getInt("id"), rs.getInt("vehicle_id"), rs.getString("vehicle_label"),
                rs.getString("document_type"), rs.getString("document_number"),
                issue == null ? null : issue.toLocalDate(), expiry == null ? null : expiry.toLocalDate(),
                rs.getString("file_path"), rs.getString("notes")
        );
    }
}
