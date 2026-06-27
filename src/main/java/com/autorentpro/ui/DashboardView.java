package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardView {
    private final VBox root = new VBox(18);

    public DashboardView() {
        build();
    }

    public Parent getView() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("dashboard-enterprise");

        Label title = new Label("Dashboard BI & Centre d'exploitation");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Vue quotidienne: parc, départs, retours, revenus, alertes et tâches importantes.");
        subtitle.getStyleClass().add("muted");

        GridPane cards = new GridPane();
        cards.setHgap(16);
        cards.setVgap(16);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(25);
        cards.getColumnConstraints().addAll(c, c, c, c);

        int totalVehicles = Db.count("vehicles", "active=1");
        int occupiedToday = countOccupiedToday();
        int reservedSoon = countReservedSoon();
        int maintenance = Db.count("vehicles", "active=1 AND UPPER(status) IN ('MAINTENANCE','HORS_SERVICE','VENDUE')");
        int availableNow = Math.max(0, totalVehicles - occupiedToday - maintenance);

        double caJour = sumTodayPayments();
        double caMois = Db.sum("payments", "amount", "YEAR(payment_date)=YEAR(CURDATE()) AND MONTH(payment_date)=MONTH(CURDATE())");
        double expensesMonth = Db.sum("expenses", "amount", "YEAR(expense_date)=YEAR(CURDATE()) AND MONTH(expense_date)=MONTH(CURDATE())")
                + Db.sum("maintenance", "amount", "YEAR(maintenance_date)=YEAR(CURDATE()) AND MONTH(maintenance_date)=MONTH(CURDATE())");

        cards.add(card("🟢 Disponibles maintenant", String.valueOf(availableNow), "Calculé depuis contrats + état technique"), 0, 0);
        cards.add(card("🔴 Occupées aujourd'hui", String.valueOf(occupiedToday), "Contrats actifs aujourd'hui"), 1, 0);
        cards.add(card("🟡 Réservées bientôt", String.valueOf(reservedSoon), "Départ dans les 30 jours"), 2, 0);
        cards.add(card("🟠 Maintenance / HS", String.valueOf(maintenance), "Non louables techniquement"), 3, 0);

        cards.add(card("📤 Départs aujourd'hui", String.valueOf(countDeparturesToday()), "Contrats qui commencent aujourd'hui"), 0, 1);
        cards.add(card("📥 Retours aujourd'hui", String.valueOf(countReturnsToday()), "Contrats qui se terminent aujourd'hui"), 1, 1);
        cards.add(card("⏰ Contrats en retard", String.valueOf(countLateContracts()), "Retour dépassé non clôturé"), 2, 1);
        cards.add(card("👥 Clients actifs", String.valueOf(Db.count("customers", "active=1")), "Base clients"), 3, 1);

        cards.add(card("💰 CA jour", money(caJour), "Paiements du jour"), 0, 2);
        cards.add(card("📈 CA mois", money(caMois), "Paiements du mois"), 1, 2);
        cards.add(card("💸 Dépenses mois", money(expensesMonth), "Dépenses + maintenance"), 2, 2);
        cards.add(card("📊 Solde mois", money(caMois - expensesMonth), "CA - dépenses"), 3, 2);

        HBox panels = new HBox(16);
        VBox todayTasks = panel("À faire aujourd'hui", todayTasksText());
        VBox alerts = panel("Alertes importantes", alertsText());
        VBox activity = panel("Résumé opérationnel", operationalText(totalVehicles, availableNow, occupiedToday, reservedSoon, maintenance));
        HBox.setHgrow(todayTasks, Priority.ALWAYS);
        HBox.setHgrow(alerts, Priority.ALWAYS);
        HBox.setHgrow(activity, Priority.ALWAYS);
        panels.getChildren().addAll(todayTasks, alerts, activity);

        root.getChildren().setAll(title, subtitle, cards, panels);
    }

    private VBox card(String title, String value, String subtitle) {
        VBox box = new VBox(7);
        box.getStyleClass().add("stat-card");
        box.setPadding(new Insets(18));
        Label t = new Label(title);
        t.getStyleClass().add("stat-title");
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        Label s = new Label(subtitle);
        s.getStyleClass().add("stat-subtitle");
        box.getChildren().addAll(t, v, s);
        return box;
    }

    private VBox panel(String title, String text) {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(18));
        panel.getStyleClass().add("content-card");
        Label ptitle = new Label(title);
        ptitle.getStyleClass().add("section-title");
        Label body = new Label(text == null || text.isBlank() ? "Aucune donnée." : text);
        body.getStyleClass().add("muted");
        body.setWrapText(true);
        panel.getChildren().addAll(ptitle, body);
        return panel;
    }

    private String todayTasksText() {
        StringBuilder sb = new StringBuilder();
        sb.append("• Départs aujourd'hui: ").append(countDeparturesToday()).append("\n");
        sb.append("• Retours aujourd'hui: ").append(countReturnsToday()).append("\n");
        sb.append("• Contrats en retard: ").append(countLateContracts()).append("\n");
        sb.append("• Paiements du jour: ").append(money(sumTodayPayments())).append("\n");
        sb.append(nextContracts("Départs", "start_date=CURDATE()"));
        sb.append(nextContracts("Retours", "end_date=CURDATE()"));
        return sb.toString();
    }

    private String alertsText() {
        StringBuilder sb = new StringBuilder();
        int docsSoon = Db.count("vehicle_documents", "expiry_date IS NOT NULL AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
        int docsExpired = Db.count("vehicle_documents", "expiry_date IS NOT NULL AND expiry_date < CURDATE()");
        int clientsLicenseSoon = Db.count("customers", "license_expiry IS NOT NULL AND license_expiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
        sb.append("• Documents véhicules expirés: ").append(docsExpired).append("\n");
        sb.append("• Documents véhicules à renouveler: ").append(docsSoon).append("\n");
        sb.append("• Permis clients proches expiration: ").append(clientsLicenseSoon).append("\n");
        sb.append("• Contrats non clôturés dépassés: ").append(countLateContracts()).append("\n");
        return sb.toString();
    }

    private String operationalText(int total, int available, int occupied, int reserved, int maintenance) {
        int occupancyRate = total <= 0 ? 0 : (int) Math.round((occupied * 100.0) / total);
        return "• Parc total: " + total + " véhicule(s)\n"
                + "• Disponibles: " + available + "\n"
                + "• Occupées: " + occupied + "\n"
                + "• Réservées prochainement: " + reserved + "\n"
                + "• Maintenance / hors service: " + maintenance + "\n"
                + "• Taux occupation aujourd'hui: " + occupancyRate + "%";
    }

    private int countOccupiedToday() {
        return scalarInt("SELECT COUNT(DISTINCT vehicle_id) FROM contracts " +
                "WHERE UPPER(status) IN ('ACTIVE','OPEN','EN_COURS','VEHICULE_LIVRE','RETOUR_AUJOURD_HUI','CONFIRME','RESERVE','RESERVED') " +
                "AND CURDATE() BETWEEN start_date AND end_date");
    }

    private int countReservedSoon() {
        return scalarInt("SELECT COUNT(DISTINCT vehicle_id) FROM reservations " +
                "WHERE UPPER(status) IN ('ACTIVE','RESERVE','RESERVED','CONFIRME','CONFIRMED') " +
                "AND start_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
    }

    private int countDeparturesToday() {
        return scalarInt("SELECT COUNT(*) FROM contracts WHERE start_date=CURDATE() AND UPPER(status) NOT IN ('ANNULE','CANCELLED','CLOTURE','FERME')");
    }

    private int countReturnsToday() {
        return scalarInt("SELECT COUNT(*) FROM contracts WHERE end_date=CURDATE() AND UPPER(status) NOT IN ('ANNULE','CANCELLED','CLOTURE','FERME')");
    }

    private int countLateContracts() {
        return scalarInt("SELECT COUNT(*) FROM contracts WHERE end_date<CURDATE() AND UPPER(status) NOT IN ('ANNULE','CANCELLED','CLOTURE','FERME')");
    }

    private double sumTodayPayments() {
        return Db.sum("payments", "amount", "payment_date=CURDATE()");
    }

    private String nextContracts(String title, String condition) {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT c.contract_number, COALESCE(cu.full_name,'') customer, COALESCE(v.registration,'') registration " +
                "FROM contracts c " +
                "LEFT JOIN customers cu ON cu.id=c.customer_id " +
                "LEFT JOIN vehicles v ON v.id=c.vehicle_id " +
                "WHERE " + condition + " AND UPPER(c.status) NOT IN ('ANNULE','CANCELLED','CLOTURE','FERME') " +
                "ORDER BY c.id DESC LIMIT 4";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                if (count == 0) sb.append("\n").append(title).append(":\n");
                sb.append("  - ").append(rs.getString("contract_number")).append(" | ").append(rs.getString("registration")).append(" | ").append(rs.getString("customer")).append("\n");
                count++;
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private int scalarInt(String sql) {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String money(double value) {
        return String.format("%.2f DH", value);
    }
}
