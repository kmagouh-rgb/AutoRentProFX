package com.autorentpro.ui;

import com.autorentpro.model.Expense;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.Map;

public class ExpensesView {
    private final BorderPane root = new BorderPane();
    private final ExpenseRepository repo = new ExpenseRepository();
    private final TableView<Expense> table = new TableView<>();
    private final TextField search = new TextField();

    public ExpensesView() { build(); load(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(22));
        VBox top = new VBox(12);
        Label title = new Label("Dépenses"); title.getStyleClass().add("page-title");
        HBox actions = new HBox(10);
        search.setPromptText("Recherche: libellé, catégorie, véhicule..."); search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);
        Button add = new Button("+ Nouvelle dépense"); add.getStyleClass().add("primary-button");
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
        edit.setOnAction(e -> { Expense ex = table.getSelectionModel().getSelectedItem(); if (ex != null) openForm(ex); });
        del.setOnAction(e -> deleteSelected());
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Expense,String> date = new TableColumn<>("Date"); date.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getExpenseDate())));
        TableColumn<Expense,String> cat = new TableColumn<>("Catégorie"); cat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        TableColumn<Expense,String> label = new TableColumn<>("Libellé"); label.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLabel()));
        TableColumn<Expense,String> vehicle = new TableColumn<>("Véhicule"); vehicle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleLabel()));
        TableColumn<Expense,Number> amount = new TableColumn<>("Montant"); amount.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getAmount()));
        TableColumn<Expense,String> notes = new TableColumn<>("Notes"); notes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes()));
        table.getColumns().addAll(date, cat, label, vehicle, amount, notes);
    }

    private void load() { table.setItems(repo.findAll(search.getText())); }

    private void openForm(Expense selected) {
        Dialog<Expense> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Nouvelle dépense" : "Modifier dépense");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        Map<Integer,String> vehicles = repo.vehicles();
        ComboBox<String> vehicle = new ComboBox<>(); vehicles.forEach((id,label) -> vehicle.getItems().add(id + " - " + label));
        if (selected != null) vehicles.forEach((id,label) -> { if (id == selected.getVehicleId()) vehicle.setValue(id + " - " + label); }); else if (!vehicle.getItems().isEmpty()) vehicle.getSelectionModel().selectFirst();
        DatePicker date = new DatePicker(selected == null ? LocalDate.now() : selected.getExpenseDate());
        ComboBox<String> category = new ComboBox<>(); category.getItems().addAll("CARBURANT", "ENTRETIEN", "ASSURANCE", "VISITE_TECHNIQUE", "BUREAU", "PUBLICITE", "AUTRE"); category.setValue(selected == null ? "ENTRETIEN" : selected.getCategory());
        TextField label = new TextField(selected == null ? "" : selected.getLabel());
        TextField amount = new TextField(selected == null ? "0" : String.valueOf(selected.getAmount()));
        TextArea notes = new TextArea(selected == null ? "" : selected.getNotes()); notes.setPrefRowCount(3);
        int r=0;
        grid.addRow(r++, new Label("Date"), date);
        grid.addRow(r++, new Label("Catégorie"), category);
        grid.addRow(r++, new Label("Libellé"), label);
        grid.addRow(r++, new Label("Véhicule"), vehicle);
        grid.addRow(r++, new Label("Montant"), amount);
        grid.addRow(r, new Label("Notes"), notes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) return new Expense(selected == null ? 0 : selected.getId(), extractId(vehicle.getValue()), "", date.getValue(), category.getValue(), label.getText(), parseDouble(amount.getText()), notes.getText());
            return null;
        });
        dialog.showAndWait().ifPresent(ex -> { try { repo.save(ex); load(); } catch(Exception err) { alert("Erreur dépense", err.getMessage()); } });
    }

    private void deleteSelected() {
        Expense ex = table.getSelectionModel().getSelectedItem(); if (ex == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette dépense ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) { try { repo.delete(ex.getId()); load(); } catch(Exception err) { alert("Erreur suppression", err.getMessage()); } } });
    }

    private int extractId(String value) { try { return Integer.parseInt(value.split(" - ")[0].trim()); } catch(Exception e) { return 0; } }
    private double parseDouble(String s) { try { return Double.parseDouble(s.trim().replace(',', '.')); } catch(Exception e) { return 0; } }
    private void alert(String title, String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
