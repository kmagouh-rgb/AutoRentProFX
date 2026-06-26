package com.autorentpro.ui;

import com.autorentpro.model.Vehicle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.io.File;

public class FleetView {
    private final VBox root = new VBox(16);
    private final VehicleRepository repo = new VehicleRepository();
    private final TilePane cards = new TilePane();
    private final TextField search = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> fuelFilter = new ComboBox<>();
    private final ComboBox<String> brandFilter = new ComboBox<>();
    private final ComboBox<String> transmissionFilter = new ComboBox<>();
    private final CheckBox withPhotoOnly = new CheckBox("Avec photo uniquement");
    private final Runnable openContractsAction;
    private final Runnable openAvailabilityAction;
    private final Runnable openDocumentsAction;
    private final Runnable openMaintenanceAction;

    public FleetView() {
        this(null, null, null, null);
    }

    public FleetView(Runnable openContractsAction, Runnable openAvailabilityAction, Runnable openDocumentsAction, Runnable openMaintenanceAction) {
        this.openContractsAction = openContractsAction;
        this.openAvailabilityAction = openAvailabilityAction;
        this.openDocumentsAction = openDocumentsAction;
        this.openMaintenanceAction = openMaintenanceAction;
        build();
        load();
    }

    public Parent getView() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(24));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(4);
        Label title = new Label("Fleet Cards Workflow");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Cartes véhicules avec photo, actions rapides et accès direct à la disponibilité, contrat, documents et maintenance.");
        subtitle.getStyleClass().add("muted");
        titles.getChildren().addAll(title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button refresh = new Button("↻ Actualiser");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> load());

        header.getChildren().addAll(titles, headerSpacer, refresh);

        HBox filters1 = new HBox(12);
        filters1.setAlignment(Pos.CENTER_LEFT);

        search.setPromptText("Rechercher: immatriculation, marque, modèle...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);

        statusFilter.getItems().addAll("Tous", "EN_SERVICE", "MAINTENANCE", "HORS_SERVICE", "VENDUE");
        statusFilter.setValue("Tous");
        statusFilter.getStyleClass().add("filter-combo");

        fuelFilter.getItems().addAll("Tous", "Diesel", "Essence", "Hybride", "Electrique");
        fuelFilter.setValue("Tous");
        fuelFilter.getStyleClass().add("filter-combo");

        filters1.getChildren().addAll(search, statusFilter, fuelFilter);

        HBox filters2 = new HBox(12);
        filters2.setAlignment(Pos.CENTER_LEFT);

        brandFilter.getItems().addAll("Toutes marques", "RENAULT", "DACIA", "PEUGEOT", "CITROEN", "FIAT", "SEAT", "JEEP");
        brandFilter.setValue("Toutes marques");
        brandFilter.getStyleClass().add("filter-combo");

        transmissionFilter.getItems().addAll("Toutes boîtes", "Manuelle", "Automatique", "");
        transmissionFilter.setValue("Toutes boîtes");
        transmissionFilter.getStyleClass().add("filter-combo");

        withPhotoOnly.getStyleClass().add("muted");

        filters2.getChildren().addAll(brandFilter, transmissionFilter, withPhotoOnly);

        search.textProperty().addListener((o, a, b) -> load());
        statusFilter.valueProperty().addListener((o, a, b) -> load());
        fuelFilter.valueProperty().addListener((o, a, b) -> load());
        brandFilter.valueProperty().addListener((o, a, b) -> load());
        transmissionFilter.valueProperty().addListener((o, a, b) -> load());
        withPhotoOnly.selectedProperty().addListener((o, a, b) -> load());

        cards.setHgap(20);
        cards.setVgap(20);
        cards.setPrefColumns(4);
        cards.getStyleClass().add("fleet-tile-pane");

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        root.getChildren().addAll(header, statsBar(), filters1, filters2, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
    }

    private HBox statsBar() {
        HBox stats = new HBox(14);
        stats.getStyleClass().add("fleet-stats-row");
        stats.getChildren().addAll(
                statCard("🚗", "EN SERVICE", "Exploitable", "stat-green"),
                statCard("🛠", "MAINTENANCE", "En entretien", "stat-orange"),
                statCard("⛔", "HORS SERVICE", "Non exploitable", "stat-red"),
                statCard("🖼", "PHOTOS", "Fleet cards visuelles", "stat-blue")
        );
        return stats;
    }

    private VBox statCard(String icon, String title, String subtitle, String styleClass) {
        VBox box = new VBox(5);
        box.getStyleClass().addAll("fleet-stat-card", styleClass);
        Label i = new Label(icon);
        i.getStyleClass().add("fleet-stat-icon");
        Label t = new Label(title);
        t.getStyleClass().add("fleet-stat-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("muted");
        box.getChildren().addAll(i, t, s);
        return box;
    }

    private void load() {
        cards.getChildren().clear();
        for (Vehicle v : repo.findAll(search.getText())) {
            if (!matchesFilters(v)) continue;
            cards.getChildren().add(vehicleCard(v));
        }
    }

    private boolean matchesFilters(Vehicle v) {
        String status = statusFilter.getValue();
        if (status != null && !status.equals("Tous") && !status.equalsIgnoreCase(nullToEmpty(v.getStatus()))) return false;

        String fuel = fuelFilter.getValue();
        if (fuel != null && !fuel.equals("Tous") && !fuel.equalsIgnoreCase(nullToEmpty(v.getFuel()))) return false;

        String brand = brandFilter.getValue();
        if (brand != null && !brand.equals("Toutes marques") && !brand.equalsIgnoreCase(nullToEmpty(v.getBrand()))) return false;

        String tr = transmissionFilter.getValue();
        if (tr != null && !tr.equals("Toutes boîtes") && !tr.equalsIgnoreCase(nullToEmpty(v.getTransmission()))) return false;

        if (withPhotoOnly.isSelected() && !hasValidPhoto(v)) return false;

        return true;
    }

    private VBox vehicleCard(Vehicle v) {
        VBox box = new VBox(0);
        box.getStyleClass().add("vehicle-pro-card");
        box.setPrefWidth(282);
        box.setMinHeight(425);

        StackPane photoArea = photoArea(v, 282, 178, true);

        VBox body = new VBox(9);
        body.getStyleClass().add("vehicle-card-body");
        body.setPadding(new Insets(14));

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label brandIcon = new Label(brandIcon(v.getBrand()));
        brandIcon.getStyleClass().add("brand-icon");
        VBox nameBox = new VBox(1);
        Label name = new Label((nullToEmpty(v.getBrand()) + " " + nullToEmpty(v.getModel())).trim());
        name.getStyleClass().add("vehicle-card-title");
        Label year = new Label(String.valueOf(v.getYear()));
        year.getStyleClass().add("muted");
        nameBox.getChildren().addAll(name, year);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        MenuButton menu = actionMenu(v);
        titleRow.getChildren().addAll(brandIcon, nameBox, spacer, menu);

        HBox plateRow = new HBox(8);
        plateRow.setAlignment(Pos.CENTER_LEFT);
        Label plate = new Label(nullToEmpty(v.getRegistration()));
        plate.getStyleClass().add("plate-badge");
        Label fuel = new Label(nullToEmpty(v.getFuel()));
        fuel.getStyleClass().add("mini-badge");
        plateRow.getChildren().addAll(plate, fuel);

        GridPane details = new GridPane();
        details.setHgap(10);
        details.setVgap(7);
        addMini(details, 0, 0, "KM", v.getMileage() + " km");
        addMini(details, 1, 0, "Boîte", emptyDash(v.getTransmission()));
        addMini(details, 0, 1, "Prix", String.format("%.2f DH/j", v.getDailyPrice()));
        addMini(details, 1, 1, "État", statusBadge(v.getStatus()));

        VBox occupation = new VBox(4);
        Label occText = new Label("Occupation annuelle  " + estimatedOccupation(v) + "%");
        occText.getStyleClass().add("muted");
        ProgressBar bar = new ProgressBar(estimatedOccupation(v) / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("occupation-bar");
        occupation.getChildren().addAll(occText, bar);

        Label availabilityHint = new Label("Disponibilité: calculée par Contrats + Réservations");
        availabilityHint.getStyleClass().add("availability-hint");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button fiche = new Button("Ouvrir fiche");
        fiche.getStyleClass().add("primary-button");
        fiche.setOnAction(e -> openVehicleDetails(v));
        Button dispo = new Button("Disponibilité");
        dispo.getStyleClass().add("secondary-button");
        dispo.setOnAction(e -> openAvailability(v));
        actions.getChildren().addAll(fiche, dispo);

        HBox workflow = new HBox(8);
        workflow.setAlignment(Pos.CENTER_LEFT);
        Button contrat = new Button("Nouveau contrat");
        contrat.getStyleClass().add("success-button");
        contrat.setOnAction(e -> openContractWorkflow(v));
        Button hist = new Button("Historique");
        hist.getStyleClass().add("secondary-button");
        hist.setOnAction(e -> openVehicleDetails(v));
        workflow.getChildren().addAll(contrat, hist);

        body.getChildren().addAll(titleRow, plateRow, details, occupation, availabilityHint, actions, workflow);
        box.getChildren().addAll(photoArea, body);

        box.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                openVehicleDetails(v);
            }
        });

        return box;
    }

    private MenuButton actionMenu(Vehicle v) {
        MenuButton mb = new MenuButton("⋮");
        mb.getStyleClass().add("card-menu-button");
        MenuItem fiche = new MenuItem("Voir fiche");
        fiche.setOnAction(e -> openVehicleDetails(v));
        MenuItem photo = new MenuItem("Agrandir photo");
        photo.setOnAction(e -> openPhotoPreview(v));
        MenuItem contrat = new MenuItem("Nouveau contrat");
        contrat.setOnAction(e -> openContractWorkflow(v));
        MenuItem dispo = new MenuItem("Disponibilité");
        dispo.setOnAction(e -> openAvailability(v));
        MenuItem docs = new MenuItem("Documents");
        docs.setOnAction(e -> openDocuments(v));
        MenuItem maintenance = new MenuItem("Maintenance");
        maintenance.setOnAction(e -> openMaintenance(v));
        mb.getItems().addAll(fiche, photo, new SeparatorMenuItem(), contrat, dispo, docs, maintenance);
        return mb;
    }

    private StackPane photoArea(Vehicle v, double width, double height, boolean clickable) {
        StackPane area = new StackPane();
        area.getStyleClass().add("vehicle-photo-area");
        area.setPrefHeight(height);
        area.setMinHeight(height);
        area.setPrefWidth(width);

        if (hasValidPhoto(v)) {
            try {
                File file = new File(nullToEmpty(v.getPhotoPath()).trim());
                Image image = new Image(file.toURI().toString(), width, height, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(width);
                imageView.setFitHeight(height);
                imageView.setPreserveRatio(false);
                area.getChildren().add(imageView);
            } catch (Exception ex) {
                area.getChildren().add(placeholder(v, width, height));
            }
        } else {
            area.getChildren().add(placeholder(v, width, height));
        }

        Label badge = new Label(statusBadge(v.getStatus()));
        badge.getStyleClass().add(statusClass(v.getStatus()));
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(10));
        area.getChildren().add(badge);

        Label zoom = new Label("🔍");
        zoom.getStyleClass().add("photo-zoom-badge");
        StackPane.setAlignment(zoom, Pos.TOP_RIGHT);
        StackPane.setMargin(zoom, new Insets(10));
        area.getChildren().add(zoom);

        if (clickable) {
            area.setOnMouseClicked(e -> openPhotoPreview(v));
        }

        return area;
    }

    private VBox placeholder(Vehicle v, double width, double height) {
        VBox p = new VBox(6);
        p.setAlignment(Pos.CENTER);
        p.getStyleClass().add("vehicle-photo-placeholder");
        p.setPrefWidth(width);
        p.setPrefHeight(height);
        Label icon = new Label("🚘");
        icon.getStyleClass().add("vehicle-photo-icon");
        Label text = new Label("Photo véhicule");
        text.getStyleClass().add("muted");
        Label small = new Label(nullToEmpty(v.getBrand()) + " " + nullToEmpty(v.getModel()));
        small.getStyleClass().add("muted");
        p.getChildren().addAll(icon, text, small);
        return p;
    }

    private void openPhotoPreview(Vehicle v) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Photo véhicule - " + nullToEmpty(v.getRegistration()));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(820);
        dialog.getDialogPane().setPrefHeight(560);

        VBox box = new VBox(12);
        box.setPadding(new Insets(18));
        box.setAlignment(Pos.CENTER);

        if (hasValidPhoto(v)) {
            File file = new File(nullToEmpty(v.getPhotoPath()).trim());
            Image image = new Image(file.toURI().toString(), 760, 460, true, true);
            ImageView img = new ImageView(image);
            img.setFitWidth(760);
            img.setFitHeight(460);
            img.setPreserveRatio(true);
            box.getChildren().add(img);
        } else {
            box.getChildren().add(placeholder(v, 760, 420));
        }

        Label title = new Label((nullToEmpty(v.getBrand()) + " " + nullToEmpty(v.getModel()) + "  •  " + nullToEmpty(v.getRegistration())).trim());
        title.getStyleClass().add("vehicle-card-title");
        box.getChildren().add(title);

        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private void openVehicleDetails(Vehicle v) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Fiche véhicule - " + nullToEmpty(v.getRegistration()));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(820);
        dialog.getDialogPane().setPrefHeight(590);

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(18));

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane photo = photoArea(v, 340, 210, false);
        photo.setPrefWidth(340);
        photo.setPrefHeight(210);
        photo.setMinWidth(340);
        photo.setMinHeight(210);

        VBox titleBox = new VBox(8);
        Label title = new Label((nullToEmpty(v.getBrand()) + " " + nullToEmpty(v.getModel())).trim());
        title.getStyleClass().add("page-title");
        Label plate = new Label(nullToEmpty(v.getRegistration()));
        plate.getStyleClass().add("plate-badge");
        Label status = new Label(statusBadge(v.getStatus()));
        status.getStyleClass().add(statusClass(v.getStatus()));
        Label availability = new Label("La disponibilité réelle dépend de la période choisie dans Disponibilité.");
        availability.getStyleClass().add("availability-hint");
        titleBox.getChildren().addAll(title, plate, status, availability);
        header.getChildren().addAll(photo, titleBox);
        pane.setTop(header);

        GridPane info = new GridPane();
        info.setHgap(18);
        info.setVgap(12);
        info.setPadding(new Insets(22, 0, 0, 0));
        addInfo(info, 0, 0, "Marque", v.getBrand());
        addInfo(info, 0, 1, "Modèle", v.getModel());
        addInfo(info, 0, 2, "Année", String.valueOf(v.getYear()));
        addInfo(info, 0, 3, "Carburant", v.getFuel());
        addInfo(info, 0, 4, "Photo", hasValidPhoto(v) ? "Oui" : "Non");
        addInfo(info, 1, 0, "Transmission", v.getTransmission());
        addInfo(info, 1, 1, "Kilométrage", v.getMileage() + " km");
        addInfo(info, 1, 2, "Prix / jour", String.format("%.2f DH", v.getDailyPrice()));
        addInfo(info, 1, 3, "État technique", statusBadge(v.getStatus()));
        addInfo(info, 1, 4, "Occupation estimée", estimatedOccupation(v) + "%");
        pane.setCenter(info);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button preview = new Button("Agrandir photo");
        preview.getStyleClass().add("secondary-button");
        preview.setOnAction(e -> openPhotoPreview(v));
        Button ok = new Button("Fermer");
        ok.getStyleClass().add("primary-button");
        ok.setOnAction(e -> dialog.close());
        actions.getChildren().addAll(preview, ok);
        pane.setBottom(actions);

        dialog.getDialogPane().setContent(pane);
        dialog.showAndWait();
    }

    private void addMini(GridPane grid, int col, int row, String label, String value) {
        VBox box = new VBox(2);
        box.getStyleClass().add("mini-info-box");
        Label l = new Label(label);
        l.getStyleClass().add("muted");
        Label v = new Label(nullToEmpty(value));
        v.getStyleClass().add("mini-info-value");
        box.getChildren().addAll(l, v);
        grid.add(box, col, row);
    }

    private void addInfo(GridPane grid, int col, int row, String label, String value) {
        VBox box = new VBox(3);
        Label l = new Label(label);
        l.getStyleClass().add("muted");
        Label v = new Label(nullToEmpty(value));
        v.getStyleClass().add("vehicle-card-title");
        box.getChildren().addAll(l, v);
        grid.add(box, col, row);
    }


    private void openContractWorkflow(Vehicle v) {
        Alert info = new Alert(Alert.AlertType.CONFIRMATION);
        info.setTitle("Nouveau contrat");
        info.setHeaderText("Créer un contrat pour " + nullToEmpty(v.getBrand()) + " " + nullToEmpty(v.getModel()));
        info.setContentText("1) Vérifiez la disponibilité par dates.\n2) Si la voiture est disponible, ouvrez Contrats pour créer le contrat.\n\nImmatriculation: " + nullToEmpty(v.getRegistration()));
        ButtonType dispo = new ButtonType("Disponibilité");
        ButtonType contracts = new ButtonType("Contrats");
        ButtonType cancel = ButtonType.CANCEL;
        info.getButtonTypes().setAll(dispo, contracts, cancel);
        info.showAndWait().ifPresent(bt -> {
            if (bt == dispo) openAvailability(v);
            if (bt == contracts && openContractsAction != null) openContractsAction.run();
        });
    }

    private void openAvailability(Vehicle v) {
        if (openAvailabilityAction != null) {
            openAvailabilityAction.run();
        } else {
            alert("Disponibilité", "Ouvrez le module Disponibilité et choisissez la période pour " + nullToEmpty(v.getRegistration()) + ".");
        }
    }

    private void openDocuments(Vehicle v) {
        if (openDocumentsAction != null) {
            openDocumentsAction.run();
        } else {
            alert("Documents", "Ouvrez le module Documents pour gérer les documents du véhicule " + nullToEmpty(v.getRegistration()) + ".");
        }
    }

    private void openMaintenance(Vehicle v) {
        if (openMaintenanceAction != null) {
            openMaintenanceAction.run();
        } else {
            alert("Maintenance", "Ouvrez le module Maintenance pour ajouter une intervention au véhicule " + nullToEmpty(v.getRegistration()) + ".");
        }
    }

    private void alert(String title, String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    private boolean hasValidPhoto(Vehicle v) {
        String path = nullToEmpty(v.getPhotoPath()).trim();
        return !path.isEmpty() && new File(path).exists();
    }

    private int estimatedOccupation(Vehicle v) {
        int base = Math.abs((nullToEmpty(v.getRegistration()) + v.getId()).hashCode());
        return 25 + (base % 70);
    }

    private String brandIcon(String brand) {
        String b = normalize(nullToEmpty(brand));
        if (b.contains("RENAULT")) return "R";
        if (b.contains("DACIA")) return "D";
        if (b.contains("PEUGEOT")) return "P";
        if (b.contains("CITROEN")) return "C";
        if (b.contains("FIAT")) return "F";
        if (b.contains("SEAT")) return "S";
        if (b.contains("JEEP")) return "J";
        return "A";
    }

    private String statusClass(String s) {
        String v = normalizeStatus(s);
        return switch (v) {
            case "EN_SERVICE" -> "status-disponible";
            case "MAINTENANCE" -> "status-maintenance";
            case "HORS_SERVICE" -> "status-louee";
            case "VENDUE" -> "status-vendue";
            default -> "pill";
        };
    }

    private String statusBadge(String s) {
        String v = normalizeStatus(s);
        return switch (v) {
            case "EN_SERVICE" -> "En service";
            case "MAINTENANCE" -> "Maintenance";
            case "HORS_SERVICE" -> "Hors service";
            case "VENDUE" -> "Vendue";
            default -> v;
        };
    }

    private String normalizeStatus(String s) {
        if (s == null || s.isBlank()) return "EN_SERVICE";
        String v = s.trim().toUpperCase();
        if (v.equals("DISPONIBLE") || v.equals("LOUEE") || v.equals("RESERVEE")) return "EN_SERVICE";
        return v;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private String emptyDash(String s) {
        String v = nullToEmpty(s);
        return v.isBlank() ? "—" : v;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
