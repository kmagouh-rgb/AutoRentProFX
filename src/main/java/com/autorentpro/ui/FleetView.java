package com.autorentpro.ui;

import com.autorentpro.model.Vehicle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FleetView {
    private final BorderPane root = new BorderPane();
    private final VehicleRepository repo = new VehicleRepository();
    private final TilePane cards = new TilePane();
    private final TextField search = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> fuelFilter = new ComboBox<>();
    private final ComboBox<String> brandFilter = new ComboBox<>();
    private final ComboBox<String> transmissionFilter = new ComboBox<>();
    private final CheckBox withPhotoOnly = new CheckBox("Avec photo uniquement");
    private final ComboBox<String> sortBox = new ComboBox<>();
    private final Label filteredCount = new Label("0 affichée(s)");
    private final PauseTransition filterDelay = new PauseTransition(Duration.millis(350));
    private volatile int loadVersion = 0;

    private final Label availableCount = new Label("0");
    private final Label occupiedCount = new Label("0");
    private final Label reservedCount = new Label("0");
    private final Label maintenanceCount = new Label("0");
    private final Label totalCount = new Label("0");

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
        root.getStyleClass().add("fleet-enterprise-root");

        VBox page = new VBox(18);
        page.setFillWidth(true);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titles = new VBox(4);
        Label title = new Label("Fleet Cards Enterprise");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue moderne du parc avec disponibilité calculée depuis contrats et réservations.");
        subtitle.getStyleClass().add("muted");
        titles.getChildren().addAll(title, subtitle);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button refresh = new Button("↻ Actualiser");
        refresh.getStyleClass().add("secondary-button");
        refresh.setOnAction(e -> load());
        Button availability = new Button("Disponibilité");
        availability.getStyleClass().add("primary-button");
        availability.setOnAction(e -> openAvailability(null));
        header.getChildren().addAll(titles, headerSpacer, availability, refresh);

        HBox stats = new HBox(14);
        stats.getStyleClass().add("fleet-stats-row");
        stats.getChildren().addAll(
                statCard("🚗", "Disponible maintenant", availableCount, "stat-green"),
                statCard("🔴", "Occupée", occupiedCount, "stat-red"),
                statCard("🟡", "Réservée", reservedCount, "stat-yellow"),
                statCard("🛠", "Maintenance / HS", maintenanceCount, "stat-orange"),
                statCard("📦", "Total véhicules", totalCount, "stat-blue")
        );

        HBox filters = new HBox(12);
        filters.setAlignment(Pos.CENTER_LEFT);
        search.setPromptText("Rechercher: immatriculation, marque, modèle...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);
        statusFilter.getItems().addAll("Tous", "DISPONIBLE", "OCCUPEE", "RESERVEE", "MAINTENANCE", "HORS_SERVICE", "VENDUE");
        statusFilter.setValue("Tous");
        statusFilter.getStyleClass().add("filter-combo");
        fuelFilter.getItems().addAll("Tous", "Diesel", "Essence", "Hybride", "Electrique");
        fuelFilter.setValue("Tous");
        fuelFilter.getStyleClass().add("filter-combo");
        brandFilter.getItems().addAll("Toutes marques", "RENAULT", "DACIA", "PEUGEOT", "CITROEN", "FIAT", "SEAT", "JEEP", "BMW", "VOLKSWAGEN");
        brandFilter.setValue("Toutes marques");
        brandFilter.getStyleClass().add("filter-combo");
        transmissionFilter.getItems().addAll("Toutes boîtes", "Manuelle", "Automatique", "");
        transmissionFilter.setValue("Toutes boîtes");
        transmissionFilter.getStyleClass().add("filter-combo");
        withPhotoOnly.getStyleClass().add("muted");
        sortBox.getItems().addAll("Tri: plus récent", "Marque A-Z", "Immatriculation", "Prix croissant", "Prix décroissant", "Kilométrage");
        sortBox.setValue("Tri: plus récent");
        sortBox.getStyleClass().add("filter-combo");
        filteredCount.getStyleClass().add("pill");
        Button resetFilters = new Button("Réinitialiser");
        resetFilters.getStyleClass().add("secondary-button");
        resetFilters.setOnAction(e -> {
            search.clear();
            statusFilter.setValue("Tous");
            brandFilter.setValue("Toutes marques");
            fuelFilter.setValue("Tous");
            transmissionFilter.setValue("Toutes boîtes");
            withPhotoOnly.setSelected(false);
            sortBox.setValue("Tri: plus récent");
            load();
        });
        filters.getChildren().addAll(search, statusFilter, brandFilter, fuelFilter, transmissionFilter, sortBox, withPhotoOnly, resetFilters, filteredCount);

        search.textProperty().addListener((o, a, b) -> delayedLoad());
        statusFilter.valueProperty().addListener((o, a, b) -> delayedLoad());
        fuelFilter.valueProperty().addListener((o, a, b) -> delayedLoad());
        brandFilter.valueProperty().addListener((o, a, b) -> delayedLoad());
        transmissionFilter.valueProperty().addListener((o, a, b) -> delayedLoad());
        sortBox.valueProperty().addListener((o, a, b) -> delayedLoad());
        withPhotoOnly.selectedProperty().addListener((o, a, b) -> delayedLoad());

        cards.setHgap(20);
        cards.setVgap(20);
        cards.setPrefColumns(4);
        cards.getStyleClass().add("fleet-tile-pane");

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        page.getChildren().addAll(header, stats, filters, scroll);
        root.setCenter(page);
    }

    private VBox statCard(String icon, String title, Label value, String styleClass) {
        VBox box = new VBox(5);
        box.getStyleClass().addAll("fleet-stat-card", styleClass);
        Label i = new Label(icon);
        i.getStyleClass().add("fleet-stat-icon");
        Label t = new Label(title);
        t.getStyleClass().add("fleet-stat-title");
        value.getStyleClass().add("fleet-stat-value");
        box.getChildren().addAll(i, value, t);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private void delayedLoad() {
        filterDelay.setOnFinished(e -> load());
        filterDelay.playFromStart();
    }

    private void load() {
        int version = ++loadVersion;
        cards.getChildren().setAll(loadingBox());
        filteredCount.setText("Chargement...");

        String searchText = search.getText();
        String statusValue = statusFilter.getValue();
        String fuelValue = fuelFilter.getValue();
        String brandValue = brandFilter.getValue();
        String transmissionValue = transmissionFilter.getValue();
        boolean photoOnly = withPhotoOnly.isSelected();
        String sortValue = sortBox.getValue();

        Task<LoadResult> task = new Task<>() {
            @Override
            protected LoadResult call() {
                List<Vehicle> all = new ArrayList<>(repo.findAll(searchText));
                List<CardData> visible = new ArrayList<>();
                int available = 0;
                int occupied = 0;
                int reserved = 0;
                int maintenance = 0;
                int total = 0;

                for (Vehicle v : all) {
                    VehicleRepository.VehicleUsage usage = repo.currentUsage(v.getId());
                    String state = effectiveState(v, usage);

                    if ("DISPONIBLE".equals(state)) available++;
                    else if ("OCCUPEE".equals(state)) occupied++;
                    else if ("RESERVEE".equals(state)) reserved++;
                    else maintenance++;
                    total++;

                    if (!matchesFiltersSnapshot(v, state, statusValue, fuelValue, brandValue, transmissionValue, photoOnly)) continue;
                    visible.add(new CardData(v, usage, state));
                }

                sortVisibleSnapshot(visible, sortValue);
                return new LoadResult(visible, available, occupied, reserved, maintenance, total);
            }
        };

        task.setOnSucceeded(e -> {
            if (version != loadVersion) return;
            LoadResult result = task.getValue();
            cards.getChildren().clear();

            int limit = Math.min(result.visible.size(), 300);
            for (int i = 0; i < limit; i++) {
                CardData data = result.visible.get(i);
                cards.getChildren().add(vehicleCard(data.vehicle, data.usage, data.state));
            }

            if (result.visible.size() > limit) {
                Label more = new Label("Affichage limité à 300 véhicules pour garder l'interface rapide. Utilisez la recherche/filtre pour réduire la liste.");
                more.getStyleClass().add("muted");
                VBox box = new VBox(8, more);
                box.getStyleClass().add("content-card");
                box.setPadding(new Insets(20));
                cards.getChildren().add(box);
            }

            if (result.visible.isEmpty()) {
                VBox empty = new VBox(8);
                empty.getStyleClass().add("content-card");
                empty.setPadding(new Insets(28));
                Label title = new Label("Aucun véhicule ne correspond aux filtres.");
                title.getStyleClass().add("vehicle-card-title");
                Label hint = new Label("Essayez de réinitialiser les filtres ou de changer la recherche.");
                hint.getStyleClass().add("muted");
                empty.getChildren().addAll(title, hint);
                cards.getChildren().add(empty);
            }

            availableCount.setText(String.valueOf(result.available));
            occupiedCount.setText(String.valueOf(result.occupied));
            reservedCount.setText(String.valueOf(result.reserved));
            maintenanceCount.setText(String.valueOf(result.maintenance));
            totalCount.setText(String.valueOf(result.total));
            filteredCount.setText(result.visible.size() + " affichée(s)");
        });

        task.setOnFailed(e -> {
            if (version != loadVersion) return;
            Throwable ex = task.getException();
            cards.getChildren().setAll(errorBox(ex == null ? "Erreur inconnue" : ex.getMessage()));
            filteredCount.setText("Erreur");
        });

        Thread th = new Thread(task, "fleet-loader");
        th.setDaemon(true);
        th.start();
    }

    private VBox loadingBox() {
        VBox box = new VBox(10);
        box.getStyleClass().add("content-card");
        box.setPadding(new Insets(28));
        Label title = new Label("Chargement des véhicules...");
        title.getStyleClass().add("vehicle-card-title");
        ProgressIndicator pi = new ProgressIndicator();
        pi.setMaxSize(42, 42);
        box.getChildren().addAll(title, pi);
        return box;
    }

    private VBox errorBox(String message) {
        VBox box = new VBox(10);
        box.getStyleClass().add("content-card");
        box.setPadding(new Insets(28));
        Label title = new Label("Erreur Fleet Cards");
        title.getStyleClass().add("vehicle-card-title");
        Label msg = new Label(message == null ? "" : message);
        msg.getStyleClass().add("muted");
        msg.setWrapText(true);
        box.getChildren().addAll(title, msg);
        return box;
    }

    private boolean matchesFiltersSnapshot(Vehicle v, String state, String status, String fuel, String brand, String tr, boolean photoOnly) {
        if (status != null && !status.equals("Tous") && !status.equalsIgnoreCase(state)) return false;
        if (fuel != null && !fuel.equals("Tous") && !fuel.equalsIgnoreCase(nullToEmpty(v.getFuel()))) return false;
        if (brand != null && !brand.equals("Toutes marques") && !brand.equalsIgnoreCase(nullToEmpty(v.getBrand()))) return false;
        if (tr != null && !tr.equals("Toutes boîtes") && !tr.equalsIgnoreCase(nullToEmpty(v.getTransmission()))) return false;
        return !photoOnly || hasValidPhoto(v);
    }

    private void sortVisibleSnapshot(List<CardData> visible, String sort) {
        Comparator<CardData> cmp;
        if (sort != null && sort.contains("Marque")) {
            cmp = Comparator.comparing(d -> nullToEmpty(d.vehicle.getBrand()) + " " + nullToEmpty(d.vehicle.getModel()), String.CASE_INSENSITIVE_ORDER);
        } else if (sort != null && sort.contains("Immatriculation")) {
            cmp = Comparator.comparing(d -> nullToEmpty(d.vehicle.getRegistration()), String.CASE_INSENSITIVE_ORDER);
        } else if (sort != null && sort.contains("Prix croissant")) {
            cmp = Comparator.comparingDouble(d -> d.vehicle.getDailyPrice());
        } else if (sort != null && sort.contains("Prix décroissant")) {
            cmp = Comparator.comparingDouble((CardData d) -> d.vehicle.getDailyPrice()).reversed();
        } else if (sort != null && sort.contains("Kilométrage")) {
            cmp = Comparator.comparingInt(d -> d.vehicle.getMileage());
        } else {
            cmp = Comparator.comparingInt((CardData d) -> d.vehicle.getId()).reversed();
        }
        visible.sort(cmp);
    }

    private static class LoadResult {
        final List<CardData> visible;
        final int available;
        final int occupied;
        final int reserved;
        final int maintenance;
        final int total;

        LoadResult(List<CardData> visible, int available, int occupied, int reserved, int maintenance, int total) {
            this.visible = visible;
            this.available = available;
            this.occupied = occupied;
            this.reserved = reserved;
            this.maintenance = maintenance;
            this.total = total;
        }
    }

    private void sortVisible(List<CardData> visible) {
        String sort = sortBox.getValue() == null ? "" : sortBox.getValue();
        Comparator<CardData> cmp;
        if (sort.contains("Marque")) {
            cmp = Comparator.comparing(d -> nullToEmpty(d.vehicle.getBrand()) + " " + nullToEmpty(d.vehicle.getModel()), String.CASE_INSENSITIVE_ORDER);
        } else if (sort.contains("Immatriculation")) {
            cmp = Comparator.comparing(d -> nullToEmpty(d.vehicle.getRegistration()), String.CASE_INSENSITIVE_ORDER);
        } else if (sort.contains("Prix croissant")) {
            cmp = Comparator.comparingDouble(d -> d.vehicle.getDailyPrice());
        } else if (sort.contains("Prix décroissant")) {
            cmp = Comparator.comparingDouble((CardData d) -> d.vehicle.getDailyPrice()).reversed();
        } else if (sort.contains("Kilométrage")) {
            cmp = Comparator.comparingInt(d -> d.vehicle.getMileage());
        } else {
            cmp = Comparator.comparingInt((CardData d) -> d.vehicle.getId()).reversed();
        }
        visible.sort(cmp);
    }

    private boolean matchesFilters(Vehicle v, String state) {
        String status = statusFilter.getValue();
        if (status != null && !status.equals("Tous") && !status.equalsIgnoreCase(state)) return false;

        String fuel = fuelFilter.getValue();
        if (fuel != null && !fuel.equals("Tous") && !fuel.equalsIgnoreCase(nullToEmpty(v.getFuel()))) return false;

        String brand = brandFilter.getValue();
        if (brand != null && !brand.equals("Toutes marques") && !brand.equalsIgnoreCase(nullToEmpty(v.getBrand()))) return false;

        String tr = transmissionFilter.getValue();
        if (tr != null && !tr.equals("Toutes boîtes") && !tr.equalsIgnoreCase(nullToEmpty(v.getTransmission()))) return false;

        return !withPhotoOnly.isSelected() || hasValidPhoto(v);
    }

    private VBox vehicleCard(Vehicle v, VehicleRepository.VehicleUsage usage, String state) {
        VBox box = new VBox(0);
        box.getStyleClass().addAll("vehicle-pro-card", "vehicle-state-" + state.toLowerCase());
        box.setPrefWidth(282);
        box.setMinHeight(430);

        StackPane photoArea = photoArea(v, 282, 176, state, true);

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
        Label year = new Label(v.getYear() + "  •  " + emptyDash(v.getFuel()) + "  •  " + emptyDash(v.getTransmission()));
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
        Label price = new Label(String.format("%.2f DH/j", v.getDailyPrice()));
        price.getStyleClass().add("mini-badge");
        plateRow.getChildren().addAll(plate, price);

        HBox kmRow = new HBox(12);
        kmRow.setAlignment(Pos.CENTER_LEFT);
        Label km = new Label("⌁ " + v.getMileage() + " km");
        km.getStyleClass().add("muted");
        Label tech = new Label("État technique: " + technicalStatus(v));
        tech.getStyleClass().add("muted");
        kmRow.getChildren().addAll(km, tech);

        VBox dynamic = dynamicBlock(v, usage, state);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        if ("DISPONIBLE".equals(state)) {
            Button contrat = new Button("Nouveau contrat");
            contrat.getStyleClass().add("success-button");
            contrat.setOnAction(e -> openContractWorkflow(v));
            Button dispo = new Button("Disponibilité");
            dispo.getStyleClass().add("secondary-button");
            dispo.setOnAction(e -> openAvailability(v));
            actions.getChildren().addAll(contrat, dispo);
        } else if ("OCCUPEE".equals(state) || "RESERVEE".equals(state)) {
            Button voir = new Button("Voir contrat");
            voir.getStyleClass().add("primary-button");
            voir.setOnAction(e -> openContracts());
            Button fiche = new Button("Fiche 360°");
            fiche.getStyleClass().add("secondary-button");
            fiche.setOnAction(e -> openVehicleDetails(v, usage, state));
            actions.getChildren().addAll(voir, fiche);
        } else {
            Button fiche = new Button("Voir fiche");
            fiche.getStyleClass().add("primary-button");
            fiche.setOnAction(e -> openVehicleDetails(v, usage, state));
            Button maintenanceBtn = new Button("Maintenance");
            maintenanceBtn.getStyleClass().add("secondary-button");
            maintenanceBtn.setOnAction(e -> openMaintenance(v));
            actions.getChildren().addAll(fiche, maintenanceBtn);
        }

        body.getChildren().addAll(titleRow, plateRow, kmRow, dynamic, actions);
        box.getChildren().addAll(photoArea, body);
        box.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                openVehicleDetails(v, usage, state);
            }
        });
        return box;
    }

    private VBox dynamicBlock(Vehicle v, VehicleRepository.VehicleUsage usage, String state) {
        VBox box = new VBox(5);
        box.getStyleClass().add("fleet-dynamic-block");
        Label title = new Label();
        title.getStyleClass().add("fleet-dynamic-title");
        Label line1 = new Label();
        line1.getStyleClass().add("muted");
        Label line2 = new Label();
        line2.getStyleClass().add("muted");

        if ("OCCUPEE".equals(state)) {
            title.setText("Contrat en cours");
            line1.setText("Client: " + emptyDash(usage.customer));
            line2.setText("Retour: " + emptyDate(usage.date) + "  •  " + remainingText(usage.date));
        } else if ("RESERVEE".equals(state)) {
            title.setText("Réservée aujourd'hui");
            line1.setText("Client: " + emptyDash(usage.customer));
            line2.setText("Fin réservation: " + emptyDate(usage.date));
        } else if ("MAINTENANCE".equals(state) || "HORS_SERVICE".equals(state) || "VENDUE".equals(state)) {
            title.setText(statusLabel(state));
            line1.setText("Cette voiture n'est pas exploitable pour la location.");
            line2.setText("Gérez-la depuis Maintenance / Véhicules.");
        } else {
            title.setText("Disponible maintenant");
            if ("PROCHAINE_RESERVATION".equalsIgnoreCase(usage.status)) {
                line1.setText("Prochaine réservation: " + emptyDate(usage.date));
                line2.setText("Client: " + emptyDash(usage.customer));
            } else {
                line1.setText("Aucun contrat actif aujourd'hui.");
                line2.setText("Vous pouvez créer une nouvelle location.");
            }
        }
        box.getChildren().addAll(title, line1, line2);
        return box;
    }

    private MenuButton actionMenu(Vehicle v) {
        MenuButton mb = new MenuButton("⋮");
        mb.getStyleClass().add("card-menu-button");
        MenuItem fiche = new MenuItem("Voir fiche");
        fiche.setOnAction(e -> openVehicleDetails(v, repo.currentUsage(v.getId()), effectiveState(v, repo.currentUsage(v.getId()))));
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

    private StackPane photoArea(Vehicle v, double width, double height, String state, boolean clickable) {
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

        Label badge = new Label(statusLabel(state));
        badge.getStyleClass().add(statusClass(state));
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

    private void openVehicleDetails(Vehicle v, VehicleRepository.VehicleUsage usage, String state) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Fiche 360° véhicule - " + nullToEmpty(v.getRegistration()));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(860);
        dialog.getDialogPane().setPrefHeight(620);

        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(18));
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane photo = photoArea(v, 340, 210, state, false);
        VBox titleBox = new VBox(8);
        Label title = new Label((nullToEmpty(v.getBrand()) + " " + nullToEmpty(v.getModel())).trim());
        title.getStyleClass().add("page-title");
        Label plate = new Label(nullToEmpty(v.getRegistration()));
        plate.getStyleClass().add("plate-badge");
        Label status = new Label(statusLabel(state));
        status.getStyleClass().add(statusClass(state));
        Label availability = new Label(dynamicSummary(usage, state));
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
        addInfo(info, 0, 4, "Transmission", v.getTransmission());
        addInfo(info, 1, 0, "Kilométrage", v.getMileage() + " km");
        addInfo(info, 1, 1, "Prix / jour", String.format("%.2f DH", v.getDailyPrice()));
        addInfo(info, 1, 2, "État technique", technicalStatus(v));
        addInfo(info, 1, 3, "Disponibilité actuelle", statusLabel(state));
        addInfo(info, 1, 4, "Contrat/Réservation", emptyDash(usage.reference));
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
        info.setContentText("La disponibilité finale sera vérifiée dans Contrats PRO selon la période choisie.\n\nImmatriculation: " + nullToEmpty(v.getRegistration()));
        ButtonType contracts = new ButtonType("Ouvrir Contrats PRO");
        ButtonType dispo = new ButtonType("Disponibilité");
        info.getButtonTypes().setAll(contracts, dispo, ButtonType.CANCEL);
        info.showAndWait().ifPresent(bt -> {
            if (bt == contracts) openContracts();
            if (bt == dispo) openAvailability(v);
        });
    }

    private void openContracts() {
        if (openContractsAction != null) openContractsAction.run();
    }

    private void openAvailability(Vehicle v) {
        if (openAvailabilityAction != null) openAvailabilityAction.run();
        else alert("Disponibilité", "Ouvrez le module Disponibilité pour choisir la période.");
    }

    private void openDocuments(Vehicle v) {
        if (openDocumentsAction != null) openDocumentsAction.run();
        else alert("Documents", "Ouvrez le module Documents pour gérer " + nullToEmpty(v.getRegistration()) + ".");
    }

    private void openMaintenance(Vehicle v) {
        if (openMaintenanceAction != null) openMaintenanceAction.run();
        else alert("Maintenance", "Ouvrez le module Maintenance pour " + nullToEmpty(v.getRegistration()) + ".");
    }

    private void alert(String title, String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    private boolean hasValidPhoto(Vehicle v) {
        String path = nullToEmpty(v.getPhotoPath()).trim();
        return !path.isEmpty() && new File(path).exists();
    }

    private String effectiveState(Vehicle v, VehicleRepository.VehicleUsage usage) {
        String tech = normalizeStatus(v.getStatus());
        if ("MAINTENANCE".equals(tech) || "HORS_SERVICE".equals(tech) || "VENDUE".equals(tech)) return tech;
        String u = usage == null ? "DISPONIBLE" : normalizeStatus(usage.state);
        if ("OCCUPEE".equals(u) || "RESERVEE".equals(u)) return u;
        return "DISPONIBLE";
    }

    private String technicalStatus(Vehicle v) {
        return switch (normalizeStatus(v.getStatus())) {
            case "MAINTENANCE" -> "Maintenance";
            case "HORS_SERVICE" -> "Hors service";
            case "VENDUE" -> "Vendue";
            default -> "En service";
        };
    }

    private String statusClass(String s) {
        return switch (normalizeStatus(s)) {
            case "DISPONIBLE" -> "status-disponible";
            case "OCCUPEE" -> "status-louee";
            case "RESERVEE" -> "status-reservee";
            case "MAINTENANCE" -> "status-maintenance";
            case "HORS_SERVICE" -> "status-hors-service";
            case "VENDUE" -> "status-vendue";
            default -> "pill";
        };
    }

    private String statusLabel(String s) {
        return switch (normalizeStatus(s)) {
            case "DISPONIBLE" -> "Disponible";
            case "OCCUPEE" -> "Occupée";
            case "RESERVEE" -> "Réservée";
            case "MAINTENANCE" -> "Maintenance";
            case "HORS_SERVICE" -> "Hors service";
            case "VENDUE" -> "Vendue";
            default -> s == null ? "" : s;
        };
    }

    private String dynamicSummary(VehicleRepository.VehicleUsage usage, String state) {
        if (usage == null) return statusLabel(state);
        if ("OCCUPEE".equals(state)) return "Contrat " + emptyDash(usage.reference) + " / retour " + emptyDate(usage.date);
        if ("RESERVEE".equals(state)) return "Réservation " + emptyDash(usage.reference) + " / fin " + emptyDate(usage.date);
        if ("DISPONIBLE".equals(state) && "PROCHAINE_RESERVATION".equalsIgnoreCase(usage.status)) return "Prochaine réservation le " + emptyDate(usage.date);
        return statusLabel(state);
    }

    private String remainingText(LocalDate date) {
        if (date == null) return "";
        long d = ChronoUnit.DAYS.between(LocalDate.now(), date);
        if (d < 0) return "En retard";
        if (d == 0) return "Retour aujourd'hui";
        if (d == 1) return "Retour demain";
        return "Retour dans " + d + " jours";
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
        if (b.contains("BMW")) return "B";
        if (b.contains("VOLKSWAGEN")) return "V";
        return "A";
    }

    private String normalizeStatus(String s) {
        if (s == null || s.isBlank()) return "DISPONIBLE";
        String v = s.trim().toUpperCase();
        if (v.equals("EN_SERVICE")) return "DISPONIBLE";
        if (v.equals("DISPONIBLE")) return "DISPONIBLE";
        if (v.equals("LOUEE") || v.equals("LOUÉE") || v.equals("OCCUPE") || v.equals("OCCUPÉE")) return "OCCUPEE";
        if (v.equals("RESERVE") || v.equals("RESERVEE") || v.equals("RÉSERVÉE")) return "RESERVEE";
        return v;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private String emptyDash(String s) {
        String v = nullToEmpty(s);
        return v.isBlank() ? "—" : v;
    }

    private String emptyDate(LocalDate d) {
        return d == null ? "—" : d.toString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static class CardData {
        final Vehicle vehicle;
        final VehicleRepository.VehicleUsage usage;
        final String state;

        CardData(Vehicle vehicle, VehicleRepository.VehicleUsage usage, String state) {
            this.vehicle = vehicle;
            this.usage = usage;
            this.state = state;
        }
    }
}

