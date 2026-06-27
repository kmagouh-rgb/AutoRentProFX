package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AlertsView {
    private final BorderPane root = new BorderPane();
    private final VBox list = new VBox(10);
    private final Label countLabel = new Label();
    private final ComboBox<String> filter = new ComboBox<>();

    public AlertsView() {
        build();
        load();
    }

    public Parent getView() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(22));

        Label title = new Label("Centre de notifications");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Alertes opérationnelles: contrats, retours, paiements, documents et maintenance.");
        subtitle.getStyleClass().add("muted");

        filter.getItems().addAll("Toutes", "Critiques", "Contrats", "Documents", "Maintenance", "Paiements");
        filter.setValue("Toutes");
        filter.setOnAction(e -> load());

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> load());

        HBox actions = new HBox(10, filter, refresh, countLabel);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(10, title, subtitle, actions);
        root.setTop(top);

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        root.setCenter(scroll);
    }

    private void load() {
        ObservableList<AlertRow> rows = FXCollections.observableArrayList();
        try (Connection cn = Db.getConnection()) {
            addContractAlerts(cn, rows);
            addDocumentAlerts(cn, rows);
            addPaymentAlerts(cn, rows);
            addMaintenanceAlerts(cn, rows);
        } catch (Exception e) {
            rows.add(new AlertRow("Erreur", "Impossible de lire les alertes: " + e.getMessage(), "CRITIQUE", "SYSTEM"));
        }

        String f = filter.getValue();
        list.getChildren().clear();
        int shown = 0;
        for (AlertRow row : rows) {
            if (!matches(row, f)) continue;
            list.getChildren().add(card(row));
            shown++;
        }
        countLabel.setText(shown + " alerte(s)");
        countLabel.getStyleClass().add("pill");

        if (shown == 0) {
            Label empty = new Label("Aucune alerte pour ce filtre.");
            empty.getStyleClass().add("muted");
            list.getChildren().add(empty);
        }
    }

    private boolean matches(AlertRow row, String filter) {
        if (filter == null || filter.equals("Toutes")) return true;
        if (filter.equals("Critiques")) return row.level.equals("CRITIQUE");
        return row.category.equalsIgnoreCase(filter);
    }

    private VBox card(AlertRow row) {
        VBox box = new VBox(6);
        box.getStyleClass().add("notification-card");
        box.setPadding(new Insets(14));

        Label level = new Label(row.level + " • " + row.category);
        level.getStyleClass().add(switch (row.level) {
            case "CRITIQUE" -> "status-louee";
            case "IMPORTANT" -> "status-maintenance";
            default -> "status-disponible";
        });

        Label title = new Label(row.title);
        title.setStyle("-fx-font-weight: 900; -fx-font-size: 15px;");

        Label message = new Label(row.message);
        message.setWrapText(true);
        message.getStyleClass().add("muted");

        box.getChildren().addAll(level, title, message);
        return box;
    }

    private void addContractAlerts(Connection cn, ObservableList<AlertRow> rows) throws Exception {
        String sql = "SELECT contract_number, end_date, status FROM contracts " +
                "WHERE UPPER(status) IN ('ACTIVE','OPEN','EN_COURS','VEHICULE_LIVRE','RETOUR_AUJOURD_HUI') " +
                "AND end_date <= DATE_ADD(CURDATE(), INTERVAL 1 DAY) ORDER BY end_date";
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String end = String.valueOf(rs.getDate("end_date"));
                String level = rs.getDate("end_date").before(new java.sql.Date(System.currentTimeMillis())) ? "CRITIQUE" : "IMPORTANT";
                rows.add(new AlertRow("Contrat à suivre", rs.getString("contract_number") + " retour prévu: " + end + " / statut: " + rs.getString("status"), level, "Contrats"));
            }
        }
    }

    private void addDocumentAlerts(Connection cn, ObservableList<AlertRow> rows) {
        String sql = "SELECT v.registration, d.type, d.expiry_date FROM vehicle_documents d JOIN vehicles v ON v.id=d.vehicle_id " +
                "WHERE d.expiry_date IS NOT NULL AND d.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) ORDER BY d.expiry_date";
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new AlertRow("Document véhicule proche expiration",
                        rs.getString("registration") + " / " + rs.getString("type") + " expire le " + rs.getDate("expiry_date"),
                        "IMPORTANT", "Documents"));
            }
        } catch (Exception ignored) {}
    }

    private void addPaymentAlerts(Connection cn, ObservableList<AlertRow> rows) throws Exception {
        String sql = "SELECT contract_number, total_amount, paid_amount FROM contracts " +
                "WHERE UPPER(status) NOT IN ('ANNULE','CANCELLED','CLOTURE','FERME') AND COALESCE(total_amount,0) > COALESCE(paid_amount,0) ORDER BY id DESC LIMIT 20";
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double rest = rs.getDouble("total_amount") - rs.getDouble("paid_amount");
                rows.add(new AlertRow("Paiement incomplet",
                        rs.getString("contract_number") + " reste à payer: " + String.format("%.2f DH", rest),
                        "INFO", "Paiements"));
            }
        }
    }

    private void addMaintenanceAlerts(Connection cn, ObservableList<AlertRow> rows) throws Exception {
        String sql = "SELECT registration, brand, model, status FROM vehicles WHERE UPPER(status) IN ('MAINTENANCE','HORS_SERVICE')";
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new AlertRow("Véhicule indisponible techniquement",
                        rs.getString("registration") + " - " + rs.getString("brand") + " " + rs.getString("model") + " [" + rs.getString("status") + "]",
                        "IMPORTANT", "Maintenance"));
            }
        }
    }

    private static class AlertRow {
        final String title;
        final String message;
        final String level;
        final String category;

        AlertRow(String title, String message, String level, String category) {
            this.title = title;
            this.message = message;
            this.level = level;
            this.category = category;
        }
    }
}
