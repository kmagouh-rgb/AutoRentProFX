package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DossierLocationView {
    private final BorderPane root = new BorderPane();
    private final TableView<DossierRow> table = new TableView<>();
    private final TextField search = new TextField();
    private final VBox details = new VBox(12);
    private DossierRow selectedRow;

    public DossierLocationView() {
        build();
        load();
    }

    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(22));
        root.getStyleClass().add("page-root");

        VBox top = new VBox(12);
        Label title = new Label("Dossiers de location");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Dossier complet: contrat, client, véhicule, paiements, caution, départ, retour, PDF et historique.");
        subtitle.getStyleClass().add("muted");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        search.setPromptText("Recherche: contrat, client, véhicule, téléphone, statut...");
        search.getStyleClass().add("search-field");
        HBox.setHgrow(search, Priority.ALWAYS);

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().add("secondary-button");
        actions.getChildren().addAll(search, refresh);
        top.getChildren().addAll(title, subtitle, actions);
        root.setTop(top);

        setupTable();
        details.setPadding(new Insets(18));
        details.getStyleClass().add("content-card");
        details.getChildren().add(hint());

        ScrollPane detailsScroll = new ScrollPane(details);
        detailsScroll.setFitToWidth(true);
        detailsScroll.getStyleClass().add("transparent-scroll");

        SplitPane split = new SplitPane(table, detailsScroll);
        split.setDividerPositions(0.46);
        split.setPadding(new Insets(16, 0, 0, 0));
        root.setCenter(split);

        search.textProperty().addListener((o, a, b) -> load());
        refresh.setOnAction(e -> load());
        table.getSelectionModel().selectedItemProperty().addListener((o, old, row) -> showDossier(row));
    }

    private Label hint() {
        Label hint = new Label("Sélectionnez un contrat pour voir le dossier complet.");
        hint.getStyleClass().add("muted");
        return hint;
    }

    private void setupTable() {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<DossierRow, String> num = new TableColumn<>("Contrat");
        num.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().contractNumber));

        TableColumn<DossierRow, String> client = new TableColumn<>("Client");
        client.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().customer));

        TableColumn<DossierRow, String> vehicle = new TableColumn<>("Véhicule");
        vehicle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().vehicle));

        TableColumn<DossierRow, String> period = new TableColumn<>("Période");
        period.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().startDate + " → " + c.getValue().endDate));

        TableColumn<DossierRow, String> paid = new TableColumn<>("Paiement");
        paid.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f / %.2f DH", c.getValue().paidAmount, c.getValue().totalAmount)));

        TableColumn<DossierRow, String> rest = new TableColumn<>("Reste");
        rest.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f DH", c.getValue().remaining())));

        TableColumn<DossierRow, String> status = new TableColumn<>("Statut");
        status.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));

        table.getColumns().setAll(num, client, vehicle, period, paid, rest, status);
    }

    private void load() {
        ObservableList<DossierRow> rows = FXCollections.observableArrayList();
        String q = search.getText() == null ? "" : search.getText().trim();
        String like = "%" + q + "%";
        String sql = "SELECT c.id, c.contract_number, c.start_date, c.end_date, c.total_amount, c.paid_amount, c.status, " +
                "cu.full_name, cu.phone, cu.cin, CONCAT(COALESCE(v.registration,''),' - ',COALESCE(v.brand,''),' ',COALESCE(v.model,'')) vehicle_label " +
                "FROM contracts c " +
                "LEFT JOIN customers cu ON cu.id=c.customer_id " +
                "LEFT JOIN vehicles v ON v.id=c.vehicle_id " +
                "WHERE c.contract_number LIKE ? OR cu.full_name LIKE ? OR cu.phone LIKE ? OR cu.cin LIKE ? OR v.registration LIKE ? OR v.brand LIKE ? OR v.model LIKE ? OR c.status LIKE ? " +
                "ORDER BY c.id DESC";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 1; i <= 8; i++) ps.setString(i, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DossierRow(
                            rs.getInt("id"), safe(rs.getString("contract_number")), safe(rs.getString("full_name")),
                            safe(rs.getString("phone")), safe(rs.getString("cin")), safe(rs.getString("vehicle_label")),
                            safeDate(rs.getDate("start_date")), safeDate(rs.getDate("end_date")),
                            rs.getDouble("total_amount"), rs.getDouble("paid_amount"), safe(rs.getString("status"))
                    ));
                }
            }
        } catch (Exception ex) {
            alert("Erreur chargement dossiers", ex.getMessage());
        }
        table.setItems(rows);
        if (!rows.isEmpty()) table.getSelectionModel().selectFirst();
    }

    private void showDossier(DossierRow row) {
        selectedRow = row;
        details.getChildren().clear();
        if (row == null) {
            details.getChildren().add(hint());
            return;
        }

        Label title = new Label("Dossier " + row.contractNumber);
        title.getStyleClass().add("page-title");
        Label status = new Label(row.status);
        status.getStyleClass().add("pill");

        HBox header = new HBox(12, title, status);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox actions = dossierActions(row);

        HBox kpis = new HBox(12,
                metric("Total", String.format("%.2f DH", row.totalAmount)),
                metric("Payé", String.format("%.2f DH", row.paidAmount)),
                metric("Reste", String.format("%.2f DH", row.remaining()))
        );
        kpis.setAlignment(Pos.CENTER_LEFT);

        GridPane info = grid();
        int r = 0;
        add(info, r++, "Client", row.customer);
        add(info, r++, "Téléphone", row.phone);
        add(info, r++, "CIN", row.cin);
        add(info, r++, "Véhicule", row.vehicle);
        add(info, r++, "Départ", row.startDate);
        add(info, r++, "Retour", row.endDate);
        add(info, r++, "Statut", row.status);

        VBox payments = paymentsBox(row.id);
        VBox timeline = timelineBox(row);

        details.getChildren().addAll(header, actions, kpis, section("Informations dossier"), info, payments, timeline);
    }

    private HBox dossierActions(DossierRow row) {
        Button pdf = new Button("PDF dossier");
        pdf.getStyleClass().add("primary-button");
        pdf.setOnAction(e -> exportDossierPdf(row));

        Button payment = new Button("Paiement");
        payment.getStyleClass().add("secondary-button");
        payment.setOnAction(e -> addPayment(row));

        Button retour = new Button("Retour");
        retour.getStyleClass().add("secondary-button");
        retour.setOnAction(e -> markStatus(row, "RETOUR_AUJOURD_HUI"));

        Button close = new Button("Clôturer");
        close.getStyleClass().add("success-button");
        close.setOnAction(e -> markStatus(row, "CLOTURE"));

        HBox box = new HBox(10, pdf, payment, retour, close);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private VBox metric(String label, String value) {
        VBox box = new VBox(3);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("metric-card");
        Label l = new Label(label);
        l.getStyleClass().add("muted");
        Label v = new Label(value);
        v.getStyleClass().add("metric-value");
        box.getChildren().addAll(l, v);
        return box;
    }

    private Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private VBox paymentsBox(int contractId) {
        VBox box = new VBox(8);
        box.getStyleClass().add("dossier-timeline");
        box.getChildren().add(section("Paiements du dossier"));
        List<String> payments = paymentLines(contractId);
        if (payments.isEmpty()) {
            Label none = new Label("Aucun paiement enregistré.");
            none.getStyleClass().add("muted");
            box.getChildren().add(none);
        } else {
            for (String p : payments) box.getChildren().add(step("DH", "Paiement", p));
        }
        return box;
    }

    private VBox timelineBox(DossierRow row) {
        VBox timeline = new VBox(8);
        timeline.getStyleClass().add("dossier-timeline");
        timeline.getChildren().addAll(section("Timeline du dossier"),
                step("1", "Contrat créé", row.contractNumber),
                step("2", "Paiements", paymentsSummary(row.id)),
                step("3", "Départ véhicule", row.startDate),
                step("4", "Retour prévu", row.endDate),
                step("5", "Clôture", row.status)
        );
        return timeline;
    }

    private void addPayment(DossierRow row) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Ajouter paiement - " + row.contractNumber);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField amount = new TextField();
        amount.setPromptText("Montant");
        ComboBox<String> method = new ComboBox<>();
        method.getItems().addAll("ESPECES", "CARTE", "VIREMENT", "CHEQUE", "AUTRE");
        method.setValue("ESPECES");
        TextField notes = new TextField();
        notes.setPromptText("Observation");
        GridPane g = grid();
        addNode(g, 0, "Montant", amount);
        addNode(g, 1, "Méthode", method);
        addNode(g, 2, "Observation", notes);
        dialog.getDialogPane().setContent(g);
        dialog.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try { return Double.parseDouble(amount.getText().replace(',', '.')); }
            catch (Exception ex) { return -1.0; }
        });
        dialog.showAndWait().ifPresent(v -> {
            if (v == null || v <= 0) { alert("Paiement", "Montant invalide."); return; }
            try (Connection cn = Db.getConnection()) {
                cn.setAutoCommit(false);
                try (PreparedStatement p1 = cn.prepareStatement("INSERT INTO payments(contract_id,amount,payment_date,method,notes) VALUES(?, ?, CURDATE(), ?, ?)");
                     PreparedStatement p2 = cn.prepareStatement("UPDATE contracts SET paid_amount=COALESCE(paid_amount,0)+? WHERE id=?")) {
                    p1.setInt(1, row.id); p1.setDouble(2, v); p1.setString(3, method.getValue()); p1.setString(4, notes.getText()); p1.executeUpdate();
                    p2.setDouble(1, v); p2.setInt(2, row.id); p2.executeUpdate();
                    cn.commit();
                } catch (Exception ex) { cn.rollback(); throw ex; }
                load();
            } catch (Exception ex) { alert("Erreur paiement", ex.getMessage()); }
        });
    }

    private void markStatus(DossierRow row, String newStatus) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Changer le statut du dossier vers " + newStatus + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.YES) return;
            try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement("UPDATE contracts SET status=? WHERE id=?")) {
                ps.setString(1, newStatus);
                ps.setInt(2, row.id);
                ps.executeUpdate();
                load();
            } catch (Exception ex) { alert("Erreur statut", ex.getMessage()); }
        });
    }

    private void exportDossierPdf(DossierRow row) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer dossier PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName((row.contractNumber == null || row.contractNumber.isBlank() ? "dossier" : row.contractNumber) + "_dossier.pdf");
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) return;
        try {
            generateDossierPdf(file, row);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file);
        } catch (Exception ex) { alert("Erreur PDF", ex.getMessage()); }
    }

    private void generateDossierPdf(File file, DossierRow row) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 790;
                text(cs, PDType1Font.HELVETICA_BOLD, 18, 45, y, "DOSSIER DE LOCATION"); y -= 28;
                text(cs, PDType1Font.HELVETICA_BOLD, 12, 45, y, "Contrat: " + row.contractNumber); y -= 20;
                text(cs, PDType1Font.HELVETICA, 11, 45, y, "Client: " + row.customer + "   Téléphone: " + row.phone); y -= 18;
                text(cs, PDType1Font.HELVETICA, 11, 45, y, "CIN: " + row.cin); y -= 18;
                text(cs, PDType1Font.HELVETICA, 11, 45, y, "Véhicule: " + row.vehicle); y -= 18;
                text(cs, PDType1Font.HELVETICA, 11, 45, y, "Période: " + row.startDate + " -> " + row.endDate); y -= 18;
                text(cs, PDType1Font.HELVETICA, 11, 45, y, String.format("Total: %.2f DH   Payé: %.2f DH   Reste: %.2f DH", row.totalAmount, row.paidAmount, row.remaining())); y -= 30;
                text(cs, PDType1Font.HELVETICA_BOLD, 13, 45, y, "Paiements"); y -= 20;
                List<String> payments = paymentLines(row.id);
                if (payments.isEmpty()) payments.add("Aucun paiement enregistré.");
                for (String p : payments) {
                    if (y < 70) break;
                    text(cs, PDType1Font.HELVETICA, 10, 60, y, "- " + p); y -= 16;
                }
                y -= 10;
                text(cs, PDType1Font.HELVETICA_BOLD, 13, 45, y, "Timeline"); y -= 20;
                String[] steps = {"Contrat créé: " + row.contractNumber, "Départ: " + row.startDate, "Retour prévu: " + row.endDate, "Statut: " + row.status};
                for (String s : steps) { text(cs, PDType1Font.HELVETICA, 10, 60, y, "- " + s); y -= 16; }
            }
            doc.save(file);
        }
    }

    private String paymentsSummary(int contractId) {
        String sql = "SELECT COUNT(*) nb, COALESCE(SUM(amount),0) total FROM payments WHERE contract_id=?";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("nb") + " paiement(s) / " + String.format("%.2f DH", rs.getDouble("total"));
            }
        } catch (Exception ignored) {}
        return "Aucun paiement";
    }

    private List<String> paymentLines(int contractId) {
        List<String> out = new ArrayList<>();
        String sql = "SELECT payment_date, amount, method, notes FROM payments WHERE contract_id=? ORDER BY payment_date, id";
        try (Connection cn = Db.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(safeDate(rs.getDate("payment_date")) + " | " + String.format("%.2f DH", rs.getDouble("amount")) +
                            " | " + safe(rs.getString("method")) +
                            (rs.getString("notes") == null || rs.getString("notes").isBlank() ? "" : " | " + rs.getString("notes")));
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(8);
        g.setPadding(new Insets(12));
        g.getStyleClass().add("dossier-grid");
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPrefWidth(140);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c1, c2);
        return g;
    }

    private void add(GridPane g, int row, String label, String value) {
        Label l = new Label(label); l.getStyleClass().add("muted");
        Label v = new Label(value == null ? "" : value); v.getStyleClass().add("dossier-value");
        g.add(l, 0, row); g.add(v, 1, row);
    }

    private void addNode(GridPane g, int row, String label, javafx.scene.Node node) {
        Label l = new Label(label); l.getStyleClass().add("muted");
        if (node instanceof Control c) c.setMaxWidth(Double.MAX_VALUE);
        g.add(l, 0, row); g.add(node, 1, row);
    }

    private HBox step(String number, String title, String text) {
        Label n = new Label(number); n.getStyleClass().add("timeline-number");
        VBox content = new VBox(2);
        Label t = new Label(title); t.getStyleClass().add("section-title");
        Label d = new Label(text == null ? "" : text); d.getStyleClass().add("muted");
        content.getChildren().addAll(t, d);
        HBox box = new HBox(10, n, content);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("timeline-step");
        return box;
    }

    private void text(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String value) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(pdfText(value));
        cs.endText();
    }

    private String pdfText(String value) {
        if (value == null) return "";
        return value.replace('’', '\'').replace('“', '"').replace('”', '"').replace('–', '-').replace('—', '-').replace(' ', ' ');
    }

    private void alert(String title, String msg) { new Alert(Alert.AlertType.ERROR, msg == null ? "" : msg, ButtonType.OK).showAndWait(); }
    private String safe(String s) { return s == null ? "" : s; }
    private String safeDate(java.sql.Date d) { return d == null ? "" : d.toString(); }

    private record DossierRow(int id, String contractNumber, String customer, String phone, String cin, String vehicle,
                              String startDate, String endDate, double totalAmount, double paidAmount, String status) {
        double remaining() { return Math.max(0, totalAmount - paidAmount); }
    }
}
