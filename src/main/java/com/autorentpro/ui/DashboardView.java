package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class DashboardView {
    private final VBox root = new VBox(18);
    public DashboardView() { build(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(24));
        Label title = new Label("Dashboard financier & opérationnel");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue rapide sur l'activité de l'agence, la finance, les documents et le parc automobile.");
        subtitle.getStyleClass().add("muted");
        GridPane cards = new GridPane();
        cards.setHgap(16);
        cards.setVgap(16);
        cards.add(card("🚗 Total véhicules", String.valueOf(Db.count("vehicles", "active=1")), "Tous les véhicules"), 0, 0);
        cards.add(card("📅 Occupées aujourd'hui", String.valueOf(Db.count("contracts", "status='ACTIVE' AND CURDATE() BETWEEN start_date AND end_date")), "Selon contrats"), 1, 0);
        cards.add(card("🛠 Maintenance", String.valueOf(Db.count("vehicles", "active=1 AND status='MAINTENANCE'")), "Situation technique"), 2, 0);
        cards.add(card("⛔ Hors service", String.valueOf(Db.count("vehicles", "active=1 AND status='HORS_SERVICE'")), "Non louables"), 3, 0);
        cards.add(card("👤 Clients", String.valueOf(Db.count("customers", "active=1")), "Clients actifs"), 0, 1);
        cards.add(card("📅 Réservations", String.valueOf(Db.count("reservations", "status='ACTIVE'")), "Réservations actives"), 1, 1);
        cards.add(card("📄 Contrats", String.valueOf(Db.count("contracts", "status='ACTIVE'")), "Contrats ouverts"), 2, 1);
        cards.add(card("💰 CA contrats", String.format("%.2f DH", Db.sum("contracts", "total_amount", "1=1")), "Total enregistré"), 3, 1);
        cards.add(card("💸 Dépenses", String.format("%.2f DH", Db.sum("expenses", "amount", "1=1") + Db.sum("maintenance", "amount", "1=1")), "Dépenses + maintenance"), 0, 2);
        cards.add(card("📈 Solde", String.format("%.2f DH", Db.sum("contracts", "paid_amount", "1=1") - Db.sum("expenses", "amount", "1=1") - Db.sum("maintenance", "amount", "1=1")), "Payé - dépenses"), 1, 2);
        cards.add(card("🛠 Opérations", String.valueOf(Db.count("maintenance", "1=1")), "Maintenances enregistrées"), 2, 2);
        cards.add(card("📄 Contrats fermés", String.valueOf(Db.count("contracts", "status=\'FERME\'")), "Historique clôturé"), 3, 2);
        cards.add(card("📁 Docs bientôt expirés", String.valueOf(Db.count("vehicle_documents", "expiry_date IS NOT NULL AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)")), "30 prochains jours"), 0, 3);
        cards.add(card("⚠ Docs expirés", String.valueOf(Db.count("vehicle_documents", "expiry_date IS NOT NULL AND expiry_date < CURDATE()")), "À régulariser"), 1, 3);
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(25);
        cards.getColumnConstraints().addAll(c, c, c, c);

        HBox panels = new HBox(16);
        VBox alerts = panel("Alertes rapides", "• Vérifier les assurances proches de l'expiration.\n• La disponibilité se vérifie par dates dans le module Disponibilité.\n• Suivre les contrats ouverts et les retours prévus.");
        VBox activities = panel("Activités récentes", "• Connexion administrateur.\n• Version V5.6: disponibilité retirée de Gestion véhicules.\n• Modules maintenance et dépenses ajoutés.");
        HBox.setHgrow(alerts, Priority.ALWAYS); HBox.setHgrow(activities, Priority.ALWAYS);
        panels.getChildren().addAll(alerts, activities);
        root.getChildren().addAll(title, subtitle, cards, panels);
    }

    private VBox card(String title, String value, String subtitle) {
        VBox box = new VBox(7);
        box.getStyleClass().add("stat-card");
        box.setPadding(new Insets(18));
        Label t = new Label(title); t.getStyleClass().add("stat-title");
        Label v = new Label(value); v.getStyleClass().add("stat-value");
        Label s = new Label(subtitle); s.getStyleClass().add("stat-subtitle");
        box.getChildren().addAll(t, v, s);
        return box;
    }

    private VBox panel(String title, String text) {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setPadding(new Insets(18));
        panel.getStyleClass().add("content-card");
        Label ptitle = new Label(title); ptitle.getStyleClass().add("section-title");
        Label body = new Label(text); body.getStyleClass().add("muted"); body.setWrapText(true);
        panel.getChildren().addAll(ptitle, body);
        return panel;
    }
}
