package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class ReportsView {
    private final VBox root = new VBox(18);
    public ReportsView() { build(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(24));
        Label title = new Label("Rapports V4.9");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Synthèse rapide des revenus, dépenses et état du parc.");
        subtitle.getStyleClass().add("muted");

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        double ca = Db.sum("contracts", "total_amount", "1=1");
        double paid = Db.sum("contracts", "paid_amount", "1=1");
        double expenses = Db.sum("expenses", "amount", "1=1");
        double maintenance = Db.sum("maintenance", "amount", "1=1");
        grid.add(card("Chiffre contrats", ca, "Total des contrats créés"), 0, 0);
        grid.add(card("Montant encaissé", paid, "Paiements enregistrés"), 1, 0);
        grid.add(card("Dépenses", expenses, "Dépenses générales"), 2, 0);
        grid.add(card("Maintenance", maintenance, "Coûts maintenance"), 3, 0);
        grid.add(card("Solde net", paid - expenses - maintenance, "Encaissements - charges"), 0, 1);
        grid.add(textCard("Contrats", "Ouverts: " + Db.count("contracts", "status='ACTIVE'") + "\nFermés: " + Db.count("contracts", "status='FERME'") + "\nAnnulés: " + Db.count("contracts", "status='ANNULE'")), 1, 1);
        grid.add(textCard("Véhicules", "En service: " + Db.count("vehicles", "active=1 AND status='EN_SERVICE'") + "\nOccupées aujourd'hui: " + Db.count("contracts", "status='ACTIVE' AND CURDATE() BETWEEN start_date AND end_date") + "\nMaintenance: " + Db.count("vehicles", "active=1 AND status='MAINTENANCE'")), 2, 1);
        grid.add(textCard("Documents", "Expirés: " + Db.count("vehicle_documents", "expiry_date IS NOT NULL AND expiry_date < CURDATE()") + "\nBientôt expirés: " + Db.count("vehicle_documents", "expiry_date IS NOT NULL AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)") + "\nTotal docs: " + Db.count("vehicle_documents", "1=1")), 3, 1);
        ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(25); grid.getColumnConstraints().addAll(cc,cc,cc,cc);
        root.getChildren().addAll(title, subtitle, grid);
    }

    private VBox card(String title, double value, String subtitle) {
        VBox b = new VBox(8); b.setPadding(new Insets(18)); b.getStyleClass().add("stat-card");
        Label t = new Label(title); t.getStyleClass().add("stat-title");
        Label v = new Label(String.format("%.2f DH", value)); v.getStyleClass().add("stat-value");
        Label s = new Label(subtitle); s.getStyleClass().add("stat-subtitle");
        b.getChildren().addAll(t,v,s); return b;
    }

    private VBox textCard(String title, String text) {
        VBox b = new VBox(8); b.setPadding(new Insets(18)); b.getStyleClass().add("content-card");
        Label t = new Label(title); t.getStyleClass().add("section-title");
        Label v = new Label(text); v.getStyleClass().add("muted");
        b.getChildren().addAll(t,v); return b;
    }
}
