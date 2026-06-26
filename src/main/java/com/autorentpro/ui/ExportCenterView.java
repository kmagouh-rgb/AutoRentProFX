package com.autorentpro.ui;

import com.autorentpro.db.Db;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportCenterView {
    private final VBox root = new VBox(18);
    private final Label status = new Label("Prêt pour export et sauvegarde.");

    public ExportCenterView() { build(); }
    public Parent getView() { return root; }

    private void build() {
        root.setPadding(new Insets(24));
        Label title = new Label("Outils: Export & Sauvegarde");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Export CSV rapide des données principales et sauvegarde SQL simple de la base autorent_pro.");
        sub.getStyleClass().add("muted");

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        ColumnConstraints c = new ColumnConstraints(); c.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c, c);

        grid.add(toolCard("Export véhicules", "Crée un fichier CSV avec la liste des véhicules.", "+ Export véhicules", () -> exportTable("vehicles")), 0, 0);
        grid.add(toolCard("Export clients", "Crée un fichier CSV avec la liste des clients.", "+ Export clients", () -> exportTable("customers")), 1, 0);
        grid.add(toolCard("Export contrats", "Crée un fichier CSV avec les contrats et montants.", "+ Export contrats", () -> exportTable("contracts")), 0, 1);
        grid.add(toolCard("Sauvegarde SQL", "Crée une sauvegarde SQL simple des tables principales.", "+ Backup SQL", this::backupSql), 1, 1);

        status.getStyleClass().add("muted");
        root.getChildren().addAll(title, sub, grid, status);
    }

    private VBox toolCard(String title, String text, String buttonText, Runnable action) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("content-card");
        Label t = new Label(title); t.getStyleClass().add("section-title");
        Label m = new Label(text); m.getStyleClass().add("muted"); m.setWrapText(true);
        Button b = new Button(buttonText); b.getStyleClass().add("primary-button");
        b.setOnAction(e -> action.run());
        box.getChildren().addAll(t, m, b);
        return box;
    }

    private File chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir dossier de sortie");
        return chooser.showDialog(root.getScene() == null ? null : root.getScene().getWindow());
    }

    private void exportTable(String table) {
        File folder = chooseFolder();
        if (folder == null) return;
        String name = table + "_" + stamp() + ".csv";
        Path out = folder.toPath().resolve(name);
        try (Connection cn = Db.getConnection(); Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + table); BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                if (i > 1) w.write(";");
                w.write(csv(md.getColumnLabel(i)));
            }
            w.newLine();
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) w.write(";");
                    w.write(csv(rs.getString(i)));
                }
                w.newLine();
            }
            status.setText("Export créé: " + out);
            info("Export terminé", "Fichier créé:\n" + out);
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void backupSql() {
        File folder = chooseFolder();
        if (folder == null) return;
        Path out = folder.toPath().resolve("autorent_backup_" + stamp() + ".sql");
        String[] tables = {"users", "vehicles", "customers", "reservations", "contracts", "payments", "maintenance", "expenses", "vehicle_documents"};
        try (Connection cn = Db.getConnection(); BufferedWriter w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write("-- AutoRent Pro FX backup generated " + LocalDateTime.now()); w.newLine();
            w.write("CREATE DATABASE IF NOT EXISTS autorent_pro CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"); w.newLine();
            w.write("USE autorent_pro;"); w.newLine(); w.newLine();
            for (String table : tables) dumpTable(cn, w, table);
            status.setText("Backup SQL créé: " + out);
            info("Sauvegarde terminée", "Fichier créé:\n" + out);
        } catch (Exception ex) {
            error(ex);
        }
    }

    private void dumpTable(Connection cn, BufferedWriter w, String table) throws Exception {
        if (!tableExists(cn, table)) return;
        w.write("\n-- Table " + table); w.newLine();
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                w.write("INSERT INTO " + table + " (");
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) w.write(",");
                    w.write("`" + md.getColumnLabel(i) + "`");
                }
                w.write(") VALUES (");
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) w.write(",");
                    String value = rs.getString(i);
                    if (value == null) w.write("NULL");
                    else w.write("'" + value.replace("'", "''") + "'");
                }
                w.write(");"); w.newLine();
            }
        }
    }

    private boolean tableExists(Connection cn, String table) {
        try (PreparedStatement ps = cn.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        } catch (Exception e) { return false; }
    }

    private String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private String stamp() { return DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now()); }

    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(title); a.showAndWait();
    }

    private void error(Exception ex) {
        status.setText("Erreur: " + ex.getMessage());
        Alert a = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
        a.setHeaderText("Erreur export/backup"); a.showAndWait();
    }
}
