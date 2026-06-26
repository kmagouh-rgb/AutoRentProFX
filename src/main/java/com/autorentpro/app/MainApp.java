package com.autorentpro.app;

import com.autorentpro.ui.LoginView;
import com.autorentpro.db.Db;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        Db.ensureSchemaCompatibility();
        LoginView login = new LoginView(stage);
        Scene scene = new Scene(login.getView(), 1040, 680);
        scene.getStylesheets().add(getClass().getResource("/com/autorentpro/css/app.css").toExternalForm());
        stage.setTitle("AutoRent Pro FX V6.1");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
