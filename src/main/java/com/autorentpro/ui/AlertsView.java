package com.autorentpro.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AlertsView {
    private final VBox root = new VBox(16);
    private final AlertsRepository repo = new AlertsRepository();

    public AlertsView() { build(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(24));
        Label title = new Label("Centre des alertes");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Documents proches de l'expiration, contrats à retourner et alertes de maintenance.");
        subtitle.getStyleClass().add("muted");

        TableView<String[]> table = new TableView<>();
        table.getStyleClass().add("data-table");
        TableColumn<String[], String> level = new TableColumn<>("Niveau");
        level.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(v.getValue()[0]));
        level.setPrefWidth(120);
        TableColumn<String[], String> type = new TableColumn<>("Type");
        type.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(v.getValue()[1]));
        type.setPrefWidth(140);
        TableColumn<String[], String> message = new TableColumn<>("Message");
        message.setCellValueFactory(v -> new javafx.beans.property.SimpleStringProperty(v.getValue()[2]));
        message.setPrefWidth(760);
        table.getColumns().addAll(level, type, message);

        ObservableList<String[]> alerts = repo.findAlerts();
        table.setItems(alerts);

        HBox toolbar = new HBox(10);
        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("primary-button");
        Label count = new Label(alerts.size() + " alerte(s)");
        count.getStyleClass().add("pill");
        refresh.setOnAction(e -> {
            ObservableList<String[]> newAlerts = repo.findAlerts();
            table.setItems(newAlerts);
            count.setText(newAlerts.size() + " alerte(s)");
        });
        toolbar.getChildren().addAll(refresh, count);

        root.getChildren().addAll(title, subtitle, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
    }
}
