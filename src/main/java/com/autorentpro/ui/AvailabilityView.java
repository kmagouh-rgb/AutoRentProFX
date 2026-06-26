package com.autorentpro.ui;

import com.autorentpro.model.AvailabilityRow;
import com.autorentpro.model.Contract;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.Map;

public class AvailabilityView {
    private final BorderPane root = new BorderPane();
    private final AvailabilityRepository repo = new AvailabilityRepository();
    private final ContractsRepository contractsRepo = new ContractsRepository();
    private final TableView<AvailabilityRow> table = new TableView<>();
    private final DatePicker start = new DatePicker(LocalDate.now());
    private final DatePicker end = new DatePicker(LocalDate.now().plusDays(1));
    private final TextField search = new TextField();
    private final CheckBox onlyAvailable = new CheckBox("Afficher seulement disponibles");
    private final Label summary = new Label();

    public AvailabilityView() { build(); load(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(22));
        VBox top = new VBox(12);
        Label title = new Label("Disponibilité et création rapide de contrat");
        title.getStyleClass().add("page-title");
        Label note = new Label("La disponibilité se calcule uniquement par période depuis les Contrats + Réservations. Gestion véhicules garde seulement l'état technique.");
        note.getStyleClass().add("muted");

        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER_LEFT);
        search.setPromptText("Recherche véhicule, matricule, statut technique...");
        search.getStyleClass().add("search-field");
        Button refresh = new Button("Rechercher disponibilité");
        refresh.getStyleClass().add("primary-button");
        Button createContract = new Button("Créer contrat avec voiture sélectionnée");
        createContract.getStyleClass().add("secondary-button");
        filters.getChildren().addAll(new Label("Du"), start, new Label("Au"), end, search, onlyAvailable, refresh, createContract);
        HBox.setHgrow(search, Priority.ALWAYS);
        summary.getStyleClass().add("pill");
        top.getChildren().addAll(title, note, filters, summary);
        root.setTop(top);

