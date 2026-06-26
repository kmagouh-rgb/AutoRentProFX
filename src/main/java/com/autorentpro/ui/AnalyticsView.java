package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AnalyticsView {
    private final VBox root = new VBox(18);
    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    public AnalyticsView() {
        build();
        loadData();
    }

    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(24));
        Label title = new Label("Analyse du parc - V5.3");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Rentabilité par véhicule: revenus contrats, maintenance, dépenses et solde net.");
        subtitle.getStyleClass().add("muted");

        HBox summary = new HBox(16);
        summary.getChildren().addAll(
                miniCard("CA contrats", () -> Db.sum("contracts", "total_amount", "1=1")),
                miniCard("Encaissé", () -> Db.sum("contracts", "paid_amount", "1=1")),
                miniCard("Charges", () -> Db.sum("expenses", "amount", "1=1") + Db.sum("maintenance", "amount", "1=1")),
                miniCard("Solde net", () -> Db.sum("contracts", "paid_amount", "1=1") - Db.sum("expenses", "amount", "1=1") - Db.sum("maintenance", "amount", "1=1"))
        );

        TableView<Row> table = new TableView<>(rows);
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(330);

        TableColumn<Row, String> vehicle = new TableColumn<>("Véhicule");
        vehicle.setCellValueFactory(c -> c.getValue().vehicle);
        TableColumn<Row, Number> revenue = new TableColumn<>("Revenus");
        revenue.setCellValueFactory(c -> c.getValue().revenue);
        TableColumn<Row, Number> maintenance = new TableColumn<>("Maintenance");
        maintenance.setCellValueFactory(c -> c.getValue().maintenance);
        TableColumn<Row, Number> expenses = new TableColumn<>("Dépenses");
        expenses.setCellValueFactory(c -> c.getValue().expenses);
        TableColumn<Row, Number> net = new TableColumn<>("Net");
        net.setCellValueFactory(c -> c.getValue().net);
        table.getColumns().addAll(vehicle, revenue, maintenance, expenses, net);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Top rentabilité véhicules");
        chart.setLegendVisible(false);
        chart.setPrefHeight(300);
        rows.addListener((javafx.collections.ListChangeListener<Row>) c -> refreshChart(chart));

        root.getChildren().addAll(title, subtitle, summary, table, chart);
    }

    private VBox miniCard(String title, ValueProvider provider) {
        VBox b = new VBox(8);
        b.getStyleClass().add("stat-card");
        b.setPadding(new Insets(16));
        HBox.setHgrow(b, Priority.ALWAYS);
        Label t = new Label(title); t.getStyleClass().add("stat-title");
        Label v = new Label(String.format("%.2f DH", provider.get())); v.getStyleClass().add("stat-value");
        b.getChildren().addAll(t, v);
        return b;
    }

    private void loadData() {
        rows.clear();
        String sql = "SELECT v.id, COALESCE(v.registration, v.plate_number, '') AS reg, v.brand, v.model, " +
                "COALESCE(SUM(c.total_amount),0) AS revenue, " +
                "(SELECT COALESCE(SUM(m.amount),0) FROM maintenance m WHERE m.vehicle_id=v.id) AS maintenance, " +
                "(SELECT COALESCE(SUM(e.amount),0) FROM expenses e WHERE e.vehicle_id=v.id) AS expenses " +
                "FROM vehicles v LEFT JOIN contracts c ON c.vehicle_id=v.id " +
                "WHERE COALESCE(v.active,1)=1 GROUP BY v.id, reg, v.brand, v.model ORDER BY revenue DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("reg") + " - " + rs.getString("brand") + " " + rs.getString("model");
                double rev = rs.getDouble("revenue");
                double maint = rs.getDouble("maintenance");
                double exp = rs.getDouble("expenses");
                rows.add(new Row(name, rev, maint, exp));
            }
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
            a.setHeaderText("Erreur analyse");
            a.showAndWait();
        }
    }

    private void refreshChart(BarChart<String, Number> chart) {
        chart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        rows.stream().limit(8).forEach(r -> series.getData().add(new XYChart.Data<>(r.vehicle.get(), r.net.get())));
        chart.getData().add(series);
    }

    @FunctionalInterface
    private interface ValueProvider { double get(); }

    public static class Row {
        final SimpleStringProperty vehicle;
        final SimpleDoubleProperty revenue;
        final SimpleDoubleProperty maintenance;
        final SimpleDoubleProperty expenses;
        final SimpleDoubleProperty net;
        Row(String vehicle, double revenue, double maintenance, double expenses) {
            this.vehicle = new SimpleStringProperty(vehicle);
            this.revenue = new SimpleDoubleProperty(revenue);
            this.maintenance = new SimpleDoubleProperty(maintenance);
            this.expenses = new SimpleDoubleProperty(expenses);
            this.net = new SimpleDoubleProperty(revenue - maintenance - expenses);
        }
    }
}
