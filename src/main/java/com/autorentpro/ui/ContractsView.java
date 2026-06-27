package com.autorentpro.ui;

import com.autorentpro.model.Contract;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.io.File;
import java.io.IOException;
import java.awt.Desktop;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javafx.stage.FileChooser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class ContractsView {
    private final BorderPane root = new BorderPane();
    private final ContractsRepository repo = new ContractsRepository();
    private final TableView<Contract> table = new TableView<>();
    private final TextField search = new TextField();

    public ContractsView() { build(); load(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(22));
        VBox top = new VBox(12);
        Label title = new Label("Contrats de location");
        title.getStyleClass().add("page-title");
        Label note = new Label("Workflow contrat: Réservé → Confirmé → Véhicule livré → En cours → Retour aujourd’hui → Clôturé. La disponibilité reste calculée par dates.");
        note.getStyleClass().add("muted");
        HBox actions = new HBox(10);
        search.setPromptText("Recherche: contrat, voiture, client, statut...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);
        Button add = new Button("+ Nouveau contrat"); add.getStyleClass().add("primary-button");
        Button edit = new Button("Modifier"); edit.getStyleClass().add("secondary-button");
        Button pay = new Button("Paiement"); pay.getStyleClass().add("secondary-button");
        Button close = new Button("Fermer contrat"); close.getStyleClass().add("secondary-button");
        Button cancel = new Button("Annuler"); cancel.getStyleClass().add("danger-button");
        Button print = new Button("Contrat PDF officiel"); print.getStyleClass().add("secondary-button");
        actions.getChildren().addAll(search, add, edit, pay, close, cancel, print);
        top.getChildren().addAll(title, note, actions);
        root.setTop(top);
        setupTable();
        root.setCenter(table);

        search.textProperty().addListener((obs,o,n) -> load());
        add.setOnAction(e -> openForm(null));
        edit.setOnAction(e -> { Contract c = table.getSelectionModel().getSelectedItem(); if (c != null) openForm(c); });
        pay.setOnAction(e -> addPayment());
        close.setOnAction(e -> closeSelected());
        cancel.setOnAction(e -> cancelSelected());
        print.setOnAction(e -> exportSelectedPdf());
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Contract,String> num = new TableColumn<>("N° Contrat"); num.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getContractNumber()));
        TableColumn<Contract,String> veh = new TableColumn<>("Véhicule"); veh.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getVehicleLabel()));
        TableColumn<Contract,String> cli = new TableColumn<>("Client"); cli.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerLabel()));
        TableColumn<Contract,String> dates = new TableColumn<>("Période"); dates.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartDate()+" → "+c.getValue().getEndDate()));
        TableColumn<Contract,Number> total = new TableColumn<>("Total"); total.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getTotalAmount()));
        TableColumn<Contract,Number> paid = new TableColumn<>("Payé"); paid.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getPaidAmount()));
        TableColumn<Contract,Number> rest = new TableColumn<>("Reste"); rest.setCellValueFactory(c -> new SimpleDoubleProperty(c.getValue().getRestAmount()));
        TableColumn<Contract,String> status = new TableColumn<>("Statut");
        status.setCellValueFactory(c -> new SimpleStringProperty(displayStatus(c.getValue().getStatus())));
        status.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("status-reserve", "status-active", "status-closed", "status-cancelled");
                if (!empty && item != null) {
                    String n = item.toUpperCase();
                    if (n.contains("RÉSERV") || n.contains("CONFIRM")) getStyleClass().add("status-reserve");
                    else if (n.contains("COURS") || n.contains("LIVR") || n.contains("RETOUR")) getStyleClass().add("status-active");
                    else if (n.contains("CLÔT") || n.contains("FERM")) getStyleClass().add("status-closed");
                    else if (n.contains("ANNUL")) getStyleClass().add("status-cancelled");
                }
            }
        });
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Contract item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("contract-row-reserve", "contract-row-active", "contract-row-closed", "contract-row-cancelled");
                if (!empty && item != null) {
                    String n = normalizeStatus(item.getStatus());
                    if (n.equals("RESERVE") || n.equals("CONFIRME")) getStyleClass().add("contract-row-reserve");
                    else if (n.equals("VEHICULE_LIVRE") || n.equals("EN_COURS") || n.equals("RETOUR_AUJOURD_HUI") || n.equals("ACTIVE")) getStyleClass().add("contract-row-active");
                    else if (n.equals("CLOTURE") || n.equals("FERME")) getStyleClass().add("contract-row-closed");
                    else if (n.equals("ANNULE")) getStyleClass().add("contract-row-cancelled");
                }
            }
        });
        table.getColumns().addAll(num, veh, cli, dates, total, paid, rest, status);
    }

    private void load() { table.setItems(repo.findAll(search.getText())); }

    private void openForm(Contract selected) {
        Dialog<Contract> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Nouveau contrat PRO" : "Modifier contrat");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(980);
        dialog.getDialogPane().setPrefHeight(720);

        DatePicker start = new DatePicker(selected == null ? LocalDate.now() : selected.getStartDate());
        DatePicker end = new DatePicker(selected == null ? LocalDate.now().plusDays(1) : selected.getEndDate());

        Map<Integer,String> customers = repo.customers();
        ComboBox<String> customer = new ComboBox<>();
        customer.setMaxWidth(Double.MAX_VALUE);
        customers.forEach((id,label)-> customer.getItems().add(id + " - " + label));
        if (selected != null) {
            customers.forEach((id,label)-> { if (id == selected.getCustomerId()) customer.setValue(id + " - " + label); });
        } else if (!customer.getItems().isEmpty()) {
            customer.setValue(customer.getItems().get(0));
        }

        ListView<String> availableVehicles = new ListView<>();
        availableVehicles.setPrefHeight(210);
        availableVehicles.getStyleClass().add("data-table");

        Label availabilityInfo = new Label("Choisissez les dates puis cliquez sur Rechercher voitures disponibles.");
        availabilityInfo.getStyleClass().add("muted");

        Button searchVehicles = new Button("🔍 Rechercher voitures disponibles");
        searchVehicles.getStyleClass().add("primary-button");

        TextField price = new TextField(selected == null ? "0" : String.valueOf(selected.getDailyPrice()));
        TextField paid = new TextField(selected == null ? "0" : String.valueOf(selected.getPaidAmount()));
        TextField caution = new TextField("0");
        TextField discount = new TextField("0");

        ComboBox<String> status = new ComboBox<>();
        status.getItems().addAll("RESERVE", "CONFIRME", "VEHICULE_LIVRE", "EN_COURS", "RETOUR_AUJOURD_HUI", "CLOTURE", "ANNULE");
        status.setValue(selected == null ? "RESERVE" : normalizeStatus(selected.getStatus()));

        Label totalPreview = new Label();
        totalPreview.getStyleClass().add("pill");

        Runnable calc = () -> {
            double daily = parseDouble(price.getText());
            double remise = parseDouble(discount.getText());
            double total = Math.max(0, ContractsRepository.days(start.getValue(), end.getValue()) * daily - remise);
            totalPreview.setText("Jours: " + ContractsRepository.days(start.getValue(), end.getValue()) + "   |   Total: " + total + " DH   |   Payé: " + parseDouble(paid.getText()) + " DH   |   Reste: " + Math.max(0, total - parseDouble(paid.getText())) + " DH");
        };
        price.textProperty().addListener((o,a,b)-> calc.run());
        paid.textProperty().addListener((o,a,b)-> calc.run());
        discount.textProperty().addListener((o,a,b)-> calc.run());
        start.valueProperty().addListener((o,a,b)-> calc.run());
        end.valueProperty().addListener((o,a,b)-> calc.run());
        calc.run();

        Runnable refreshAvailable = () -> {
            availableVehicles.getItems().clear();
            try {
                Map<Integer,String> vehicles = repo.availableVehicles(start.getValue(), end.getValue(), selected == null ? 0 : selected.getId());
                vehicles.forEach((id,label)-> availableVehicles.getItems().add(id + " - " + label));
                if (selected != null) {
                    // En modification, garder le véhicule actuel visible même s'il n'apparaît pas dans la liste filtrée
                    boolean found = false;
                    for (String item : availableVehicles.getItems()) {
                        if (extractId(item) == selected.getVehicleId()) {
                            availableVehicles.getSelectionModel().select(item);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        availableVehicles.getItems().add(0, selected.getVehicleId() + " - " + selected.getVehicleLabel());
                        availableVehicles.getSelectionModel().select(0);
                    }
                } else if (!availableVehicles.getItems().isEmpty()) {
                    availableVehicles.getSelectionModel().select(0);
                }
                availabilityInfo.setText(vehicles.size() + " voiture(s) disponible(s) pour la période " + start.getValue() + " → " + end.getValue());
            } catch (Exception ex) {
                availabilityInfo.setText("Erreur disponibilité: " + ex.getMessage());
            }
        };
        searchVehicles.setOnAction(e -> refreshAvailable.run());
        refreshAvailable.run();

        TabPane tabs = new TabPane();
        tabs.getTabs().forEach(t -> t.setClosable(false));

        GridPane datesTab = formGrid();
        datesTab.addRow(0, new Label("Date départ"), start);
        datesTab.addRow(1, new Label("Date retour"), end);
        datesTab.addRow(2, new Label("Disponibilité"), new VBox(8, searchVehicles, availabilityInfo));
        Tab tDates = new Tab("1. Dates", datesTab);

        VBox vehicleBox = new VBox(10, new Label("Voitures disponibles pour la période choisie"), availableVehicles);
        vehicleBox.setPadding(new Insets(20));
        Tab tVehicle = new Tab("2. Véhicule disponible", vehicleBox);

        GridPane clientTab = formGrid();
        clientTab.addRow(0, new Label("Client"), customer);
        Label clientHint = new Label("Le client doit être créé dans Gestion des clients avant la création du contrat.");
        clientHint.getStyleClass().add("muted");
        clientTab.add(clientHint, 1, 1);
        Tab tClient = new Tab("3. Client", clientTab);

        GridPane priceTab = formGrid();
        priceTab.addRow(0, new Label("Prix / jour"), price);
        priceTab.addRow(1, new Label("Remise"), discount);
        priceTab.addRow(2, new Label("Payé"), paid);
        priceTab.addRow(3, new Label("Caution"), caution);
        priceTab.addRow(4, new Label("Statut"), status);
        priceTab.add(totalPreview, 1, 5);
        Tab tPrice = new Tab("4. Prix / Paiement", priceTab);

        VBox actionsBox = new VBox(12);
        actionsBox.setPadding(new Insets(20));
        Label actionsTitle = new Label("Actions rapides du contrat");
        actionsTitle.getStyleClass().add("page-title");
        Label actionsText = new Label("Après enregistrement: PDF, Paiement, Départ, Retour, Clôture se font depuis la liste des contrats / Dossiers.");
        actionsText.getStyleClass().add("muted");
        actionsBox.getChildren().addAll(actionsTitle, actionsText);
        Tab tActions = new Tab("5. Actions", actionsBox);

        tabs.getTabs().addAll(tDates, tVehicle, tClient, tPrice, tActions);
        tabs.getTabs().forEach(t -> t.setClosable(false));

        dialog.getDialogPane().setContent(tabs);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String selectedVehicle = availableVehicles.getSelectionModel().getSelectedItem();
                int vehicleId = extractId(selectedVehicle);
                int customerId = extractId(customer.getValue());
                double daily = parseDouble(price.getText());
                double remise = parseDouble(discount.getText());
                double total = Math.max(0, ContractsRepository.days(start.getValue(), end.getValue()) * daily - remise);
                return new Contract(selected == null ? 0 : selected.getId(), selected == null ? null : selected.getContractNumber(), vehicleId, customerId, "", "", start.getValue(), end.getValue(), daily, total, parseDouble(paid.getText()), status.getValue());
            }
            return null;
        });
        dialog.showAndWait().ifPresent(c -> { try { repo.save(c); load(); } catch(Exception ex) { alert("Erreur sauvegarde", ex.getMessage()); } });
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPrefWidth(170);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);
        return grid;
    }

    private void addPayment() {
        Contract c = table.getSelectionModel().getSelectedItem();
        if (c == null) return;
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Ajouter paiement - " + c.getContractNumber());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
        TextField amount = new TextField(String.valueOf(Math.max(c.getRestAmount(),0)));
        ComboBox<String> method = new ComboBox<>(); method.getItems().addAll("ESPECES", "VIREMENT", "CARTE", "CHEQUE"); method.setValue("ESPECES");
        TextField notes = new TextField();
        grid.addRow(0, new Label("Montant"), amount); grid.addRow(1, new Label("Méthode"), method); grid.addRow(2, new Label("Note"), notes);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? parseDouble(amount.getText()) : null);
        dialog.showAndWait().ifPresent(v -> { try { repo.addPayment(c.getId(), v, method.getValue(), notes.getText()); load(); } catch(Exception ex) { alert("Erreur paiement", ex.getMessage()); } });
    }

    private void closeSelected() {
        Contract c = table.getSelectionModel().getSelectedItem(); if (c == null) return;
        try { repo.closeContract(c); load(); } catch(Exception ex) { alert("Erreur fermeture", ex.getMessage()); }
    }

    private void cancelSelected() {
        Contract c = table.getSelectionModel().getSelectedItem(); if (c == null) return;
        try { repo.cancel(c.getId()); load(); } catch(Exception ex) { alert("Erreur annulation", ex.getMessage()); }
    }


    private void exportSelectedPdf() {
        Contract c = table.getSelectionModel().getSelectedItem();
        if (c == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le contrat PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName((c.getContractNumber() == null ? "contrat" : c.getContractNumber()) + ".pdf");
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;
        try {
            generateOfficialContractPdf(file, c);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
            new Alert(Alert.AlertType.INFORMATION, "Contrat PDF généré avec succès.", ButtonType.OK).showAndWait();
        } catch (Exception ex) {
            alert("Erreur PDF", ex.getMessage());
        }
    }

    private void generateOfficialContractPdf(File file, Contract c) throws IOException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 34;
                float w = page.getMediaBox().getWidth();

                // Header société, inspiré du modèle scanné fourni par l'utilisateur.
                text(cs, PDType1Font.HELVETICA_BOLD, 25, 92, 785, "Sté Aharchi car");
                text(cs, PDType1Font.HELVETICA_BOLD, 10, 96, 770, "Location De Voitures");
                text(cs, PDType1Font.HELVETICA_BOLD, 13, 372, 790, "+212 672 30 55 84");
                text(cs, PDType1Font.HELVETICA_BOLD, 13, 372, 772, "+212 671 71 77 66");
                text(cs, PDType1Font.HELVETICA_BOLD, 12, 372, 754, "aharchi.car@gmail.com");
                line(cs, 74, 812, 220, 812, 1.6f);
                line(cs, 72, 807, 104, 792, 1.6f);
                line(cs, 142, 804, 278, 804, 1.6f);

                box(cs, margin, 715, w - margin * 2, 32);
                centered(cs, PDType1Font.TIMES_BOLD, 23, "CONTRAT DE LOCATION", 715, 32);

                // Bloc véhicule
                box(cs, margin, 585, w - margin * 2, 118);
                text(cs, PDType1Font.TIMES_BOLD, 13, margin + 10, 690, "VEHICULE :");
                text(cs, PDType1Font.TIMES_ROMAN, 11, margin + 10, 662, "Marque : " + safe(c.getVehicleLabel()));
                text(cs, PDType1Font.TIMES_ROMAN, 11, margin + 220, 662, "Carburant : ................................");
                text(cs, PDType1Font.TIMES_ROMAN, 11, margin + 405, 662, "Immatriculation : " + vehicleRegistration(c.getVehicleLabel()));
                text(cs, PDType1Font.TIMES_ROMAN, 11, margin + 10, 624, "Date de Départ : " + fmt(c.getStartDate(), df));
                text(cs, PDType1Font.TIMES_ROMAN, 11, margin + 290, 624, "Date de Retour : " + fmt(c.getEndDate(), df));
                text(cs, PDType1Font.TIMES_ROMAN, 11, margin + 10, 599, "Nombre de Jour : " + ContractsRepository.days(c.getStartDate(), c.getEndDate()));
                text(cs, PDType1Font.TIMES_ROMAN, 11, margin + 290, 599, "Prolongation : .............");

                // Conducteurs
                float colW = (w - margin * 2 - 8) / 2;
                box(cs, margin, 360, colW, 212);
                box(cs, margin + colW + 8, 360, colW, 212);
                conducteurBlock(cs, margin + 10, 552, "CONDUCTEUR : (1)", c.getCustomerLabel(), "", "", "", "", "");
                conducteurBlock(cs, margin + colW + 18, 552, "CONDUCTEUR : (2)", "", "", "", "", "", "");

                // Départ véhicule + observations
                box(cs, margin, 188, colW, 158);
                box(cs, margin + colW + 8, 188, colW, 158);
                text(cs, PDType1Font.TIMES_BOLD, 13, margin + 10, 330, "DEPART DE VEHICULE :");
                text(cs, PDType1Font.TIMES_ROMAN, 10, margin + 10, 300, "Niveau de carburant :");
                fuelGauge(cs, margin + 95, 230);
                text(cs, PDType1Font.TIMES_ROMAN, 10, margin + 10, 205, "Kilométrage départ : ........................");

                text(cs, PDType1Font.TIMES_BOLD, 13, margin + colW + 18, 330, "OBSERVATIONS :");
                text(cs, PDType1Font.TIMES_ROMAN, 9, margin + colW + 22, 300, "- La voiture a été délivrée nettoyée et en parfait état pour");
                text(cs, PDType1Font.TIMES_ROMAN, 9, margin + colW + 22, 286, "  rouler et doit être restituée dans le meilleur état qu’elle a été livrée.");
                for (int i = 0; i < 7; i++) {
                    dottedLine(cs, margin + colW + 22, 268 - i * 14, w - margin - 12, 268 - i * 14);
                }

                // Bas de page
                box(cs, margin, 68, 165, 108);
                text(cs, PDType1Font.TIMES_ROMAN, 7.2f, margin + 8, 160, "J’ai lu et acquiescé les conditions de contrat");
                text(cs, PDType1Font.TIMES_ROMAN, 7.2f, margin + 8, 148, "mentionnées au verso de la page. J’assume");
                text(cs, PDType1Font.TIMES_ROMAN, 7.2f, margin + 8, 136, "ma responsabilité en tant que locataire du");
                text(cs, PDType1Font.TIMES_ROMAN, 7.2f, margin + 8, 124, "véhicule pour la durée de ce contrat.");
                text(cs, PDType1Font.TIMES_ROMAN, 7.2f, margin + 8, 112, "Je suis d’accord de payer toutes les amendes");
                text(cs, PDType1Font.TIMES_ROMAN, 7.2f, margin + 8, 100, "dues au non-respect du code de la route.");

                box(cs, margin + 175, 68, 178, 108);
                centeredInBox(cs, PDType1Font.TIMES_BOLD, 10, "Signature du locataire", margin + 175, 160, 178);
                line(cs, margin + 175, 148, margin + 353, 148, 0.6f);
                line(cs, margin + 264, 148, margin + 264, 122, 0.6f);
                text(cs, PDType1Font.TIMES_BOLD, 8, margin + 198, 136, "Date &heure Départ");
                text(cs, PDType1Font.TIMES_BOLD, 8, margin + 282, 136, "Date &heure Retour");
                line(cs, margin + 175, 122, margin + 353, 122, 0.6f);
                text(cs, PDType1Font.TIMES_ROMAN, 9, margin + 185, 104, "Signature :");

                box(cs, margin + 363, 68, w - margin - (margin + 363), 108);
                centeredInBox(cs, PDType1Font.TIMES_BOLD, 10, "Signature et Cachet du loueur", margin + 363, 160, w - margin - (margin + 363));

                text(cs, PDType1Font.TIMES_ROMAN, 7, margin, 48, "(1). Le conducteur est la personne physique ou morale au nom de laquelle est établi le contrat. Dans le cas d’une personne physique le Locataire est le payeur et");
                text(cs, PDType1Font.TIMES_ROMAN, 7, margin, 36, "le conducteur principal. Dans le cas d’une personne morale (exemples : société, association, etc.), alors le conducteur principal est le signataire du contrat.");
            }

            PDPage conditionsPage = new PDPage(PDRectangle.A4);
            doc.addPage(conditionsPage);
            try (PDPageContentStream cs2 = new PDPageContentStream(doc, conditionsPage)) {
                float margin = 42;
                float y = 790;
                text(cs2, PDType1Font.HELVETICA_BOLD, 18, margin, y, "CONDITIONS GENERALES DE LOCATION");
                y -= 28;
                text(cs2, PDType1Font.HELVETICA, 10, margin, y, "Contrat: " + safe(c.getContractNumber()));
                y -= 26;

                String[] lines = new String[] {
                    "1. Le locataire reconnait avoir recu le vehicule en bon etat de fonctionnement et de proprete.",
                    "2. Le vehicule doit etre restitue a la date et a l'heure prevues au contrat.",
                    "3. Tout retard peut entrainer une facturation supplementaire selon le tarif journalier applique.",
                    "4. Le locataire est responsable des infractions, amendes et dommages causes pendant la periode de location.",
                    "5. Le carburant doit etre restitue au meme niveau qu'au depart, sauf accord contraire.",
                    "6. Les frais de nettoyage, carburant, dommages ou kilometres supplementaires peuvent etre retenus sur la caution.",
                    "7. Le vehicule ne peut etre conduit que par les conducteurs declares dans le contrat.",
                    "8. Toute prolongation doit etre confirmee par l'agence avant l'expiration du contrat.",
                    "9. En cas d'accident ou de panne, le locataire doit informer immediatement l'agence.",
                    "10. La signature du contrat vaut acceptation des presentes conditions."
                };

                for (String lineText : lines) {
                    text(cs2, PDType1Font.HELVETICA, 10, margin, y, lineText);
                    y -= 24;
                }

                y -= 30;
                box(cs2, margin, 110, 230, 90);
                centeredInBox(cs2, PDType1Font.HELVETICA_BOLD, 11, "Signature du locataire", margin, 180, 230);
                text(cs2, PDType1Font.HELVETICA, 9, margin + 14, 135, "Lu et approuve");

                box(cs2, 325, 110, 230, 90);
                centeredInBox(cs2, PDType1Font.HELVETICA_BOLD, 11, "Cachet et signature agence", 325, 180, 230);

                text(cs2, PDType1Font.HELVETICA, 8, margin, 62, "Document genere par AutoRent Pro FX - Contrat PDF professionnel.");
            }

            doc.save(file);
        }
    }

    private void conducteurBlock(PDPageContentStream cs, float x, float y, String title, String name, String birth, String address, String cin, String permis, String passport) throws IOException {
        text(cs, PDType1Font.TIMES_BOLD, 13, x, y, title);
        text(cs, PDType1Font.TIMES_ROMAN, 10, x, y - 32, "Nom & prénom : " + safe(name));
        text(cs, PDType1Font.TIMES_ROMAN, 10, x, y - 56, "Date et lieu de naissance : " + safe(birth) + "        à");
        text(cs, PDType1Font.TIMES_ROMAN, 10, x, y - 80, "Adresse : " + safe(address));
        text(cs, PDType1Font.TIMES_ROMAN, 10, x, y - 104, "Tél : ");
        text(cs, PDType1Font.TIMES_ROMAN, 10, x, y - 130, "CIN N° : " + safe(cin) + "        Valable jusqu’au :");
        text(cs, PDType1Font.TIMES_ROMAN, 10, x, y - 150, "PC N° : " + safe(permis) + "        Délivrée le :        à");
        text(cs, PDType1Font.TIMES_ROMAN, 10, x, y - 174, "Passeport N° : " + safe(passport) + "        N° entrée :");
    }

    private void fuelGauge(PDPageContentStream cs, float x, float y) throws IOException {
        cs.setLineWidth(1f);
        cs.addRect(x, y, 78, 50);
        // Approximation simple de jauge carburant imprimable
        cs.moveTo(x + 6, y + 10); cs.curveTo(x + 12, y + 47, x + 62, y + 55, x + 74, y + 20); cs.stroke();
        line(cs, x + 13, y + 24, x + 25, y + 21, 1.2f);
        line(cs, x + 24, y + 39, x + 31, y + 30, 1.2f);
        line(cs, x + 43, y + 44, x + 43, y + 33, 1.2f);
        line(cs, x + 62, y + 37, x + 55, y + 29, 1.2f);
        text(cs, PDType1Font.HELVETICA, 8, x + 14, y + 15, "0");
        text(cs, PDType1Font.HELVETICA, 8, x + 35, y + 34, "1/2");
        text(cs, PDType1Font.HELVETICA, 8, x + 61, y + 25, "F");
    }

    private String vehicleRegistration(String label) {
        if (label == null) return "";
        int idx = label.indexOf(" - ");
        return idx > 0 ? label.substring(0, idx) : label;
    }

    private String fmt(LocalDate d, DateTimeFormatter df) { return d == null ? "" : df.format(d); }
    private String safe(String s) { return s == null ? "" : s; }

    private void text(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String value) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(pdfText(value));
        cs.endText();
    }

    private String pdfText(String value) {
        if (value == null) return "";
        return value.replace('’', '\'')
                .replace('“', '"').replace('”', '"')
                .replace('–', '-').replace('—', '-')
                .replace(' ', ' ');
    }

    private void centered(PDPageContentStream cs, PDType1Font font, float size, String value, float boxY, float boxH) throws IOException {
        float pageW = PDRectangle.A4.getWidth();
        float textW = font.getStringWidth(value) / 1000f * size;
        text(cs, font, size, (pageW - textW) / 2f, boxY + 9, value);
    }

    private void centeredInBox(PDPageContentStream cs, PDType1Font font, float size, String value, float x, float y, float boxW) throws IOException {
        float textW = font.getStringWidth(value) / 1000f * size;
        text(cs, font, size, x + (boxW - textW) / 2f, y, value);
    }

    private void box(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.setLineWidth(1.2f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private void line(PDPageContentStream cs, float x1, float y1, float x2, float y2, float lw) throws IOException {
        cs.setLineWidth(lw);
        cs.moveTo(x1, y1); cs.lineTo(x2, y2); cs.stroke();
    }

    private void dottedLine(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.setLineWidth(0.7f);
        cs.setLineDashPattern(new float[]{2, 3}, 0);
        cs.moveTo(x1, y1); cs.lineTo(x2, y2); cs.stroke();
        cs.setLineDashPattern(new float[]{}, 0);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "RESERVE";
        String s = status.trim().toUpperCase();
        if (s.equals("ACTIVE") || s.equals("OPEN")) return "EN_COURS";
        if (s.equals("FERME")) return "CLOTURE";
        if (s.equals("RESERVED")) return "RESERVE";
        return s;
    }

    private String displayStatus(String status) {
        return switch (normalizeStatus(status)) {
            case "RESERVE" -> "Réservé";
            case "CONFIRME" -> "Confirmé";
            case "VEHICULE_LIVRE" -> "Véhicule livré";
            case "EN_COURS" -> "En cours";
            case "RETOUR_AUJOURD_HUI" -> "Retour aujourd’hui";
            case "CLOTURE" -> "Clôturé";
            case "ANNULE" -> "Annulé";
            default -> status == null ? "" : status;
        };
    }

    private int extractId(String value) {
        if (value == null || value.isBlank()) return 0;
        try { return Integer.parseInt(value.split(" - ")[0].trim()); } catch(Exception e) { return 0; }
    }
    private double parseDouble(String s) { try { return Double.parseDouble(s.trim().replace(',', '.')); } catch(Exception e) { return 0; } }
    private void alert(String title, String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
