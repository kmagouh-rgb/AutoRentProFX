package com.autorentpro.ui;

import com.autorentpro.model.Maintenance;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.Map;

public class MaintenanceView {
    private final BorderPane root = new BorderPane();
    private final MaintenanceRepository repo = new MaintenanceRepository();
    private final TableView<Maintenance> table = new TableView<>();
    private final TextField search = new TextField();

    public MaintenanceView() { build(); load(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(22));
        VBox top = new VBox(12);
        Label title = new Label("Maintenance véhicules");
        title.getStyleClass().add("page-title");
        HBox actions = new HBox(10);
        search.setPromptText("Recherche: véhicule, type, statut, notes...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);
        Button add = new Button("+ Nouvelle maintenance"); add.getStyleClass().add("primary-button");
        Button edit = new Button("Modifier"); edit.getStyleClass().add("secondary-button");
        Button del = new Button("Supprimer"); del.getStyleClass().add("danger-button");
        actions.getChildren().addAll(search, add, edit, del);
        top.getChildren().addAll(title, actions);
        root.setTop(top);
        setupTable();
        VBox center = new VBox(table); center.setPadding(new Insets(16,0,0,0)); VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(center);

        search.textProperty().addListener((obs,o,n) -> load());
        add.setOnAction(e -> openForm(null));
        edit.setOnAction(e -> { Maintenance m = table.getSelectionModel().getSelectedItem(); if (m != null) openForm(m); });
        del.setOnAction(e -> deleteSelected());
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Maintenance,String> vehicle = new TableColumn<>("Véhicule"); vehicle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleLabel()));
        TableColumn<Maintenance,String> date = new TableColumn<>("Date"); date.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getMaintenanceDate())));
        TableColumn<Maintenance,String> type = new TableColumn<>("Type"); type.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        TableColumn<Maintenance,Number> km = new TableColumn<>("Km"); km.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getMileage()));
        TableColumn<Maintenance,Number> amount = new TableColumn<>("Montant"); amount.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getAmount()));
        TableColumn<Maintenance,String> status = new TableColumn<>("Statut"); status.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        TableColumn<Maintenance,String> notes = new TableColumn<>("Notes"); notes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes()));
        table.getColumns().addAll(vehicle, date, type, km, amount, status, notes);
    }

    private void load() { table.setItems(repo.findAll(search.getText())); }

    private void openForm(Maintenance selected) {
        Dialog<Maintenance> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Nouvelle maintenance" : "Modifier maintenance");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        Map<Integer,String> vehicles = repo.vehicles();
        ComboBox<String> vehicle = new ComboBox<>(); vehicles.forEach((id,label) -> vehicle.getItems().add(id + " - " + label));
        if (selected != null) vehicles.forEach((id,label) -> { if (id == selected.getVehicleId()) vehicle.setValue(id + " - " + label); }); else if (!vehicle.getItems().isEmpty()) vehicle.getSelectionModel().selectFirst();
        DatePicker date = new DatePicker(selected == null ? LocalDate.now() : selected.getMaintenanceDate());
        ComboBox<String> type = new ComboBox<>(); type.getItems().addAll("VIDANGE", "FILTRES", "PNEUS", "FREINS", "BATTERIE", "ASSURANCE", "VISITE_TECHNIQUE", "REPARATION", "AUTRE"); type.setValue(selected == null ? "VIDANGE" : selected.getType());
        TextField km = new TextField(selected == null ? "0" : String.valueOf(selected.getMileage()));
        TextField amount = new TextField(selected == null ? "0" : String.valueOf(selected.getAmount()));
        ComboBox<String> status = new ComboBox<>(); status.getItems().addAll("PLANIFIEE", "EN_COURS", "TERMINEE", "ANNULEE"); status.setValue(selected == null ? "TERMINEE" : selected.getStatus());
        TextArea notes = new TextArea(selected == null ? "" : selected.getNotes()); notes.setPrefRowCount(3);
        int r=0;
        grid.addRow(r++, new Label("Véhicule"), vehicle);
        grid.addRow(r++, new Label("Date"), date);
        grid.addRow(r++, new Label("Type"), type);
        grid.addRow(r++, new Label("Kilométrage"), km);
        grid.addRow(r++, new Label("Montant"), amount);
        grid.addRow(r++, new Label("Statut"), status);
        grid.addRow(r, new Label("Notes"), notes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) return new Maintenance(selected == null ? 0 : selected.getId(), extractId(vehicle.getValue()), "", date.getValue(), type.getValue(), parseInt(km.getText()), parseDouble(amount.getText()), status.getValue(), notes.getText());
            return null;
        });
        dialog.showAndWait().ifPresent(m -> { try { repo.save(m); load(); } catch(Exception ex) { alert("Erreur maintenance", ex.getMessage()); } });
    }

    private void deleteSelected() {
        Maintenance m = table.getSelectionModel().getSelectedItem(); if (m == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette maintenance ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) { try { repo.delete(m.getId()); load(); } catch(Exception ex) { alert("Erreur suppression", ex.getMessage()); } } });
    }

    private int extractId(String value) { try { return Integer.parseInt(value.split(" - ")[0].trim()); } catch(Exception e) { return 0; } }
    private int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch(Exception e) { return 0; } }
    private double parseDouble(String s) { try { return Double.parseDouble(s.trim().replace(',', '.')); } catch(Exception e) { return 0; } }
    private void alert(String title, String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
