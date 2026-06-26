package com.autorentpro.ui;

import com.autorentpro.model.VehicleDocument;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;

public class DocumentsView {
    private final BorderPane root = new BorderPane();
    private final DocumentsRepository repo = new DocumentsRepository();
    private final TableView<VehicleDocument> table = new TableView<>();
    private final TextField search = new TextField();

    public DocumentsView() { build(); load(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(22));
        VBox top = new VBox(12);
        Label title = new Label("Documents véhicules");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Assurance, visite technique, vignette, carte grise et autres documents avec alertes d'expiration.");
        subtitle.getStyleClass().add("muted");
        HBox actions = new HBox(10);
        search.setPromptText("Recherche: véhicule, type, numéro document...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);
        Button add = new Button("+ Nouveau document"); add.getStyleClass().add("primary-button");
        Button edit = new Button("Modifier"); edit.getStyleClass().add("secondary-button");
        Button del = new Button("Supprimer"); del.getStyleClass().add("danger-button");
        actions.getChildren().addAll(search, add, edit, del);
        top.getChildren().addAll(title, subtitle, actions);
        root.setTop(top);

        setupTable();
        VBox center = new VBox(table);
        center.setPadding(new Insets(16,0,0,0));
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(center);

        search.textProperty().addListener((obs, old, val) -> load());
        add.setOnAction(e -> openForm(null));
        edit.setOnAction(e -> { VehicleDocument d = table.getSelectionModel().getSelectedItem(); if (d != null) openForm(d); });
        del.setOnAction(e -> deleteSelected());
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<VehicleDocument, String> vehicle = new TableColumn<>("Véhicule");
        vehicle.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getVehicleLabel())));
        TableColumn<VehicleDocument, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDocumentType())));
        TableColumn<VehicleDocument, String> num = new TableColumn<>("Numéro");
        num.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getDocumentNumber())));
        TableColumn<VehicleDocument, String> issue = new TableColumn<>("Date émission");
        issue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getIssueDate() == null ? "" : c.getValue().getIssueDate().toString()));
        TableColumn<VehicleDocument, String> expiry = new TableColumn<>("Expiration");
        expiry.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExpiryDate() == null ? "" : c.getValue().getExpiryDate().toString()));
        TableColumn<VehicleDocument, String> status = new TableColumn<>("Alerte");
        status.setCellValueFactory(c -> new SimpleStringProperty(alertText(c.getValue().getExpiryDate())));
        TableColumn<VehicleDocument, String> path = new TableColumn<>("Fichier");
        path.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getFilePath())));
        table.getColumns().addAll(vehicle, type, num, issue, expiry, status, path);
    }

    private String alertText(LocalDate expiry) {
        if (expiry == null) return "—";
        LocalDate today = LocalDate.now();
        if (expiry.isBefore(today)) return "EXPIRÉ";
        if (!expiry.isAfter(today.plusDays(30))) return "Bientôt";
        return "OK";
    }

    private void load() { table.setItems(repo.findAll(search.getText())); }

    private void openForm(VehicleDocument selected) {
        Dialog<VehicleDocument> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Nouveau document" : "Modifier document");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Map<Integer,String> vehicles = repo.vehicles();
        ComboBox<String> vehicleCombo = new ComboBox<>();
        vehicles.forEach((id,label) -> vehicleCombo.getItems().add(id + " | " + label));
        if (selected != null) vehicleCombo.getSelectionModel().select(selected.getVehicleId() + " | " + nvl(selected.getVehicleLabel()));
        else if (!vehicleCombo.getItems().isEmpty()) vehicleCombo.getSelectionModel().select(0);

        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("ASSURANCE", "VISITE TECHNIQUE", "VIGNETTE", "CARTE GRISE", "CONTRAT ACHAT", "AUTRE");
        type.setValue(selected == null ? "ASSURANCE" : selected.getDocumentType());
        TextField number = new TextField(selected == null ? "" : nvl(selected.getDocumentNumber()));
        DatePicker issue = new DatePicker(selected == null ? LocalDate.now() : selected.getIssueDate());
        DatePicker expiry = new DatePicker(selected == null ? LocalDate.now().plusYears(1) : selected.getExpiryDate());
        TextField file = new TextField(selected == null ? "" : nvl(selected.getFilePath()));
        Button browse = new Button("Parcourir"); browse.getStyleClass().add("secondary-button");
        browse.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir un document");
            File f = fc.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (f != null) file.setText(f.getAbsolutePath());
        });
        HBox fileBox = new HBox(8, file, browse); HBox.setHgrow(file, Priority.ALWAYS);
        TextArea notes = new TextArea(selected == null ? "" : nvl(selected.getNotes())); notes.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        int r=0;
        grid.addRow(r++, new Label("Véhicule"), vehicleCombo);
        grid.addRow(r++, new Label("Type document"), type);
        grid.addRow(r++, new Label("Numéro"), number);
        grid.addRow(r++, new Label("Date émission"), issue);
        grid.addRow(r++, new Label("Date expiration"), expiry);
        grid.addRow(r++, new Label("Fichier"), fileBox);
        grid.addRow(r, new Label("Notes"), notes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                int vehicleId = parseVehicleId(vehicleCombo.getValue());
                return new VehicleDocument(selected == null ? 0 : selected.getId(), vehicleId, "", type.getValue(), number.getText(), issue.getValue(), expiry.getValue(), file.getText(), notes.getText());
            }
            return null;
        });
        dialog.showAndWait().ifPresent(d -> { try { repo.save(d); load(); } catch (Exception ex) { alert("Erreur sauvegarde", ex.getMessage()); } });
    }

    private int parseVehicleId(String value) {
        try { return Integer.parseInt(value.split("\\|")[0].trim()); } catch (Exception e) { return 0; }
    }

    private void deleteSelected() {
        VehicleDocument d = table.getSelectionModel().getSelectedItem();
        if (d == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce document ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) { try { repo.delete(d.getId()); load(); } catch (Exception ex) { alert("Erreur suppression", ex.getMessage()); } }
        });
    }

    private String nvl(String s) { return s == null ? "" : s; }
    private void alert(String title, String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
