package com.autorentpro.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SettingsView {
    private final VBox root = new VBox(18);
    public SettingsView() { build(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(24));
        Label title = new Label("Paramètres");
        title.getStyleClass().add("page-title");
        VBox card = new VBox(12);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(20));
        TextField company = new TextField("AutoRent Pro");
        TextField phone = new TextField("0600000000");
        TextField city = new TextField("Imzouren");
        ComboBox<String> theme = new ComboBox<>(); theme.getItems().addAll("Light", "Dark (bientôt)", "Blue"); theme.setValue("Light");
        Button save = new Button("Enregistrer"); save.getStyleClass().add("primary-button");
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(12);
        grid.addRow(0, new Label("Société"), company);
        grid.addRow(1, new Label("Téléphone"), phone);
        grid.addRow(2, new Label("Ville"), city);
        grid.addRow(3, new Label("Thème"), theme);
        card.getChildren().addAll(new Label("Configuration société"), grid, save);
        root.getChildren().addAll(title, card);
    }
}
