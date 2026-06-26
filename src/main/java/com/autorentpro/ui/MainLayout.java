package com.autorentpro.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainLayout {
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final VBox sidebar = new VBox(8);
    private final String username;
    private boolean collapsed = false;

    public MainLayout(String username) {
        this.username = username;
        build();
        showDashboard();
    }

    public Parent getView() { return root; }

    private void build() {
        root.getStyleClass().add("app-root");
        sidebar.setPadding(new Insets(18));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(245);

        Label logo = new Label("🚗 AutoRent Pro");
        logo.getStyleClass().add("logo");
        Button dash = navButton("🏠 Dashboard");
        Button exploitation = navButton("🧩 Centre exploitation");
        Button vehicles = navButton("🚗 Véhicules");
        Button fleet = navButton("🧭 Fleet Cards");
        Button vehicle360 = navButton("🔎 Fiche 360°");
        Button clients = navButton("👤 Clients");
        Button planning = navButton("📅 Planning");
        Button availability = navButton("✅ Disponibilité");
        Button contracts = navButton("📄 Contrats");
        Button dossiers = navButton("🗂 Dossiers");
        Button maintenance = navButton("🛠 Maintenance");
        Button expenses = navButton("💸 Dépenses");
        Button documents = navButton("📁 Documents");
        Button reports = navButton("📊 Rapports");
        Button analytics = navButton("📈 Analyse");
        Button alerts = navButton("🔔 Alertes");
        Button tools = navButton("🧰 Outils");
        Button settings = navButton("⚙ Paramètres");

        dash.setOnAction(e -> showDashboard());
        exploitation.setOnAction(e -> showExploitation());
        vehicles.setOnAction(e -> showVehicles());
        fleet.setOnAction(e -> showFleet());
        vehicle360.setOnAction(e -> showVehicle360());
        clients.setOnAction(e -> showClients());
        planning.setOnAction(e -> showPlanning());
        availability.setOnAction(e -> showAvailability());
        contracts.setOnAction(e -> showContracts());
        dossiers.setOnAction(e -> showDossiers());
        maintenance.setOnAction(e -> showMaintenance());
        expenses.setOnAction(e -> showExpenses());
        documents.setOnAction(e -> showDocuments());
        reports.setOnAction(e -> showReports());
        analytics.setOnAction(e -> showAnalytics());
        alerts.setOnAction(e -> showAlerts());
        tools.setOnAction(e -> showTools());
        settings.setOnAction(e -> showSettings());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label user = new Label("Connecté: " + username);
        user.getStyleClass().add("sidebar-user");
        sidebar.getChildren().addAll(logo, dash, exploitation, vehicles, fleet, vehicle360, clients, planning, availability, contracts, dossiers, maintenance, expenses, documents, reports, analytics, alerts, tools, settings, spacer, user);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 22, 14, 22));
        header.getStyleClass().add("topbar");
        Button collapse = new Button("☰");
        collapse.getStyleClass().add("icon-button");
        collapse.setOnAction(e -> toggleSidebar());
        Label title = new Label("AutoRent Pro FX - V6.8 DOSSIER LOCATION");
        title.getStyleClass().add("top-title");
        TextField globalSearch = new TextField();
        globalSearch.setPromptText("Recherche générale: véhicule, client, contrat...");
        globalSearch.getStyleClass().add("global-search");
        globalSearch.setOnAction(e -> {
            String q = globalSearch.getText() == null ? "" : globalSearch.getText().trim();
            if (!q.isEmpty()) content.getChildren().setAll(new GlobalSearchView(q).getView());
        });
        HBox.setHgrow(globalSearch, Priority.ALWAYS);
        Label notif = new Label("🔔 " + new AlertsRepository().countAlerts() + " Alertes");
        notif.getStyleClass().add("pill");
        notif.setOnMouseClicked(e -> showAlerts());
        header.getChildren().addAll(collapse, title, globalSearch, notif);

        root.setLeft(sidebar);
        root.setTop(header);
        root.setCenter(content);
    }

    private Button navButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void toggleSidebar() {
        collapsed = !collapsed;
        sidebar.setPrefWidth(collapsed ? 74 : 245);
        for (javafx.scene.Node n : sidebar.getChildren()) {
            if (n instanceof Button b) b.setText(collapsed ? b.getText().substring(0, 2) : expandText(b.getText()));
            if (n instanceof Label l && !l.getStyleClass().contains("sidebar-user")) l.setText(collapsed ? "🚗" : "🚗 AutoRent Pro");
        }
    }

    private String expandText(String text) {
        if (text.startsWith("🏠")) return "🏠 Dashboard";
        if (text.startsWith("🧩")) return "🧩 Centre exploitation";
        if (text.startsWith("🚗")) return "🚗 Véhicules";
        if (text.startsWith("🧭")) return "🧭 Fleet Cards";
        if (text.startsWith("🔎")) return "🔎 Fiche 360°";
        if (text.startsWith("👤")) return "👤 Clients";
        if (text.startsWith("📅")) return "📅 Planning";
        if (text.startsWith("✅")) return "✅ Disponibilité";
        if (text.startsWith("📄")) return "📄 Contrats";
        if (text.startsWith("🗂")) return "🗂 Dossiers";
        if (text.startsWith("🛠")) return "🛠 Maintenance";
        if (text.startsWith("💸")) return "💸 Dépenses";
        if (text.startsWith("📁")) return "📁 Documents";
        if (text.startsWith("📊")) return "📊 Rapports";
        if (text.startsWith("📈")) return "📈 Analyse";
        if (text.startsWith("🔔")) return "🔔 Alertes";
        if (text.startsWith("🧰")) return "🧰 Outils";
        if (text.startsWith("⚙")) return "⚙ Paramètres";
        return text;
    }

    private void showDashboard() { content.getChildren().setAll(new DashboardView().getView()); }
    private void showExploitation() { content.getChildren().setAll(new ExploitationCenterView().getView()); }
    private void showVehicles() { content.getChildren().setAll(new VehiclesView().getView()); }
    private void showFleet() { content.getChildren().setAll(new FleetView(this::showContracts, this::showAvailability, this::showDocuments, this::showMaintenance).getView()); }
    private void showVehicle360() { content.getChildren().setAll(new Vehicle360View().getView()); }
    private void showClients() { content.getChildren().setAll(new CustomersView().getView()); }
    private void showPlanning() { content.getChildren().setAll(new PlanningView().getView()); }
    private void showAvailability() { content.getChildren().setAll(new AvailabilityView().getView()); }
    private void showContracts() { content.getChildren().setAll(new ContractsView().getView()); }
    private void showDossiers() { content.getChildren().setAll(new DossierLocationView().getView()); }
    private void showMaintenance() { content.getChildren().setAll(new MaintenanceView().getView()); }
    private void showExpenses() { content.getChildren().setAll(new ExpensesView().getView()); }
    private void showDocuments() { content.getChildren().setAll(new DocumentsView().getView()); }
    private void showReports() { content.getChildren().setAll(new ReportsView().getView()); }
    private void showAnalytics() { content.getChildren().setAll(new AnalyticsView().getView()); }
    private void showAlerts() { content.getChildren().setAll(new AlertsView().getView()); }
    private void showTools() { content.getChildren().setAll(new ExportCenterView().getView()); }
    private void showSettings() { content.getChildren().setAll(new SettingsView().getView()); }

    private void placeholder(String title, String msg) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(30));
        box.getStyleClass().add("content-card");
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        Label m = new Label(msg);
        m.getStyleClass().add("muted");
        box.getChildren().addAll(t, m);
        content.getChildren().setAll(box);
    }
}