        setupTable();
        root.setCenter(table);
        refresh.setOnAction(e -> load());
        createContract.setOnAction(e -> createContractFromSelectedVehicle());
        search.textProperty().addListener((o,a,b)-> load());
        onlyAvailable.selectedProperty().addListener((o,a,b)-> load());
        start.valueProperty().addListener((o,a,b)-> load());
        end.valueProperty().addListener((o,a,b)-> load());
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<AvailabilityRow,String> reg = new TableColumn<>("Immatriculation");
        reg.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRegistration()));
        TableColumn<AvailabilityRow,String> veh = new TableColumn<>("Véhicule");
        veh.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleLabel()));
        TableColumn<AvailabilityRow,Number> price = new TableColumn<>("Prix/Jour");
        price.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getDailyPrice()));
        TableColumn<AvailabilityRow,String> general = new TableColumn<>("État technique");
        general.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGeneralStatus()));
        TableColumn<AvailabilityRow,String> dispo = new TableColumn<>("Disponibilité dates");
        dispo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDateStatus()));
        dispo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("availability-pill-cell", "availability-disponible", "availability-occupee", "availability-blocked");
                setText(null);
                if (!empty && value != null) {
                    setText(value);
                    getStyleClass().add("availability-pill-cell");
                    if ("DISPONIBLE".equalsIgnoreCase(value)) {
                        getStyleClass().add("availability-disponible");
                    } else if ("OCCUPÉE".equalsIgnoreCase(value) || "OCCUPEE".equalsIgnoreCase(value)) {
                        getStyleClass().add("availability-occupee");
                    } else {
                        getStyleClass().add("availability-blocked");
                    }
                }
            }
        });
        TableColumn<AvailabilityRow,String> conflict = new TableColumn<>("Contrat/Réservation bloquante");
        conflict.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConflict()));
        table.getColumns().addAll(reg, veh, price, general, dispo, conflict);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(AvailabilityRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-available", "row-busy", "row-blocked");
                setStyle("");
                if (!empty && item != null) {
                    String status = item.getDateStatus() == null ? "" : item.getDateStatus().toUpperCase();
                    String tech = item.getGeneralStatus() == null ? "" : item.getGeneralStatus().toUpperCase();
                    if (tech.contains("MAINTENANCE") || tech.contains("HORS") || tech.contains("VENDUE")) {
                        getStyleClass().add("row-blocked");
                    } else if ("DISPONIBLE".equals(status)) {
                        getStyleClass().add("row-available");
                    } else {
                        getStyleClass().add("row-busy");
                    }
                }
            }
        });
    }

    private void load() {
        try {
            if (start.getValue() == null || end.getValue() == null || end.getValue().isBefore(start.getValue())) {
                summary.setText("Choisissez une période correcte.");
                table.getItems().clear();
                return;
            }
            var rows = repo.find(start.getValue(), end.getValue(), search.getText(), onlyAvailable.isSelected());
            table.setItems(rows);
            long available = rows.stream().filter(r -> "DISPONIBLE".equals(r.getDateStatus())).count();
            long busy = rows.size() - available;
            summary.setText("Période: " + start.getValue() + " → " + end.getValue() + " | Disponibles: " + available + " | Occupées: " + busy);
        } catch (Exception ex) {
            summary.setText("Erreur: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void createContractFromSelectedVehicle() {
        AvailabilityRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) { alert("Sélection", "Sélectionnez une voiture dans la liste."); return; }
        if (!"DISPONIBLE".equals(row.getDateStatus())) {
            alert("Voiture non disponible", "Cette voiture est déjà occupée pour cette période:\n" + row.getConflict());
            return;
        }
        try {
            ContractsRepository.ContractConflict conflict = contractsRepo.findDateConflict(row.getVehicleId(), start.getValue(), end.getValue(), 0);
            if (conflict != null) { alert("Voiture non disponible", "Conflit avec " + conflict.contractNumber + " du " + conflict.startDate + " au " + conflict.endDate); return; }
        } catch(Exception ex) { alert("Erreur disponibilité", ex.getMessage()); return; }

        Dialog<Contract> dialog = new Dialog<>();
        dialog.setTitle("Créer contrat - " + row.getRegistration());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField vehicle = new TextField(row.getRegistration() + " - " + row.getVehicleLabel());
        vehicle.setEditable(false);
        Map<Integer,String> customers = contractsRepo.customers();
        ComboBox<String> customer = new ComboBox<>();
        customers.forEach((id,label) -> customer.getItems().add(id + " - " + label));
        if (!customer.getItems().isEmpty()) customer.setValue(customer.getItems().get(0));
        TextField price = new TextField(String.valueOf(row.getDailyPrice()));
        if (row.getDailyPrice() <= 0) price.setText("0");
        TextField paid = new TextField("0");
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("ACTIVE", "RESERVE");
        status.setValue("ACTIVE");
        Label total = new Label(); total.getStyleClass().add("pill");
        Runnable calc = () -> total.setText("Total: " + (ContractsRepository.days(start.getValue(), end.getValue()) * parseDouble(price.getText())) + " DH");
        price.textProperty().addListener((o,a,b)->calc.run()); calc.run();
        int r=0;
        grid.addRow(r++, new Label("Véhicule"), vehicle);
        grid.addRow(r++, new Label("Client"), customer);
        grid.addRow(r++, new Label("Départ"), new Label(String.valueOf(start.getValue())));
        grid.addRow(r++, new Label("Retour"), new Label(String.valueOf(end.getValue())));
        grid.addRow(r++, new Label("Prix / jour"), price);
        grid.addRow(r++, new Label("Payé"), paid);
        grid.addRow(r++, new Label("Statut"), status);
        grid.add(total, 1, r);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                int customerId = extractId(customer.getValue());
                double daily = parseDouble(price.getText());
                double totalAmount = ContractsRepository.days(start.getValue(), end.getValue()) * daily;
                return new Contract(0, null, row.getVehicleId(), customerId, row.getVehicleLabel(), "", start.getValue(), end.getValue(), daily, totalAmount, parseDouble(paid.getText()), status.getValue());
            }
            return null;
        });
        dialog.showAndWait().ifPresent(c -> {
            try {
                contractsRepo.save(c);
                load();
                new Alert(Alert.AlertType.INFORMATION, "Contrat créé avec succès. La voiture est maintenant bloquée pour cette période.", ButtonType.OK).showAndWait();
            } catch(Exception ex) { alert("Erreur création contrat", ex.getMessage()); }
        });
    }

    private int extractId(String value) {
        if (value == null || value.isBlank()) return 0;
        return Integer.parseInt(value.split(" - ")[0].trim());
    }
    private double parseDouble(String s) {
        try { return Double.parseDouble(s == null ? "0" : s.replace(",", ".")); } catch(Exception e) { return 0; }
    }
    private void alert(String title, String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
