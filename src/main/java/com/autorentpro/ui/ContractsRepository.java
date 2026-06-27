package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.Contract;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public class ContractsRepository {
    public ObservableList<Contract> findAll(String keyword) {
        ObservableList<Contract> list = FXCollections.observableArrayList();
        String filter = keyword == null ? "" : keyword.trim();
        String sql = "SELECT c.*, CONCAT(v.registration,' - ',v.brand,' ',v.model) vehicle_label, cu.full_name customer_label " +
                "FROM contracts c LEFT JOIN vehicles v ON v.id=c.vehicle_id LEFT JOIN customers cu ON cu.id=c.customer_id " +
                "WHERE c.contract_number LIKE ? OR v.registration LIKE ? OR v.brand LIKE ? OR v.model LIKE ? OR cu.full_name LIKE ? OR c.status LIKE ? " +
                "ORDER BY c.id DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            String q = "%" + filter + "%";
            for (int i=1;i<=6;i++) ps.setString(i, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Contract(
                            rs.getInt("id"), rs.getString("contract_number"), rs.getInt("vehicle_id"), rs.getInt("customer_id"),
                            rs.getString("vehicle_label"), rs.getString("customer_label"), toLocal(rs.getDate("start_date")), toLocal(rs.getDate("end_date")),
                            rs.getDouble("daily_price"), rs.getDouble("total_amount"), rs.getDouble("paid_amount"), rs.getString("status")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public Map<Integer,String> vehicles() {
        Map<Integer,String> map = new LinkedHashMap<>();
        String sql = "SELECT id, registration, brand, model, status FROM vehicles WHERE active=1 ORDER BY brand, model";
        try (Connection cn = Db.getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getInt("id"), rs.getString("registration") + " - " + rs.getString("brand") + " " + rs.getString("model") + " [" + rs.getString("status") + "]");
        } catch(Exception e) { e.printStackTrace(); }
        return map;
    }

    public Map<Integer,String> availableVehicles(LocalDate startDate, LocalDate endDate, int excludeContractId) throws SQLException {
        Map<Integer,String> all = vehicles();
        Map<Integer,String> available = new LinkedHashMap<>();
        for (Map.Entry<Integer,String> e : all.entrySet()) {
            int vehicleId = e.getKey();
            // Exclure les véhicules qui ne sont pas exploitables techniquement
            String label = e.getValue() == null ? "" : e.getValue().toUpperCase();
            if (label.contains("MAINTENANCE") || label.contains("HORS_SERVICE") || label.contains("VENDUE")) {
                continue;
            }
            if (findDateConflict(vehicleId, startDate, endDate, excludeContractId) == null) {
                available.put(vehicleId, e.getValue());
            }
        }
        return available;
    }


    public Map<Integer,String> customers() {
        Map<Integer,String> map = new LinkedHashMap<>();
        String sql = "SELECT id, full_name, phone FROM customers WHERE active=1 ORDER BY full_name";
        try (Connection cn = Db.getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getInt("id"), rs.getString("full_name") + " - " + rs.getString("phone"));
        } catch(Exception e) { e.printStackTrace(); }
        return map;
    }

    public void save(Contract c) throws SQLException {
        validateDates(c);
        if (blocksAvailability(c.getStatus())) {
            ContractConflict conflict = findDateConflict(c.getVehicleId(), c.getStartDate(), c.getEndDate(), c.getId());
            if (conflict != null) {
                throw new SQLException("Cette voiture n'est pas disponible entre " + c.getStartDate() + " et " + c.getEndDate()
                        + ". Conflit avec " + conflict.contractNumber + " du " + conflict.startDate + " au " + conflict.endDate
                        + (conflict.customer == null || conflict.customer.isBlank() ? "" : " / Client: " + conflict.customer));
            }
        }
        if (c.getId() == 0) {
            String number = nextNumber();
            String sql = "INSERT INTO contracts(contract_number,vehicle_id,customer_id,start_date,end_date,daily_price,total_amount,paid_amount,status) VALUES(?,?,?,?,?,?,?,?,?)";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, number); fill(ps, c, 2); ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE contracts SET vehicle_id=?,customer_id=?,start_date=?,end_date=?,daily_price=?,total_amount=?,paid_amount=?,status=? WHERE id=?";
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
                fill(ps, c, 1); ps.setInt(9, c.getId()); ps.executeUpdate();
            }
        }
        updateVehicleStatus(c.getVehicleId(), c.getStatus());
        AuditLogger.log("CONTRAT", c.getId() == 0 ? "CREATION" : "MODIFICATION", c.getContractNumber(), "Véhicule=" + c.getVehicleId() + ", Client=" + c.getCustomerId() + ", Statut=" + c.getStatus());
    }

    private void validateDates(Contract c) throws SQLException {
        if (c.getVehicleId() <= 0) throw new SQLException("Sélectionnez une voiture.");
        if (c.getCustomerId() <= 0) throw new SQLException("Sélectionnez un client.");
        if (c.getStartDate() == null || c.getEndDate() == null) throw new SQLException("Saisissez la date de départ et la date de retour.");
        if (c.getEndDate().isBefore(c.getStartDate())) throw new SQLException("La date de retour doit être après la date de départ.");
    }

    private boolean blocksAvailability(String status) {
        if (status == null) return true;
        String s = status.trim().toUpperCase();
        return s.equals("ACTIVE") || s.equals("OPEN") || s.equals("RESERVE") || s.equals("RESERVED") || s.equals("CONFIRME") || s.equals("VEHICULE_LIVRE") || s.equals("EN_COURS") || s.equals("RETOUR_AUJOURD_HUI");
    }

    public ContractConflict findDateConflict(int vehicleId, LocalDate startDate, LocalDate endDate, int excludeContractId) throws SQLException {
        String sql = "SELECT ref_number, start_date, end_date, status, customer, source_type FROM (" +
                " SELECT c.contract_number ref_number, c.start_date, c.end_date, c.status, cu.full_name customer, 'CONTRAT' source_type " +
                " FROM contracts c LEFT JOIN customers cu ON cu.id=c.customer_id " +
                " WHERE c.vehicle_id=? AND c.id<>? " +
                " AND UPPER(c.status) IN ('ACTIVE','RESERVE','RESERVED','OPEN','CONFIRME','VEHICULE_LIVRE','EN_COURS','RETOUR_AUJOURD_HUI') " +
                " AND c.start_date <= ? AND c.end_date >= ? " +
                " UNION ALL " +
                " SELECT CONCAT('RES-', r.id) ref_number, r.start_date, r.end_date, r.status, cu.full_name customer, 'RESERVATION' source_type " +
                " FROM reservations r LEFT JOIN customers cu ON cu.id=r.customer_id " +
                " WHERE r.vehicle_id=? " +
                " AND UPPER(r.status) IN ('ACTIVE','RESERVE','RESERVED','OPEN','CONFIRMED','CONFIRME','VEHICULE_LIVRE','EN_COURS','RETOUR_AUJOURD_HUI') " +
                " AND r.start_date <= ? AND r.end_date >= ? " +
                ") x ORDER BY start_date LIMIT 1";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            ps.setInt(2, excludeContractId);
            ps.setDate(3, Date.valueOf(endDate));
            ps.setDate(4, Date.valueOf(startDate));
            ps.setInt(5, vehicleId);
            ps.setDate(6, Date.valueOf(endDate));
            ps.setDate(7, Date.valueOf(startDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ContractConflict(rs.getString("ref_number"), toLocal(rs.getDate("start_date")),
                            toLocal(rs.getDate("end_date")), rs.getString("customer"), rs.getString("source_type") + " / " + rs.getString("status"));
                }
            }
        }
        return null;
    }

    public boolean isVehicleAvailable(int vehicleId, LocalDate startDate, LocalDate endDate, int excludeContractId) throws SQLException {
        validateDates(new Contract(excludeContractId, null, vehicleId, 1, "", "", startDate, endDate, 0, 0, 0, "ACTIVE"));
        return findDateConflict(vehicleId, startDate, endDate, excludeContractId) == null;
    }

    public static class ContractConflict {
        public final String contractNumber;
        public final LocalDate startDate;
        public final LocalDate endDate;
        public final String customer;
        public final String status;
        public ContractConflict(String contractNumber, LocalDate startDate, LocalDate endDate, String customer, String status) {
            this.contractNumber = contractNumber;
            this.startDate = startDate;
            this.endDate = endDate;
            this.customer = customer;
            this.status = status;
        }
    }

    private void fill(PreparedStatement ps, Contract c, int start) throws SQLException {
        ps.setInt(start, c.getVehicleId());
        ps.setInt(start+1, c.getCustomerId());
        ps.setDate(start+2, Date.valueOf(c.getStartDate()));
        ps.setDate(start+3, Date.valueOf(c.getEndDate()));
        ps.setDouble(start+4, c.getDailyPrice());
        ps.setDouble(start+5, c.getTotalAmount());
        ps.setDouble(start+6, c.getPaidAmount());
        ps.setString(start+7, c.getStatus());
    }

    public void addPayment(int contractId, double amount, String method, String notes) throws SQLException {
        try (Connection cn = Db.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement p1 = cn.prepareStatement("INSERT INTO payments(contract_id,amount,payment_date,method,notes) VALUES(?, ?, CURDATE(), ?, ?)");
                 PreparedStatement p2 = cn.prepareStatement("UPDATE contracts SET paid_amount=paid_amount+? WHERE id=?")) {
                p1.setInt(1, contractId); p1.setDouble(2, amount); p1.setString(3, method); p1.setString(4, notes); p1.executeUpdate();
                p2.setDouble(1, amount); p2.setInt(2, contractId); p2.executeUpdate();
                cn.commit();
                AuditLogger.log("PAIEMENT", "AJOUT", "Contrat #" + contractId, "Montant=" + amount + ", Méthode=" + method);
            } catch(Exception ex) { cn.rollback(); throw ex; }
        }
    }

    public void closeContract(Contract c) throws SQLException {
        // La disponibilité du véhicule ne doit jamais être écrite dans vehicles.status.
        // Elle est calculée uniquement depuis les contrats/réservations et les dates.
        try (Connection cn = Db.getConnection();
             PreparedStatement p1 = cn.prepareStatement("UPDATE contracts SET status='CLOTURE' WHERE id=?")) {
            p1.setInt(1, c.getId());
            p1.executeUpdate();
            AuditLogger.log("CONTRAT", "CLOTURE", c.getContractNumber(), "Contrat clôturé");
        }
    }

    public void cancel(int id) throws SQLException {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("UPDATE contracts SET status='ANNULE' WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
            AuditLogger.log("CONTRAT", "ANNULATION", "Contrat #" + id, "Contrat annulé");
        }
    }

    private void updateVehicleStatus(int vehicleId, String contractStatus) throws SQLException {
        // Ne rien faire: l'occupation est calculée par date depuis contracts/réservations.
    }

    private String nextNumber() {
        int year = LocalDate.now().getYear();
        int n = Db.count("contracts", "YEAR(created_at)=" + year) + 1;
        return String.format("CTR-%d-%04d", year, n);
    }

    public static long days(LocalDate a, LocalDate b) {
        if (a == null || b == null) return 0;
        long d = ChronoUnit.DAYS.between(a, b) + 1;
        return Math.max(d, 1);
    }

    private LocalDate toLocal(Date d) { return d == null ? LocalDate.now() : d.toLocalDate(); }
}
