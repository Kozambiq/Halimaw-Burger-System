package com.myapp.halimawburgersystem;

import com.myapp.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage mainStage;
    private static Scene cachedScene;
    private static Parent loginRoot;
    private static Parent dashboardRoot;
    private static Parent inventoryRoot;
    private static Parent menuItemsRoot;
    private static Parent combosRoot;
    private static DashboardController dashboardController;
    private static MenuItemsController menuItemsController;
    private static CombosController combosController;

    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;
        DatabaseConnection.initialize();
        showLogin();
    }

    public static void showLogin() throws Exception {
        if (loginRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            loginRoot = fxmlLoader.load();
        }
        
        if (cachedScene == null) {
            cachedScene = new Scene(loginRoot, 940, 600);
            mainStage.setScene(cachedScene);
        } else {
            cachedScene.setRoot(loginRoot);
        }
        
        mainStage.setTitle("Halimaw Burger - Staff Portal");
        mainStage.show();
    }

    public static void showDashboard() throws Exception {
        clearInventoryCache();
        
        if (dashboardRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
            dashboardRoot = fxmlLoader.load();
            dashboardController = fxmlLoader.getController();
        }
        
        if (cachedScene == null) {
            cachedScene = new Scene(dashboardRoot, 1280, 800);
            mainStage.setScene(cachedScene);
        } else {
            cachedScene.setRoot(dashboardRoot);
        }
        
        mainStage.setTitle("BurgerHQ - Staff Portal");
        mainStage.show();
        
        if (dashboardController != null) {
            dashboardController.setActiveNav("Dashboard");
        }
    }

    public static void showInventory() throws Exception {
        clearMenuItemsCache();

        if (inventoryRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/inventory.fxml"));
            inventoryRoot = fxmlLoader.load();
        }
        
        if (cachedScene == null) {
            cachedScene = new Scene(inventoryRoot, 1280, 800);
            mainStage.setScene(cachedScene);
        } else {
            cachedScene.setRoot(inventoryRoot);
        }
        
        mainStage.setTitle("BurgerHQ - Inventory");
        mainStage.show();
        
        if (dashboardController != null) {
            dashboardController.setActiveNav("Inventory");
        }
    }
    
    public static void showMenuItems() throws Exception {
        clearCombosCache();

        if (menuItemsRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/menuitems.fxml"));
            menuItemsRoot = fxmlLoader.load();
            menuItemsController = fxmlLoader.getController();
        }

        if (cachedScene == null) {
            cachedScene = new Scene(menuItemsRoot, 1280, 800);
            mainStage.setScene(cachedScene);
        } else {
            cachedScene.setRoot(menuItemsRoot);
        }

        mainStage.setTitle("BurgerHQ - Menu Items");
        mainStage.show();

        if (menuItemsController != null) {
            menuItemsController.setActiveNav("Menu Items");
        }
    }

    public static void showCombos() throws Exception {
        clearMenuItemsCache();

        if (combosRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/combos.fxml"));
            combosRoot = fxmlLoader.load();
            combosController = fxmlLoader.getController();
        }

        if (cachedScene == null) {
            cachedScene = new Scene(combosRoot, 1280, 800);
            mainStage.setScene(cachedScene);
        } else {
            cachedScene.setRoot(combosRoot);
        }

        mainStage.setTitle("BurgerHQ - Combos & Promos");
        mainStage.show();

        if (combosController != null) {
            combosController.setActiveNav("Combos & Promos");
        }
    }

    public static void clearInventoryCache() {
        inventoryRoot = null;
    }

    public static void clearMenuItemsCache() {
        menuItemsRoot = null;
    }

    public static void clearCombosCache() {
        combosRoot = null;
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