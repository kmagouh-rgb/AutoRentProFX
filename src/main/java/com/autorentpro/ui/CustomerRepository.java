package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.Customer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;

public class CustomerRepository {
    public ObservableList<Customer> findAll(String keyword) {
        ObservableList<Customer> list = FXCollections.observableArrayList();
        String filter = keyword == null ? "" : keyword.trim();
        String sql = "SELECT * FROM customers WHERE active=1 AND (" +
                "full_name LIKE ? OR cin LIKE ? OR phone LIKE ? OR driving_license LIKE ? OR " +
                "passport_number LIKE ? OR email LIKE ? OR city LIKE ?) ORDER BY id DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            String q = "%" + filter + "%";
            for (int i = 1; i <= 7; i++) ps.setString(i, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private Customer map(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("sex"),
                rs.getString("birth_date"),
                rs.getString("birth_place"),
                rs.getString("nationality"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("cin"),
                rs.getString("cin_expiry"),
                rs.getString("driving_license"),
                rs.getString("license_issue_date"),
                rs.getString("license_issue_place"),
                rs.getString("license_expiry"),
                rs.getString("passport_number"),
                rs.getString("passport_expiry"),
                rs.getString("entry_number"),
                rs.getString("profession"),
                rs.getString("emergency_contact_name"),
                rs.getString("emergency_contact_phone"),
                rs.getString("observations"),
                rs.getString("doc_cin_recto"),
                rs.getString("doc_cin_verso"),
                rs.getString("doc_permis_recto"),
                rs.getString("doc_permis_verso"),
                rs.getString("doc_passport"),
                rs.getString("photo_path")
        );
    }

    public void save(Customer c) throws SQLException {
        if (c.getId() == 0) {
            String sql = "INSERT INTO customers(" + columns() + ",active) VALUES(" + placeholders() + ",1)";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
                fill(ps, c);
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE customers SET " + updateSet() + " WHERE id=?";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
                fill(ps, c);
                ps.setInt(29, c.getId());
                ps.executeUpdate();
            }
        }
    }

    private String columns() {
        return "full_name,sex,birth_date,birth_place,nationality,address,city,phone,email,cin,cin_expiry," +
                "driving_license,license_issue_date,license_issue_place,license_expiry,passport_number,passport_expiry," +
                "entry_number,profession,emergency_contact_name,emergency_contact_phone,observations," +
                "doc_cin_recto,doc_cin_verso,doc_permis_recto,doc_permis_verso,doc_passport,photo_path";
    }

    private String placeholders() {
        return "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";
    }

    private String updateSet() {
        return "full_name=?,sex=?,birth_date=?,birth_place=?,nationality=?,address=?,city=?,phone=?,email=?,cin=?,cin_expiry=?," +
                "driving_license=?,license_issue_date=?,license_issue_place=?,license_expiry=?,passport_number=?,passport_expiry=?," +
                "entry_number=?,profession=?,emergency_contact_name=?,emergency_contact_phone=?,observations=?," +
                "doc_cin_recto=?,doc_cin_verso=?,doc_permis_recto=?,doc_permis_verso=?,doc_passport=?,photo_path=?";
    }

    private void fill(PreparedStatement ps, Customer c) throws SQLException {
        ps.setString(1, c.getFullName());
        ps.setString(2, c.getSex());
        ps.setString(3, emptyToNull(c.getBirthDate()));
        ps.setString(4, c.getBirthPlace());
        ps.setString(5, c.getNationality());
        ps.setString(6, c.getAddress());
        ps.setString(7, c.getCity());
        ps.setString(8, c.getPhone());
        ps.setString(9, c.getEmail());
        ps.setString(10, c.getCin());
        ps.setString(11, emptyToNull(c.getCinExpiry()));
        ps.setString(12, c.getDrivingLicense());
        ps.setString(13, emptyToNull(c.getLicenseIssueDate()));
        ps.setString(14, c.getLicenseIssuePlace());
        ps.setString(15, emptyToNull(c.getLicenseExpiry()));
        ps.setString(16, c.getPassportNumber());
        ps.setString(17, emptyToNull(c.getPassportExpiry()));
        ps.setString(18, c.getEntryNumber());
        ps.setString(19, c.getProfession());
        ps.setString(20, c.getEmergencyContactName());
        ps.setString(21, c.getEmergencyContactPhone());
        ps.setString(22, c.getObservations());
        ps.setString(23, c.getDocCinRecto());
        ps.setString(24, c.getDocCinVerso());
        ps.setString(25, c.getDocPermisRecto());
        ps.setString(26, c.getDocPermisVerso());
        ps.setString(27, c.getDocPassport());
        ps.setString(28, c.getPhotoPath());
    }

    private String emptyToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    public void delete(int id) throws SQLException {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("UPDATE customers SET active=0 WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
