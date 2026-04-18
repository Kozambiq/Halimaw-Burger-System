package com.myapp.halimawburgersystem;

import com.myapp.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage mainStage;

    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;
        DatabaseConnection.initialize();
        showLogin();
    }

    public static void showDashboard() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1280, 800);
        mainStage.setTitle("BurgerHQ - Staff Portal");
        mainStage.setScene(scene);
        mainStage.setFullScreen(true);
        mainStage.show();
    }

    public static void showLogin() throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 940, 600);
        mainStage.setTitle("Halimaw Burger - Staff Portal");
        mainStage.setScene(scene);
        mainStage.setFullScreen(true);
        mainStage.show();
    }

    @Override
    public void stop() throws Exception {
        DatabaseConnection.close();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}