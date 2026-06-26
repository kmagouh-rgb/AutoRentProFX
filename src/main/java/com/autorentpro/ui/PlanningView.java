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
    private final VBox root = new VBox(18);
    private final GridPane grid = new GridPane();
    private final DatePicker from = new DatePicker(LocalDate.now());
    private final ComboBox<Integer> days = new ComboBox<>();

    public PlanningView() { build(); load(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(24));
        Label title = new Label("Planning réel des contrats");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("V5.2 continue depuis V5.1: le planning lit les véhicules et les contrats depuis la base de données.");
        subtitle.getStyleClass().add("muted");

        days.getItems().addAll(7, 14, 21, 30);
        days.setValue(14);
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("primary-button");
        refresh.setOnAction(e -> load());
        HBox toolbar = new HBox(10, new Label("Début"), from, new Label("Jours"), days, refresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        grid.getStyleClass().add("planning-card");
        grid.setPadding(new Insets(18));
        grid.setHgap(6);
        grid.setVgap(8);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        root.getChildren().addAll(title, subtitle, toolbar, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
    }

    private void load() {
        grid.getChildren().clear();
        LocalDate start = from.getValue() == null ? LocalDate.now() : from.getValue();
        int span = days.getValue() == null ? 14 : days.getValue();
        List<VehicleLine> vehicles = loadVehicles();
        List<ContractLine> contracts = loadContracts(start, start.plusDays(span - 1));

        grid.add(headerCell("Véhicule", 190), 0, 0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int d = 0; d < span; d++) grid.add(headerCell(start.plusDays(d).format(fmt), 72), d + 1, 0);

        int row = 1;
        for (VehicleLine v : vehicles) {
            Label name = new Label(v.registration + "\n" + v.brand + " " + v.model);
            name.getStyleClass().add("planning-car");
            name.setMinWidth(190);
            grid.add(name, 0, row);
            for (int d = 0; d < span; d++) {
                LocalDate day = start.plusDays(d);
                Label cell = new Label();
                cell.setMinSize(72, 42);
                cell.setAlignment(Pos.CENTER);
                cell.getStyleClass().add("planning-cell");
                ContractLine c = findContract(contracts, v.id, day);
                if (c != null) {
                    cell.setText(c.customer == null ? "Loué" : c.customer);
                    cell.setTooltip(new Tooltip(c.number + "\n" + c.customer + "\n" + c.start + " → " + c.end));
                    cell.getStyleClass().add(styleFor(c.status));
                }
                grid.add(cell, d + 1, row);
            }
            row++;
        }
        if (vehicles.isEmpty()) {
            Label empty = new Label("Aucun véhicule trouvé. Ajoutez d'abord des véhicules.");
            empty.getStyleClass().add("muted");
            grid.add(empty, 0, 1, span + 1, 1);
        }
    }

    private Label headerCell(String text, int width) {
        Label l = new Label(text);
        l.getStyleClass().add("planning-header");
        l.setMinWidth(width);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private String styleFor(String status) {
        if ("RESERVE".equalsIgnoreCase(status)) return "bar-blue";
        if ("FERME".equalsIgnoreCase(status)) return "bar-green";
        if ("ANNULE".equalsIgnoreCase(status)) return "bar-orange";
        return "bar-red";
    }

    private ContractLine findContract(List<ContractLine> list, int vehicleId, LocalDate day) {
        for (ContractLine c : list) {
            if (c.vehicleId == vehicleId && !day.isBefore(c.start) && !day.isAfter(c.end)) return c;
        }
        return null;
    }

    private List<VehicleLine> loadVehicles() {
        List<VehicleLine> list = new ArrayList<>();
        String sql = "SELECT id, registration, brand, model FROM vehicles WHERE active=1 ORDER BY brand, model";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new VehicleLine(rs.getInt("id"), rs.getString("registration"), rs.getString("brand"), rs.getString("model")));
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private List<ContractLine> loadContracts(LocalDate from, LocalDate to) {
        List<ContractLine> list = new ArrayList<>();
        String sql = "SELECT c.contract_number, c.vehicle_id, c.start_date, c.end_date, c.status, cu.full_name customer " +
                "FROM contracts c LEFT JOIN customers cu ON cu.id=c.customer_id " +
                "WHERE c.start_date <= ? AND c.end_date >= ? AND c.status <> 'ANNULE'";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(to));
            ps.setDate(2, java.sql.Date.valueOf(from));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ContractLine(rs.getString("contract_number"), rs.getInt("vehicle_id"),
                            rs.getDate("start_date").toLocalDate(), rs.getDate("end_date").toLocalDate(),
                            rs.getString("status"), rs.getString("customer")));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private record VehicleLine(int id, String registration, String brand, String model) {}
    private record ContractLine(String number, int vehicleId, LocalDate start, LocalDate end, String status, String customer) {}
}
