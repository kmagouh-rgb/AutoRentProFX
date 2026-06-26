package com.autorentpro.ui;

import com.autorentpro.model.Vehicle;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class VehiclesView {
    private final BorderPane root = new BorderPane();
    private final VehicleRepository repo = new VehicleRepository();
    private final TableView<Vehicle> table = new TableView<>();
    private final TextField search = new TextField();

    public VehiclesView() { build(); load(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(22));
        VBox top = new VBox(12);
        Label title = new Label("Gestion des véhicules");
        title.getStyleClass().add("page-title");
        HBox actions = new HBox(10);
        search.setPromptText("Recherche: immatriculation, marque, modèle, situation technique...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);
        Button add = new Button("+ Nouveau véhicule"); add.getStyleClass().add("primary-button");
        Button edit = new Button("Modifier"); edit.getStyleClass().add("secondary-button");
        Button del = new Button("Supprimer"); del.getStyleClass().add("danger-button");
        actions.getChildren().addAll(search, add, edit, del);
        top.getChildren().addAll(title, actions);
        root.setTop(top);

        setupTable();
        VBox center = new VBox(table);
        center.setPadding(new Insets(16,0,0,0));
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(center);

        search.textProperty().addListener((obs, old, val) -> load());
        add.setOnAction(e -> openForm(null));
        edit.setOnAction(e -> {
            Vehicle v = table.getSelectionModel().getSelectedItem();
            if (v != null) openForm(v);
        });
        del.setOnAction(e -> deleteSelected());
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Vehicle, String> reg = new TableColumn<>("Immatriculation");
        reg.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRegistration()));
        TableColumn<Vehicle, String> brand = new TableColumn<>("Marque");
        brand.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBrand()));
        TableColumn<Vehicle, String> model = new TableColumn<>("Modèle");
        model.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModel()));
        TableColumn<Vehicle, Number> year = new TableColumn<>("Année");
        year.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getYear()));
        TableColumn<Vehicle, String> fuel = new TableColumn<>("Carburant");
        fuel.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFuel()));
        TableColumn<Vehicle, Number> mileage = new TableColumn<>("Km");
        mileage.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getMileage()));
        TableColumn<Vehicle, Number> price = new TableColumn<>("Prix/Jour");
        price.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getDailyPrice()));
        TableColumn<Vehicle, String> status = new TableColumn<>("Situation technique");
        status.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        table.getColumns().addAll(reg, brand, model, year, fuel, mileage, price, status);
    }

    private void load() { table.setItems(repo.findAll(search.getText())); }

    private void openForm(Vehicle selected) {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Nouveau véhicule" : "Modifier véhicule");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField reg = new TextField(selected == null ? "" : selected.getRegistration());
        TextField brand = new TextField(selected == null ? "" : selected.getBrand());
        TextField model = new TextField(selected == null ? "" : selected.getModel());
        TextField year = new TextField(selected == null ? "2024" : String.valueOf(selected.getYear()));
        ComboBox<String> fuel = new ComboBox<>(); fuel.getItems().addAll("Diesel", "Essence", "Hybride", "Electrique"); fuel.setValue(selected == null ? "Diesel" : selected.getFuel());
        ComboBox<String> trans = new ComboBox<>(); trans.getItems().addAll("Manuelle", "Automatique"); trans.setValue(selected == null ? "Manuelle" : selected.getTransmission());
        TextField km = new TextField(selected == null ? "0" : String.valueOf(selected.getMileage()));
        TextField price = new TextField(selected == null ? "0" : String.valueOf(selected.getDailyPrice()));
        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("EN_SERVICE", "MAINTENANCE", "HORS_SERVICE", "VENDUE");
        String selectedStatus = selected == null ? "EN_SERVICE" : normalizeTechnicalStatus(selected.getStatus());
        status.setValue(selectedStatus);

        TextField photo = new TextField(selected == null ? "" : selected.getPhotoPath());
        photo.setPromptText("Chemin de la photo du véhicule");
        Button choosePhoto = new Button("Parcourir");
        choosePhoto.getStyleClass().add("secondary-button");
        choosePhoto.setOnAction(ev -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir une photo du véhicule");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
            );
            File file = chooser.showOpenDialog(root.getScene() == null ? null : root.getScene().getWindow());
            if (file != null) photo.setText(file.getAbsolutePath());
        });
        HBox photoBox = new HBox(8, photo, choosePhoto);
        HBox.setHgrow(photo, Priority.ALWAYS);

        int r=0;
        grid.addRow(r++, new Label("Immatriculation"), reg);
        grid.addRow(r++, new Label("Marque"), brand);
        grid.addRow(r++, new Label("Modèle"), model);
        grid.addRow(r++, new Label("Année"), year);
        grid.addRow(r++, new Label("Carburant"), fuel);
        grid.addRow(r++, new Label("Transmission"), trans);
        grid.addRow(r++, new Label("Kilométrage"), km);
        grid.addRow(r++, new Label("Prix / jour"), price);
        grid.addRow(r++, new Label("Situation technique"), status);
        grid.addRow(r, new Label("Photo véhicule"), photoBox);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                return new Vehicle(selected == null ? 0 : selected.getId(), reg.getText(), brand.getText(), model.getText(), parseInt(year.getText()), fuel.getValue(), trans.getValue(), parseInt(km.getText()), parseDouble(price.getText()), status.getValue(), photo.getText());
            }
            return null;
        });
        dialog.showAndWait().ifPresent(v -> {
            try { repo.save(v); load(); } catch (Exception ex) { alert("Erreur sauvegarde", ex.getMessage()); }
        });
    }

    private void deleteSelected() {
        Vehicle v = table.getSelectionModel().getSelectedItem();
        if (v == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce véhicule ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try { repo.delete(v.getId()); load(); } catch (Exception ex) { alert("Erreur suppression", ex.getMessage()); }
            }
        });
    }

    private String normalizeTechnicalStatus(String s) {
        if (s == null || s.isBlank()) return "EN_SERVICE";
        String v = s.trim().toUpperCase();
        if (v.equals("DISPONIBLE") || v.equals("LOUEE") || v.equals("RESERVEE")) return "EN_SERVICE";
        return v;
    }

    private int parseInt(String s) { try { return Integer.parseInt(s.trim()); } catch(Exception e) { return 0; } }
    private double parseDouble(String s) { try { return Double.parseDouble(s.trim().replace(',', '.')); } catch(Exception e) { return 0; } }
    private void alert(String title, String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
