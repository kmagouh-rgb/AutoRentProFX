package com.autorentpro.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupView {
    private final BorderPane root = new BorderPane();
    private final TextArea log = new TextArea();
    private final TextField outputDir = new TextField("backup");

    public BackupView() {
        build();
    }

    public Parent getView() {
        return root;
    }

    private void build() {
        root.setPadding(new Insets(22));

        Label title = new Label("Sauvegarde / Restauration");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Créer une sauvegarde SQL de la base autorent_pro. La restauration reste manuelle par sécurité.");
        subtitle.getStyleClass().add("muted");

        Button browse = new Button("Choisir dossier");
        browse.getStyleClass().add("secondary-button");
        browse.setOnAction(e -> chooseFolder());

        Button backup = new Button("Créer backup SQL");
        backup.getStyleClass().add("primary-button");
        backup.setOnAction(e -> createBackup());

        HBox line = new HBox(10, outputDir, browse, backup);
        HBox.setHgrow(outputDir, Priority.ALWAYS);

        log.setEditable(false);
        log.setPrefRowCount(18);
        log.getStyleClass().add("data-table");

        VBox box = new VBox(12, title, subtitle, line, log);
        root.setCenter(box);
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir dossier backup");
        File dir = chooser.showDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (dir != null) outputDir.setText(dir.getAbsolutePath());
    }

    private void createBackup() {
        try {
            Path dir = Path.of(outputDir.getText().trim().isEmpty() ? "backup" : outputDir.getText().trim());
            Files.createDirectories(dir);

            String fileName = "autorent_pro_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
            Path out = dir.resolve(fileName);

            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c",
                    "mysqldump -u root autorent_pro > \"" + out.toAbsolutePath() + "\""
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
            }
            int code = p.waitFor();

            if (code == 0 && Files.exists(out) && Files.size(out) > 0) {
                log.appendText("Backup créé: " + out.toAbsolutePath() + "\n");
                AuditLogger.log("SYSTEM", "BACKUP", out.getFileName().toString(), "Sauvegarde SQL créée");
            } else {
                log.appendText("Backup non créé. Vérifiez que mysqldump est accessible depuis PATH.\n");
                log.appendText(sb.toString() + "\n");
            }
        } catch (Exception ex) {
            log.appendText("Erreur backup: " + ex.getMessage() + "\n");
        }
    }
}
