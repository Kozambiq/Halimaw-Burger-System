package com.myapp.halimawburgersystem;

import com.myapp.model.Staff;
import com.myapp.model.User;
import com.myapp.util.DatabaseConnection;
import com.myapp.util.EnvLoader;
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
    private static Parent staffRoot;
    private static Parent cashierRoot;
    private static Parent ordersRoot;
    private static Parent kitchenRoot;
    private static DashboardController dashboardController;
    private static InventoryController inventoryController;
    private static MenuItemsController menuItemsController;
    private static CombosController combosController;
    private static StaffController staffController;
    private static CashierController cashierController;
    private static OrdersController ordersController;
    private static KitchenController kitchenController;
    private static Parent salesReportRoot;
    private static SalesReportController salesReportController;
    private static Parent cookRoot;
    private static CookController cookController;

    private static User currentUser;
    private static Staff currentStaff;

    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;
        EnvLoader.load();
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
        if (dashboardRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/dashboard.fxml"));
            dashboardRoot = fxmlLoader.load();
            dashboardController = fxmlLoader.getController();
        }
        
        cachedScene.setRoot(dashboardRoot);
        mainStage.setTitle("BurgerHQ - Dashboard");
        mainStage.show();
        
        if (dashboardController != null) {
            dashboardController.setActiveNav("Dashboard");
            // Refresh data when returning to tab
            dashboardController.loadDashboardData();
        }
    }

    public static void showKitchen() throws Exception {
        if (kitchenRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/kitchen.fxml"));
            kitchenRoot = fxmlLoader.load();
            kitchenController = fxmlLoader.getController();
        }

        cachedScene.setRoot(kitchenRoot);
        mainStage.setTitle("BurgerHQ - Kitchen Queue");
        mainStage.show();

        if (kitchenController != null) {
            kitchenController.setActiveNav("Kitchen Queue");
            kitchenController.loadQueue();
        }
    }

    public static void showInventory() throws Exception {
        if (inventoryRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/inventory.fxml"));
            inventoryRoot = fxmlLoader.load();
            inventoryController = fxmlLoader.getController();
        }
        
        cachedScene.setRoot(inventoryRoot);
        mainStage.setTitle("BurgerHQ - Inventory");
        mainStage.show();
        
        if (inventoryController != null) {
            inventoryController.setActiveNav("Inventory");
            inventoryController.loadInventory();
        }
    }
    
    public static void showMenuItems() throws Exception {
        if (menuItemsRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/menuitems.fxml"));
            menuItemsRoot = fxmlLoader.load();
            menuItemsController = fxmlLoader.getController();
        }

        cachedScene.setRoot(menuItemsRoot);
        mainStage.setTitle("BurgerHQ - Menu Items");
        mainStage.show();

        if (menuItemsController != null) {
            menuItemsController.setActiveNav("Menu Items");
            menuItemsController.loadMenuItems();
        }
    }

    public static void showCombos() throws Exception {
        if (combosRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/combos.fxml"));
            combosRoot = fxmlLoader.load();
            combosController = fxmlLoader.getController();
        }

        cachedScene.setRoot(combosRoot);
        mainStage.setTitle("BurgerHQ - Combos & Promos");
        mainStage.show();

        if (combosController != null) {
            combosController.setActiveNav("Combos & Promos");
            combosController.loadCombos();
        }
    }

    public static void showStaff() throws Exception {
        if (staffRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/staff.fxml"));
            staffRoot = fxmlLoader.load();
            staffController = fxmlLoader.getController();
        }

        cachedScene.setRoot(staffRoot);
        mainStage.setTitle("BurgerHQ - Staff");
        mainStage.show();

        if (staffController != null) {
            staffController.setActiveNav("Staff");
            staffController.loadStaff();
        }
    }

    public static void showOrders() throws Exception {
        if (ordersRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/orders.fxml"));
            ordersRoot = fxmlLoader.load();
            ordersController = fxmlLoader.getController();
        }

        cachedScene.setRoot(ordersRoot);
        mainStage.setTitle("BurgerHQ - Orders");
        mainStage.show();

        if (ordersController != null) {
            ordersController.setActiveNav("Orders");
            ordersController.loadOrders();
        }
    }

    public static void showCashier() throws Exception {
        if (cashierRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/cashier.fxml"));
            cashierRoot = fxmlLoader.load();
            cashierController = fxmlLoader.getController();
        }

        cachedScene.setRoot(cashierRoot);
        mainStage.setTitle("BurgerHQ - Cashier");
        mainStage.show();

        if (cashierController != null) {
            cashierController.setActiveNav("Cashier");
        }
    }

    public static void showSalesReport() throws Exception {
        if (salesReportRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/salesreport.fxml"));
            salesReportRoot = fxmlLoader.load();
            salesReportController = fxmlLoader.getController();
        }

        cachedScene.setRoot(salesReportRoot);
        mainStage.setTitle("BurgerHQ - Sales Reports");
        mainStage.show();

        if (salesReportController != null) {
            salesReportController.setActiveNav("Sales Reports");
            salesReportController.loadReport();
        }
    }

    public static void showCook() throws Exception {
        if (cookRoot == null) {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/fxml/cook.fxml"));
            cookRoot = fxmlLoader.load();
            cookController = fxmlLoader.getController();
        }

        cachedScene.setRoot(cookRoot);
        mainStage.setTitle("BurgerHQ - Kitchen Queue");
        mainStage.show();

        if (cookController != null) {
            cookController.loadQueue();
        }
    }

    public static void clearAllCaches() {
        dashboardRoot = null;
        inventoryRoot = null;
        menuItemsRoot = null;
        combosRoot = null;
        staffRoot = null;
        cashierRoot = null;
        ordersRoot = null;
        kitchenRoot = null;
        salesReportRoot = null;
        cookRoot = null;
        
        dashboardController = null;
        inventoryController = null;
        menuItemsController = null;
        combosController = null;
        staffController = null;
        cashierController = null;
        ordersController = null;
        kitchenController = null;
        salesReportController = null;
        cookController = null;
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

    public static void clearStaffCache() {
        staffRoot = null;
    }

    public static void clearOrdersCache() {
        ordersRoot = null;
    }

    public static void clearCashierCache() {
        cashierRoot = null;
    }

    public static void clearCookCache() {
        cookRoot = null;
        cookController = null;
    }

    public static void clearSalesReportCache() {
        salesReportRoot = null;
        salesReportController = null;
    }

@Override
    public void stop() throws Exception {
        DatabaseConnection.close();
        super.stop();
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static Staff getCurrentStaff() {
        return currentStaff;
    }

    public static void setCurrentStaff(Staff staff) {
        currentStaff = staff;
    }

    public static void clearSession() {
        currentUser = null;
        currentStaff = null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}