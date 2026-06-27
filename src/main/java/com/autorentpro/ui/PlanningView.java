package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PlanningView {
    private final BorderPane root = new BorderPane();
    private final GridPane grid = new GridPane();
    private final DatePicker from = new DatePicker(LocalDate.now());
    private final ComboBox<String> viewMode = new ComboBox<>();
    private final Label totalVehicles = new Label("0");
    private final Label availableToday = new Label("0");
    private final Label occupiedToday = new Label("0");
    private final Label reservedToday = new Label("0");
    private final Label blockedToday = new Label("0");

    public PlanningView() {
        build();
        load();
    }

    public Parent getView() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("planning-enterprise-root");

        VBox page = new VBox(16);
        page.setFillWidth(true);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titles = new VBox(4);
        Label title = new Label("Planning Enterprise");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Occupation du parc par dates: disponible, occupée, réservée et maintenance.");
        subtitle.getStyleClass().add("muted");
        titles.getChildren().addAll(title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button prev = new Button("←");
        prev.getStyleClass().add("secondary-button");
        prev.setOnAction(e -> { from.setValue(from.getValue().minusDays(span())); load(); });
        Button today = new Button("Aujourd'hui");
        today.getStyleClass().add("primary-button");
        today.setOnAction(e -> { from.setValue(LocalDate.now()); load(); });
        Button next = new Button("→");
        next.getStyleClass().add("secondary-button");
        next.setOnAction(e -> { from.setValue(from.getValue().plusDays(span())); load(); });

        header.getChildren().addAll(titles, spacer, prev, today, next);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);
        viewMode.getItems().addAll("Semaine", "2 semaines", "Mois");
        viewMode.setValue("2 semaines");
        viewMode.getStyleClass().add("filter-combo");
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("primary-button");
        refresh.setOnAction(e -> load());
        from.valueProperty().addListener((o, a, b) -> load());
        viewMode.valueProperty().addListener((o, a, b) -> load());
        controls.getChildren().addAll(new Label("Début"), from, new Label("Vue"), viewMode, refresh, legend());

        HBox stats = new HBox(12);
        stats.getStyleClass().add("fleet-stats-row");
        stats.getChildren().addAll(
                statCard("📦", "Total", totalVehicles, "stat-blue"),
                statCard("🟢", "Disponible aujourd'hui", availableToday, "stat-green"),
                statCard("🔴", "Occupée aujourd'hui", occupiedToday, "stat-red"),
                statCard("🟡", "Réservée aujourd'hui", reservedToday, "stat-yellow"),
                statCard("🟠", "Maintenance/HS", blockedToday, "stat-orange")
        );

        grid.getStyleClass().add("planning-enterprise-grid");
        grid.setPadding(new Insets(16));
        grid.setHgap(5);
        grid.setVgap(6);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        page.getChildren().addAll(header, controls, stats, scroll);
        root.setCenter(page);
    }

    private HBox legend() {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().addAll(
                legendItem("Disponible", "planning-available"),
                legendItem("Occupée", "planning-occupied"),
                legendItem("Réservée", "planning-reserved"),
                legendItem("Maintenance", "planning-blocked")
        );
        return box;
    }

    private Label legendItem(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll("planning-legend-item", style);
        return l;
    }

    private VBox statCard(String icon, String title, Label value, String styleClass) {
        VBox box = new VBox(4);
        box.getStyleClass().addAll("fleet-stat-card", styleClass);
        Label i = new Label(icon);
        i.getStyleClass().add("fleet-stat-icon");
        value.getStyleClass().add("fleet-stat-value");
        Label t = new Label(title);
        t.getStyleClass().add("fleet-stat-title");
        box.getChildren().addAll(i, value, t);
        return box;
    }

    private int span() {
        String mode = viewMode.getValue();
        if ("Semaine".equals(mode)) return 7;
        if ("Mois".equals(mode)) return 30;
        return 14;
    }

    private void load() {
        grid.getChildren().clear();
        LocalDate start = from.getValue() == null ? LocalDate.now() : from.getValue();
        int days = span();
        LocalDate end = start.plusDays(days - 1);
        List<VehicleLine> vehicles = loadVehicles();
        List<PlanningLine> lines = loadPlanningLines(start, end);
        LocalDate today = LocalDate.now();

        int todayAvailable = 0;
        int todayOccupied = 0;
        int todayReserved = 0;
        int todayBlocked = 0;

        grid.add(headerCell("Véhicule", 210), 0, 0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int d = 0; d < days; d++) {
            LocalDate date = start.plusDays(d);
            Label h = headerCell(date.format(fmt), 76);
            if (date.equals(today)) h.getStyleClass().add("planning-today-header");
            grid.add(h, d + 1, 0);
        }

        int row = 1;
        for (VehicleLine v : vehicles) {
            VBox vehicleBox = new VBox(2);
            vehicleBox.getStyleClass().add("planning-vehicle-box");
            Label reg = new Label(empty(v.registration));
            reg.getStyleClass().add("planning-vehicle-registration");
            Label desc = new Label((empty(v.brand) + " " + empty(v.model)).trim());
            desc.getStyleClass().add("muted");
            vehicleBox.getChildren().addAll(reg, desc);
            grid.add(vehicleBox, 0, row);

            String todayState = null;
            for (int d = 0; d < days; d++) {
                LocalDate day = start.plusDays(d);
                Label cell = new Label();
                cell.setMinSize(76, 48);
                cell.setMaxSize(76, 48);
                cell.setAlignment(Pos.CENTER);
                cell.getStyleClass().add("planning-enterprise-cell");

                CellState state = stateFor(v, lines, day);
                cell.getStyleClass().add(styleFor(state));
                cell.setText(textFor(state));
                cell.setTooltip(new Tooltip(tooltipFor(v, state, day)));
                cell.setOnMouseClicked(e -> showCellDetails(v, state, day));

                if (day.equals(today)) todayState = state.type;
                grid.add(cell, d + 1, row);
            }
            if ("AVAILABLE".equals(todayState) || todayState == null) todayAvailable++;
            else if ("OCCUPIED".equals(todayState)) todayOccupied++;
            else if ("RESERVED".equals(todayState)) todayReserved++;
            else todayBlocked++;
            row++;
        }

        totalVehicles.setText(String.valueOf(vehicles.size()));
        availableToday.setText(String.valueOf(todayAvailable));
        occupiedToday.setText(String.valueOf(todayOccupied));
        reservedToday.setText(String.valueOf(todayReserved));
        blockedToday.setText(String.valueOf(todayBlocked));

        if (vehicles.isEmpty()) {
            Label empty = new Label("Aucun véhicule trouvé.");
            empty.getStyleClass().add("muted");
            grid.add(empty, 0, 1, days + 1, 1);
        }
    }

    private Label headerCell(String text, int width) {
        Label label = new Label(text);
        label.getStyleClass().add("planning-header");
        label.setMinWidth(width);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private CellState stateFor(VehicleLine vehicle, List<PlanningLine> lines, LocalDate day) {
        String tech = normalize(vehicle.status);
        if ("MAINTENANCE".equals(tech) || "HORS_SERVICE".equals(tech) || "VENDUE".equals(tech)) {
            return new CellState("BLOCKED", "Maintenance/HS", null, null, null, tech);
        }
        for (PlanningLine l : lines) {
            if (l.vehicleId == vehicle.id && !day.isBefore(l.start) && !day.isAfter(l.end)) {
                String status = normalize(l.status);
                if ("RESERVE".equals(status) || "RESERVED".equals(status)) {
                    return new CellState("RESERVED", "Réservée", l.number, l.customer, l.end, l.status);
                }
                return new CellState("OCCUPIED", "Occupée", l.number, l.customer, l.end, l.status);
            }
        }
        return new CellState("AVAILABLE", "Disponible", null, null, null, "");
    }

    private String styleFor(CellState state) {
        return switch (state.type) {
            case "OCCUPIED" -> "planning-occupied";
            case "RESERVED" -> "planning-reserved";
            case "BLOCKED" -> "planning-blocked";
            default -> "planning-available";
        };
    }

    private String textFor(CellState state) {
        return switch (state.type) {
            case "OCCUPIED" -> "Louée";
            case "RESERVED" -> "Rés.";
            case "BLOCKED" -> "Stop";
            default -> "✓";
        };
    }

    private String tooltipFor(VehicleLine v, CellState s, LocalDate day) {
        return empty(v.registration) + " - " + empty(v.brand) + " " + empty(v.model) + "\n" +
                "Date: " + day + "\n" +
                "Etat: " + s.label +
                (s.reference == null ? "" : "\nRéf: " + s.reference) +
                (s.customer == null ? "" : "\nClient: " + s.customer) +
                (s.end == null ? "" : "\nFin: " + s.end) +
                (s.status == null || s.status.isBlank() ? "" : "\nStatut: " + s.status);
    }

    private void showCellDetails(VehicleLine v, CellState s, LocalDate day) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Planning");
        a.setHeaderText(empty(v.registration) + " - " + s.label);
        a.setContentText(tooltipFor(v, s, day));
        a.showAndWait();
    }

    private List<VehicleLine> loadVehicles() {
        List<VehicleLine> list = new ArrayList<>();
        String sql = "SELECT id, registration, brand, model, status FROM vehicles WHERE active=1 ORDER BY brand, model, registration";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new VehicleLine(rs.getInt("id"), rs.getString("registration"), rs.getString("brand"), rs.getString("model"), rs.getString("status")));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private List<PlanningLine> loadPlanningLines(LocalDate from, LocalDate to) {
        List<PlanningLine> list = new ArrayList<>();
        String sql = "SELECT c.contract_number ref, c.vehicle_id, c.start_date, c.end_date, c.status, cu.full_name customer " +
                "FROM contracts c LEFT JOIN customers cu ON cu.id=c.customer_id " +
                "WHERE c.start_date <= ? AND c.end_date >= ? AND UPPER(c.status) NOT IN ('ANNULE','CLOTURE','FERME') " +
                "UNION ALL " +
                "SELECT CONCAT('RES-', r.id) ref, r.vehicle_id, r.start_date, r.end_date, r.status, cu.full_name customer " +
                "FROM reservations r LEFT JOIN customers cu ON cu.id=r.customer_id " +
                "WHERE r.start_date <= ? AND r.end_date >= ? AND UPPER(r.status) NOT IN ('ANNULE','CANCELLED')";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(to));
            ps.setDate(2, java.sql.Date.valueOf(from));
            ps.setDate(3, java.sql.Date.valueOf(to));
            ps.setDate(4, java.sql.Date.valueOf(from));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlanningLine(rs.getString("ref"), rs.getInt("vehicle_id"),
                            rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(),
                            rs.getString("status"), rs.getString("customer")));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String v = s.trim().toUpperCase();
        if ("RESERVEE".equals(v) || "RÉSERVÉE".equals(v)) return "RESERVE";
        if ("EN_SERVICE".equals(v)) return "";
        return v;
    }

    private String empty(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private record VehicleLine(int id, String registration, String brand, String model, String status) {}
    private record PlanningLine(String number, int vehicleId, LocalDate start, LocalDate end, String status, String customer) {}
    private record CellState(String type, String label, String reference, String customer, LocalDate end, String status) {}
}
