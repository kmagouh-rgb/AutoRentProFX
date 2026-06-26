package com.autorentpro.ui;

import com.autorentpro.model.SearchResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class GlobalSearchView {
    private final BorderPane root = new BorderPane();
    private final TableView<SearchResult> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final GlobalSearchRepository repo = new GlobalSearchRepository();

    public GlobalSearchView(String keyword) {
        build();
        searchField.setText(keyword == null ? "" : keyword);
        load();
    }

    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(24));
        VBox top = new VBox(12);
        Label title = new Label("Recherche globale");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Cherche rapidement dans les véhicules, clients et contrats.");
        subtitle.getStyleClass().add("muted");
        HBox bar = new HBox(10);
        searchField.setPromptText("Immatriculation, client, téléphone, contrat, état...");
        searchField.getStyleClass().add("search-field");
        Button btn = new Button("Rechercher");
        btn.getStyleClass().add("primary-button");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        bar.getChildren().addAll(searchField, btn);
        top.getChildren().addAll(title, subtitle, bar);
        root.setTop(top);

        TableColumn<SearchResult, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        TableColumn<SearchResult, String> main = new TableColumn<>("Résultat");
        main.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        TableColumn<SearchResult, String> details = new TableColumn<>("Détails");
        details.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSubtitle()));
        TableColumn<SearchResult, String> status = new TableColumn<>("Statut");
        status.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        table.getColumns().addAll(type, main, details, status);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("data-table");
        VBox center = new VBox(table);
        center.setPadding(new Insets(18, 0, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(center);

        btn.setOnAction(e -> load());
        searchField.setOnAction(e -> load());
    }

    private void load() {
        table.setItems(repo.search(searchField.getText()));
    }
}
