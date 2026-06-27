package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuditView {
    private final BorderPane root = new BorderPane();
    private final TableView<AuditRow> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> typeFilter = new ComboBox<>();

    public AuditView() {
        build();
        load();
    }

    public Parent getView() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(22));

        Label title = new Label("Journal des opérations");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Audit des actions importantes: contrats, clients, véhicules, paiements, maintenance.");
        subtitle.getStyleClass().add("muted");

        search.setPromptText("Recherche: utilisateur, action, référence, détail...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);

        typeFilter.getItems().addAll("Tous", "CONTRAT", "PAIEMENT", "VEHICULE", "CLIENT", "MAINTENANCE", "SYSTEM");
        typeFilter.setValue("Tous");

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> load());

        HBox actions = new HBox(10, search, typeFilter, refresh);
        VBox top = new VBox(10, title, subtitle, actions);
        root.setTop(top);

        setupTable();
        root.setCenter(table);

        search.textProperty().addListener((obs, old, val) -> load());
        typeFilter.setOnAction(e -> load());
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<AuditRow, String> dt = new TableColumn<>("Date");
        dt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().createdAt));

        TableColumn<AuditRow, String> user = new TableColumn<>("Utilisateur");
        user.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().username));

        TableColumn<AuditRow, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().entityType));

        TableColumn<AuditRow, String> action = new TableColumn<>("Action");
        action.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().action));

        TableColumn<AuditRow, String> ref = new TableColumn<>("Référence");
        ref.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reference));

        TableColumn<AuditRow, String> details = new TableColumn<>("Détails");
        details.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().details));

        table.getColumns().setAll(dt, user, type, action, ref, details);
    }

    private void load() {
        ObservableList<AuditRow> rows = FXCollections.observableArrayList();
        String q = search.getText() == null ? "" : search.getText().trim();
        String f = typeFilter.getValue() == null ? "Tous" : typeFilter.getValue();

        ensureTable();

        String sql = "SELECT created_at, username, entity_type, action, reference, details FROM audit_log " +
                "WHERE (?='Tous' OR entity_type=?) " +
                "AND (username LIKE ? OR entity_type LIKE ? OR action LIKE ? OR reference LIKE ? OR details LIKE ?) " +
                "ORDER BY id DESC LIMIT 500";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            String like = "%" + q + "%";
            ps.setString(1, f);
            ps.setString(2, f);
            for (int i = 3; i <= 7; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new AuditRow(
                            rs.getString("created_at"),
                            rs.getString("username"),
                            rs.getString("entity_type"),
                            rs.getString("action"),
                            rs.getString("reference"),
                            rs.getString("details")
                    ));
                }
            }
        } catch (Exception e) {
            rows.add(new AuditRow("", "SYSTEM", "SYSTEM", "ERREUR", "", e.getMessage()));
        }

        table.setItems(rows);
    }

    private void ensureTable() {
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS audit_log (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "username VARCHAR(100) DEFAULT 'admin'," +
                        "entity_type VARCHAR(50)," +
                        "action VARCHAR(100)," +
                        "reference VARCHAR(150)," +
                        "details TEXT" +
                        ")")) {
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    public static class AuditRow {
        final String createdAt;
        final String username;
        final String entityType;
        final String action;
        final String reference;
        final String details;

        AuditRow(String createdAt, String username, String entityType, String action, String reference, String details) {
            this.createdAt = createdAt == null ? "" : createdAt;
            this.username = username == null ? "" : username;
            this.entityType = entityType == null ? "" : entityType;
            this.action = action == null ? "" : action;
            this.reference = reference == null ? "" : reference;
            this.details = details == null ? "" : details;
        }
    }
}
