package com.autorentpro.ui;

import com.autorentpro.db.Db;
import com.autorentpro.model.Vehicle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Vehicle360View {
    private final BorderPane root = new BorderPane();
    private final VehicleRepository repo = new VehicleRepository();
    private final ListView<Vehicle> vehicleList = new ListView<>();
    private final TextField search = new TextField();
    private final VBox detail = new VBox(14);

    public Vehicle360View() {
        build();
        loadVehicles();
    }

    public Parent getView() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(22));

        Label title = new Label("Fiche 360° Véhicule");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue complète: identité, disponibilité actuelle, contrats, revenus, dépenses et rentabilité.");
        subtitle.getStyleClass().add("muted");

        HBox topActions = new HBox(10);
        search.setPromptText("Recherche: immatriculation, marque, modèle...");
        search.getStyleClass().add("search-field");
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("secondary-button");
        topActions.getChildren().addAll(search, refresh);
        HBox.setHgrow(search, Priority.ALWAYS);

        VBox header = new VBox(10, title, subtitle, topActions);
        root.setTop(header);

        vehicleList.setPrefWidth(330);
        vehicleList.getStyleClass().add("data-table");
        vehicleList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Vehicle v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label name = new Label(safe(v.getBrand()) + " " + safe(v.getModel()));
                name.setStyle("-fx-font-weight: 900; -fx-font-size: 13px;");
                Label plate = new Label(safe(v.getRegistration()));
                plate.getStyleClass().add("muted");
                Label state = new Label(safe(v.getStatus()));
                state.getStyleClass().add(statusClass(v.getStatus()));
                VBox box = new VBox(4, name, plate, state);
                box.setPadding(new Insets(8));
                setGraphic(box);
            }
        });

        detail.setPadding(new Insets(0, 0, 0, 18));
        ScrollPane detailScroll = new ScrollPane(detail);
        detailScroll.setFitToWidth(true);
        detailScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        SplitPane split = new SplitPane(vehicleList, detailScroll);
        split.setDividerPositions(0.27);
        split.setPadding(new Insets(16, 0, 0, 0));
        root.setCenter(split);

        vehicleList.getSelectionModel().selectedItemProperty().addListener((obs, old, v) -> showVehicle(v));
        search.textProperty().addListener((obs, old, val) -> loadVehicles());
        refresh.setOnAction(e -> loadVehicles());
    }

    private void loadVehicles() {
        ObservableList<Vehicle> vehicles = repo.findAll(search.getText());
        vehicleList.setItems(vehicles);
        if (!vehicles.isEmpty()) {
            vehicleList.getSelectionModel().selectFirst();
        } else {
            detail.getChildren().setAll(emptyBox("Aucun véhicule trouvé."));
        }
    }

    private void showVehicle(Vehicle v) {
        if (v == null) {
            detail.getChildren().setAll(emptyBox("Sélectionnez un véhicule."));
            return;
        }

        VehicleStats stats = loadStats(v.getId());

        HBox hero = new HBox(18);
        hero.getStyleClass().add("content-card");
        hero.setPadding(new Insets(18));
        hero.setAlignment(Pos.CENTER_LEFT);

        StackPane photo = photoBox(v);
        VBox info = new VBox(8);
        Label title = new Label(safe(v.getBrand()) + " " + safe(v.getModel()) + " " + (v.getYear() > 0 ? v.getYear() : ""));
        title.getStyleClass().add("page-title");
        Label plate = new Label("Immatriculation: " + safe(v.getRegistration()));
        plate.getStyleClass().add("plate-badge");
        Label current = new Label(stats.currentStatus);
        current.getStyleClass().add(stats.currentBusy ? "status-louee" : "status-disponible");
        Label tech = new Label("État technique: " + safe(v.getStatus()));
        tech.getStyleClass().add(statusClass(v.getStatus()));
        info.getChildren().addAll(title, plate, current, tech);
        HBox.setHgrow(info, Priority.ALWAYS);
        hero.getChildren().addAll(photo, info);

        HBox kpis = new HBox(12,
                kpi("Contrats", String.valueOf(stats.contractCount), "📄"),
                kpi("Revenus", money(stats.revenue), "💰"),
                kpi("Maintenance", money(stats.maintenance), "🛠"),
                kpi("Dépenses", money(stats.expenses), "💸"),
                kpi("Solde net", money(stats.revenue - stats.maintenance - stats.expenses), "📈")
        );
        kpis.getStyleClass().add("fleet-stats-row");

        HBox technical = new HBox(12,
                infoCard("Carburant", safe(v.getFuel())),
                infoCard("Boîte", safe(v.getTransmission())),
                infoCard("Kilométrage", v.getMileage() + " km"),
                infoCard("Prix/jour", money(v.getDailyPrice()))
        );

        VBox occupation = section("Disponibilité & exploitation",
                new Label("Taux d'occupation estimé: " + stats.occupationRate + "%"),
                progress(stats.occupationRate),
                new Label("Jours loués cette année: " + stats.rentedDays),
                new Label(stats.nextEvent)
        );

        VBox contracts = section("Derniers contrats", listBox(stats.recentContracts));
        VBox costs = section("Dernières opérations maintenance / dépenses", listBox(stats.recentCosts));

        VBox identity = section("Identité véhicule",
                new Label("Immatriculation : " + safe(v.getRegistration())),
                new Label("Marque / Modèle : " + safe(v.getBrand()) + " " + safe(v.getModel())),
                new Label("Année : " + (v.getYear() > 0 ? String.valueOf(v.getYear()) : "-")),
                new Label("Carburant : " + safe(v.getFuel())),
                new Label("Boîte : " + safe(v.getTransmission())),
                new Label("Kilométrage : " + v.getMileage() + " km"),
                new Label("Prix / jour : " + money(v.getDailyPrice()))
        );

        Button actionContract = new Button(stats.currentBusy ? "Voir contrat actif" : "Nouveau contrat");
        actionContract.getStyleClass().add("primary-button");
        actionContract.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                stats.currentBusy ? stats.nextEvent : "Depuis cette fiche, le prochain développement ouvrira directement Contrats PRO avec ce véhicule.",
                ButtonType.OK).showAndWait());

        Button actionPlanning = new Button("Planning véhicule");
        actionPlanning.getStyleClass().add("secondary-button");
        actionPlanning.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                "Le planning détaillé du véhicule sera relié dans la prochaine étape.",
                ButtonType.OK).showAndWait());

        Button actionDocs = new Button("Documents");
        actionDocs.getStyleClass().add("secondary-button");
        actionDocs.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                "Documents véhicule: carte grise, assurance, visite technique, factures et photos.",
                ButtonType.OK).showAndWait());

        HBox quickActions = new HBox(10, actionContract, actionPlanning, actionDocs);
        quickActions.getStyleClass().add("quick-actions-bar");

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("vehicle360-tabs");
        Tab tabIdentity = new Tab("Fiche", new VBox(12, identity, technical, quickActions));
        Tab tabContracts = new Tab("Contrats", contracts);
        Tab tabCosts = new Tab("Maintenance & dépenses", costs);
        Tab tabStats = new Tab("Rentabilité", new VBox(12, occupation,
                section("Résumé financier",
                        new Label("Revenus : " + money(stats.revenue)),
                        new Label("Maintenance : " + money(stats.maintenance)),
                        new Label("Dépenses : " + money(stats.expenses)),
                        new Label("Solde net : " + money(stats.revenue - stats.maintenance - stats.expenses))
                )));
        tabs.getTabs().addAll(tabIdentity, tabContracts, tabCosts, tabStats);
        tabs.getTabs().forEach(t -> t.setClosable(false));

        detail.getChildren().setAll(hero, kpis, tabs);
    }

    private StackPane photoBox(Vehicle v) {
        StackPane box = new StackPane();
        box.setPrefSize(260, 160);
        box.getStyleClass().add("vehicle-photo-area");
        String path = v.getPhotoPath();
        if (path != null && !path.isBlank() && new File(path).exists()) {
            ImageView iv = new ImageView(new Image(new File(path).toURI().toString(), 260, 160, true, true));
            iv.setPreserveRatio(true);
            Rectangle clip = new Rectangle(260, 160);
            clip.setArcWidth(18);
            clip.setArcHeight(18);
            iv.setClip(clip);
            box.getChildren().add(iv);
        } else {
            Label icon = new Label("🚗");
            icon.getStyleClass().add("vehicle-photo-icon");
            box.getChildren().add(icon);
        }
        return box;
    }

    private VBox kpi(String title, String value, String icon) {
        VBox box = new VBox(4);
        box.getStyleClass().add("fleet-stat-card");
        Label i = new Label(icon);
        i.getStyleClass().add("fleet-stat-icon");
        Label t = new Label(title);
        t.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("fleet-stat-title");
        box.getChildren().addAll(i, t, v);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox infoCard(String title, String value) {
        VBox box = new VBox(5);
        box.getStyleClass().add("content-card");
        box.setPadding(new Insets(14));
        Label t = new Label(title);
        t.getStyleClass().add("muted");
        Label v = new Label(value == null || value.isBlank() ? "-" : value);
        v.setStyle("-fx-font-weight: 900; -fx-font-size: 15px;");
        box.getChildren().addAll(t, v);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox section(String title, javafx.scene.Node... nodes) {
        VBox box = new VBox(10);
        box.getStyleClass().add("content-card");
        box.setPadding(new Insets(16));
        Label t = new Label(title);
        t.getStyleClass().add("section-title");
        box.getChildren().add(t);
        box.getChildren().addAll(nodes);
        return box;
    }

    private ProgressBar progress(int value) {
        ProgressBar p = new ProgressBar(Math.max(0, Math.min(value, 100)) / 100.0);
        p.setMaxWidth(Double.MAX_VALUE);
        return p;
    }

    private VBox listBox(ObservableList<String> rows) {
        VBox box = new VBox(6);
        if (rows.isEmpty()) {
            Label empty = new Label("Aucune donnée.");
            empty.getStyleClass().add("muted");
            box.getChildren().add(empty);
            return box;
        }
        for (String row : rows) {
            Label l = new Label(row);
            l.setWrapText(true);
            l.setStyle("-fx-background-color: #f7faff; -fx-background-radius: 10; -fx-padding: 9 12; -fx-text-fill: #10223d;");
            box.getChildren().add(l);
        }
        return box;
    }

    private VBox emptyBox(String msg) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(25));
        box.getStyleClass().add("content-card");
        Label l = new Label(msg);
        l.getStyleClass().add("muted");
        box.getChildren().add(l);
        return box;
    }

    private VehicleStats loadStats(int vehicleId) {
        VehicleStats s = new VehicleStats();
        LocalDate today = LocalDate.now();
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(today.getYear(), 12, 31);

        try (Connection cn = Db.getConnection()) {
            s.contractCount = intQuery(cn, "SELECT COUNT(*) FROM contracts WHERE vehicle_id=?", vehicleId);
            s.revenue = doubleQuery(cn, "SELECT COALESCE(SUM(total_amount),0) FROM contracts WHERE vehicle_id=? AND UPPER(status) NOT IN ('ANNULE','CANCELLED')", vehicleId);
            s.maintenance = doubleQuery(cn, "SELECT COALESCE(SUM(amount),0) FROM maintenance WHERE vehicle_id=?", vehicleId);
            s.expenses = doubleQuery(cn, "SELECT COALESCE(SUM(amount),0) FROM expenses WHERE vehicle_id=?", vehicleId);
            s.rentedDays = rentedDays(cn, vehicleId, yearStart, yearEnd);
            s.occupationRate = (int) Math.round((s.rentedDays * 100.0) / Math.max(1, ChronoUnit.DAYS.between(yearStart, today) + 1));
            s.currentBusy = exists(cn, "SELECT 1 FROM contracts WHERE vehicle_id=? AND UPPER(status) IN ('ACTIVE','RESERVE','RESERVED','OPEN','EN_COURS') AND start_date<=CURDATE() AND end_date>=CURDATE() LIMIT 1", vehicleId);
            s.currentStatus = s.currentBusy ? "OCCUPÉE AUJOURD'HUI" : "DISPONIBLE AUJOURD'HUI";
            s.nextEvent = nextEvent(cn, vehicleId);
            s.recentContracts = rows(cn,
                    "SELECT CONCAT(contract_number, ' | ', COALESCE(start_date,''), ' → ', COALESCE(end_date,''), ' | ', COALESCE(status,''), ' | ', COALESCE(total_amount,0), ' DH') txt FROM contracts WHERE vehicle_id=? ORDER BY id DESC LIMIT 6",
                    vehicleId);
            s.recentCosts = rows(cn,
                    "SELECT CONCAT('Maintenance: ', COALESCE(maintenance_date,''), ' | ', COALESCE(type,''), ' | ', COALESCE(amount,0), ' DH') txt FROM maintenance WHERE vehicle_id=? " +
                            "UNION ALL SELECT CONCAT('Dépense: ', COALESCE(expense_date,''), ' | ', COALESCE(category,''), ' | ', COALESCE(amount,0), ' DH') txt FROM expenses WHERE vehicle_id=? LIMIT 8",
                    vehicleId, vehicleId);
        } catch (Exception e) {
            s.nextEvent = "Erreur lecture données: " + e.getMessage();
        }
        return s;
    }

    private int rentedDays(Connection cn, int vehicleId, LocalDate from, LocalDate to) {
        int total = 0;
        String sql = "SELECT start_date,end_date FROM contracts WHERE vehicle_id=? AND UPPER(status) NOT IN ('ANNULE','CANCELLED') AND start_date<=? AND end_date>=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            ps.setDate(2, java.sql.Date.valueOf(to));
            ps.setDate(3, java.sql.Date.valueOf(from));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate a = rs.getDate(1).toLocalDate();
                    LocalDate b = rs.getDate(2).toLocalDate();
                    if (a.isBefore(from)) a = from;
                    if (b.isAfter(to)) b = to;
                    total += (int) ChronoUnit.DAYS.between(a, b) + 1;
                }
            }
        } catch (Exception ignored) {}
        return Math.max(total, 0);
    }

    private String nextEvent(Connection cn, int vehicleId) {
        String sql = "SELECT contract_number,start_date,end_date,status FROM contracts WHERE vehicle_id=? AND end_date>=CURDATE() ORDER BY start_date LIMIT 1";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "Prochain/actuel contrat: " + rs.getString("contract_number") + " du " + rs.getDate("start_date") + " au " + rs.getDate("end_date") + " [" + rs.getString("status") + "]";
                }
            }
        } catch (Exception ignored) {}
        return "Aucun contrat futur.";
    }

    private int intQuery(Connection cn, String sql, int vehicleId) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    private double doubleQuery(Connection cn, String sql, int vehicleId) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        }
    }

    private boolean exists(Connection cn, String sql, int vehicleId) {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (Exception e) { return false; }
    }

    private ObservableList<String> rows(Connection cn, String sql, int... ids) {
        ObservableList<String> list = FXCollections.observableArrayList();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < ids.length; i++) ps.setInt(i + 1, ids[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("txt"));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private String statusClass(String status) {
        String s = status == null ? "" : status.toUpperCase();
        if (s.contains("MAINT")) return "status-maintenance";
        if (s.contains("VEND")) return "status-vendue";
        if (s.contains("HORS")) return "status-louee";
        return "status-disponible";
    }

    private String safe(String value) { return value == null ? "" : value; }
    private String money(double v) { return String.format("%.2f DH", v); }

    private static class VehicleStats {
        int contractCount;
        double revenue;
        double maintenance;
        double expenses;
        int rentedDays;
        int occupationRate;
        boolean currentBusy;
        String currentStatus = "DISPONIBLE AUJOURD'HUI";
        String nextEvent = "";
        ObservableList<String> recentContracts = FXCollections.observableArrayList();
        ObservableList<String> recentCosts = FXCollections.observableArrayList();
    }
}
