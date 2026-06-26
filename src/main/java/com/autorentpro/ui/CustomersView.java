package com.autorentpro.ui;

import com.autorentpro.model.Customer;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class CustomersView {
    private final BorderPane root = new BorderPane();
    private final CustomerRepository repo = new CustomerRepository();
    private final TableView<Customer> table = new TableView<>();
    private final TextField search = new TextField();

    public CustomersView() {
        build();
        load();
    }

    public Parent getView() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(22));

        VBox top = new VBox(12);

        Label title = new Label("Gestion des clients");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Fiche complète client: identité, permis, passeport, urgence et documents.");
        subtitle.getStyleClass().add("muted");

        HBox actions = new HBox(10);

        search.setPromptText("Recherche: nom, CIN, téléphone, permis, passeport, ville...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);

        Button add = new Button("+ Nouveau client");
        add.getStyleClass().add("primary-button");

        Button edit = new Button("Modifier fiche");
        edit.getStyleClass().add("secondary-button");

        Button del = new Button("Supprimer");
        del.getStyleClass().add("danger-button");

        actions.getChildren().addAll(search, add, edit, del);
        top.getChildren().addAll(title, subtitle, actions);
        root.setTop(top);

        setupTable();

        VBox center = new VBox(table);
        center.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(table, Priority.ALWAYS);
        root.setCenter(center);

        search.textProperty().addListener((obs, old, val) -> load());

        add.setOnAction(e -> openForm(null));

        edit.setOnAction(e -> {
            Customer c = table.getSelectionModel().getSelectedItem();
            if (c != null) openForm(c);
        });

        table.setRowFactory(tv -> {
            TableRow<Customer> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openForm(row.getItem());
                }
            });
            return row;
        });

        del.setOnAction(e -> deleteSelected());
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Customer, String> name = new TableColumn<>("Nom et prénom");
        name.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));

        TableColumn<Customer, String> cin = new TableColumn<>("CIN");
        cin.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCin()));

        TableColumn<Customer, String> phone = new TableColumn<>("Téléphone");
        phone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));

        TableColumn<Customer, String> license = new TableColumn<>("Permis");
        license.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDrivingLicense()));

        TableColumn<Customer, String> licenseExp = new TableColumn<>("Exp. permis");
        licenseExp.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLicenseExpiry()));

        TableColumn<Customer, String> passport = new TableColumn<>("Passeport");
        passport.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPassportNumber()));

        TableColumn<Customer, String> city = new TableColumn<>("Ville");
        city.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCity()));

        table.getColumns().setAll(name, cin, phone, license, licenseExp, passport, city);
    }

    private void load() {
        table.setItems(repo.findAll(search.getText()));
    }

    private void openForm(Customer selected) {
        Dialog<Customer> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Nouveau client" : "Modifier fiche client");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(900);
        dialog.getDialogPane().setPrefHeight(680);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Informations personnelles", personalTab(selected)));
        tabs.getTabs().add(new Tab("Pièces officielles", identityTab(selected)));
        tabs.getTabs().add(new Tab("Documents", documentsTab(selected)));
        tabs.getTabs().add(new Tab("Observations", observationsTab(selected)));
        tabs.getTabs().forEach(t -> t.setClosable(false));

        dialog.getDialogPane().setContent(tabs);

        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            return collectCustomer(selected == null ? 0 : selected.getId(), tabs);
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                repo.save(c);
                load();
            } catch (Exception ex) {
                alert("Erreur sauvegarde", ex.getMessage());
            }
        });
    }

    private GridPane personalTab(Customer c) {
        GridPane g = grid();
        put(g, 0, "Nom et prénom", field("fullName", c == null ? "" : c.getFullName()));
        put(g, 1, "Sexe", combo("sex", c == null ? "" : c.getSex(), "", "Homme", "Femme"));
        put(g, 2, "Date de naissance", date("birthDate", c == null ? "" : c.getBirthDate()));
        put(g, 3, "Lieu de naissance", field("birthPlace", c == null ? "" : c.getBirthPlace()));
        put(g, 4, "Nationalité", field("nationality", c == null ? "Marocaine" : c.getNationality()));
        put(g, 5, "Adresse", area("address", c == null ? "" : c.getAddress(), 2));
        put(g, 6, "Ville", field("city", c == null ? "" : c.getCity()));
        put(g, 7, "Téléphone", field("phone", c == null ? "" : c.getPhone()));
        put(g, 8, "Email", field("email", c == null ? "" : c.getEmail()));
        put(g, 9, "Profession", field("profession", c == null ? "" : c.getProfession()));
        return g;
    }

    private GridPane identityTab(Customer c) {
        GridPane g = grid();
        put(g, 0, "N° CIN", field("cin", c == null ? "" : c.getCin()));
        put(g, 1, "CIN valable jusqu'au", date("cinExpiry", c == null ? "" : c.getCinExpiry()));
        put(g, 2, "N° Permis de conduire", field("drivingLicense", c == null ? "" : c.getDrivingLicense()));
        put(g, 3, "Permis délivré le", date("licenseIssueDate", c == null ? "" : c.getLicenseIssueDate()));
        put(g, 4, "Permis délivré à", field("licenseIssuePlace", c == null ? "" : c.getLicenseIssuePlace()));
        put(g, 5, "Permis valable jusqu'au", date("licenseExpiry", c == null ? "" : c.getLicenseExpiry()));
        put(g, 6, "N° Passeport", field("passportNumber", c == null ? "" : c.getPassportNumber()));
        put(g, 7, "Passeport valable jusqu'au", date("passportExpiry", c == null ? "" : c.getPassportExpiry()));
        put(g, 8, "N° d'entrée", field("entryNumber", c == null ? "" : c.getEntryNumber()));
        return g;
    }

    private GridPane documentsTab(Customer c) {
        GridPane g = grid();
        put(g, 0, "Photo CIN Recto", fileField("docCinRecto", c == null ? "" : c.getDocCinRecto()));
        put(g, 1, "Photo CIN Verso", fileField("docCinVerso", c == null ? "" : c.getDocCinVerso()));
        put(g, 2, "Photo Permis Recto", fileField("docPermisRecto", c == null ? "" : c.getDocPermisRecto()));
        put(g, 3, "Photo Permis Verso", fileField("docPermisVerso", c == null ? "" : c.getDocPermisVerso()));
        put(g, 4, "Passeport", fileField("docPassport", c == null ? "" : c.getDocPassport()));
        put(g, 5, "Photo client", fileField("photoPath", c == null ? "" : c.getPhotoPath()));
        return g;
    }

    private GridPane observationsTab(Customer c) {
        GridPane g = grid();
        put(g, 0, "Contact urgence", field("emergencyContactName", c == null ? "" : c.getEmergencyContactName()));
        put(g, 1, "Téléphone urgence", field("emergencyContactPhone", c == null ? "" : c.getEmergencyContactPhone()));
        put(g, 2, "Observations", area("observations", c == null ? "" : c.getObservations(), 8));
        return g;
    }

    private GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(20));

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPrefWidth(190);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);

        g.getColumnConstraints().addAll(c1, c2);

        return g;
    }

    private void put(GridPane g, int row, String label, javafx.scene.Node control) {
        Label l = new Label(label);

        if (control instanceof Control c) {
            c.setMaxWidth(Double.MAX_VALUE);
        }

        g.add(l, 0, row);
        g.add(control, 1, row);
    }

    private TextField field(String id, String value) {
        TextField f = new TextField(value == null ? "" : value);
        f.setId(id);
        return f;
    }

    private TextArea area(String id, String value, int rows) {
        TextArea a = new TextArea(value == null ? "" : value);
        a.setId(id);
        a.setPrefRowCount(rows);
        return a;
    }

    private ComboBox<String> combo(String id, String value, String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setId(id);
        cb.setValue(value == null ? "" : value);
        return cb;
    }

    private DatePicker date(String id, String value) {
        DatePicker dp = new DatePicker();
        dp.setId(id);

        try {
            if (value != null && !value.isBlank()) {
                dp.setValue(java.time.LocalDate.parse(value));
            }
        } catch (Exception ignored) {
        }

        return dp;
    }

    private HBox fileField(String id, String value) {
        TextField f = field(id, value);

        Button browse = new Button("Parcourir");
        browse.getStyleClass().add("secondary-button");

        browse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir un document");

            File file = chooser.showOpenDialog(root.getScene() == null ? null : root.getScene().getWindow());

            if (file != null) {
                f.setText(file.getAbsolutePath());
            }
        });

        HBox box = new HBox(8, f, browse);
        HBox.setHgrow(f, Priority.ALWAYS);
        box.setId(id + "Box");

        return box;
    }

    private Customer collectCustomer(int id, TabPane tabs) {
        return new Customer(
                id,
                value(tabs, "fullName"),
                value(tabs, "sex"),
                value(tabs, "birthDate"),
                value(tabs, "birthPlace"),
                value(tabs, "nationality"),
                value(tabs, "address"),
                value(tabs, "city"),
                value(tabs, "phone"),
                value(tabs, "email"),
                value(tabs, "cin"),
                value(tabs, "cinExpiry"),
                value(tabs, "drivingLicense"),
                value(tabs, "licenseIssueDate"),
                value(tabs, "licenseIssuePlace"),
                value(tabs, "licenseExpiry"),
                value(tabs, "passportNumber"),
                value(tabs, "passportExpiry"),
                value(tabs, "entryNumber"),
                value(tabs, "profession"),
                value(tabs, "emergencyContactName"),
                value(tabs, "emergencyContactPhone"),
                value(tabs, "observations"),
                value(tabs, "docCinRecto"),
                value(tabs, "docCinVerso"),
                value(tabs, "docPermisRecto"),
                value(tabs, "docPermisVerso"),
                value(tabs, "docPassport"),
                value(tabs, "photoPath")
        );
    }

    private String value(TabPane tabs, String id) {
        for (Tab t : tabs.getTabs()) {
            if (!(t.getContent() instanceof Parent parent)) {
                continue;
            }

            Control c = findControl(parent, id);

            if (c instanceof TextField tf) {
                return tf.getText();
            }

            if (c instanceof TextArea ta) {
                return ta.getText();
            }

            if (c instanceof DatePicker dp) {
                return dp.getValue() == null ? "" : dp.getValue().toString();
            }

            if (c instanceof ComboBox<?> cb) {
                return cb.getValue() == null ? "" : cb.getValue().toString();
            }
        }

        return "";
    }

    private Control findControl(Parent p, String id) {
        for (javafx.scene.Node n : p.getChildrenUnmodifiable()) {
            if (id.equals(n.getId()) && n instanceof Control c) {
                return c;
            }

            if (n instanceof HBox hb) {
                for (javafx.scene.Node child : hb.getChildren()) {
                    if (id.equals(child.getId()) && child instanceof Control c) {
                        return c;
                    }
                }
            }

            if (n instanceof Parent pp) {
                Control found = findControl(pp, id);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private void deleteSelected() {
        Customer c = table.getSelectionModel().getSelectedItem();

        if (c == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce client ?", ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    repo.delete(c.getId());
                    load();
                } catch (Exception ex) {
                    alert("Erreur suppression", ex.getMessage());
                }
            }
        });
    }

    private void alert(String title, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
