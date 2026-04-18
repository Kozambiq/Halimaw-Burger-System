package com.myapp.halimawburgersystem;

import com.myapp.dao.IngredientDAO;
import com.myapp.model.Ingredient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class InventoryController {

    @FXML private Label lblTotal;
    @FXML private Label lblLowStock;
    @FXML private Label lblOutOfStock;
    @FXML private TableView<Ingredient> inventoryTable;
    @FXML private TableColumn<Ingredient, String> colName;
    @FXML private TableColumn<Ingredient, String> colQuantity;
    @FXML private TableColumn<Ingredient, String> colUnit;
    @FXML private TableColumn<Ingredient, String> colStatus;
    
    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    private IngredientDAO ingredientDAO = new IngredientDAO();
    private boolean alreadyLoaded = false;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;
        
        setActiveNav("Inventory");
        setupTableColumns();
        loadInventory();
    }

    private void setActiveNav(String navName) {
        clearAllHighlights();
        switch(navName) {
            case "Dashboard":
                if (btnDashboard != null) btnDashboard.getStyleClass().add("nav-item-active");
                break;
            case "Orders":
                if (btnOrders != null) btnOrders.getStyleClass().add("nav-item-active");
                break;
            case "Kitchen Queue":
                if (btnKitchen != null) btnKitchen.getStyleClass().add("nav-item-active");
                break;
            case "Menu Items":
                if (btnMenuItems != null) btnMenuItems.getStyleClass().add("nav-item-active");
                break;
            case "Combos & Promos":
                if (btnCombos != null) btnCombos.getStyleClass().add("nav-item-active");
                break;
            case "Inventory":
                if (btnInventory != null) btnInventory.getStyleClass().add("nav-item-active");
                break;
            case "Sales Reports":
                if (btnSales != null) btnSales.getStyleClass().add("nav-item-active");
                break;
            case "Staff":
                if (btnStaff != null) btnStaff.getStyleClass().add("nav-item-active");
                break;
        }
    }

    private void clearAllHighlights() {
        Button[] buttons = {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
        if (buttons == null) return;
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-item-active");
            }
        }
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colQuantity.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f", cellData.getValue().getQuantity())
            )
        );
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadInventory() {
        try {
            int total = ingredientDAO.getTotalCount();
            int low = ingredientDAO.getLowStockCount();
            int out = ingredientDAO.getOutOfStockCount();
            List<Ingredient> ingredients = ingredientDAO.findAll();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblLowStock.setText(String.valueOf(low));
                lblOutOfStock.setText(String.valueOf(out));
                inventoryTable.setItems(FXCollections.observableArrayList(ingredients));
            });
        } catch (Exception e) {
            System.err.println("Error loading inventory: " + e.getMessage());
        }
    }

    @FXML
    private void onRestock() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Restock");
        alert.setHeaderText(null);
        alert.setContentText("Select an ingredient first, then restock.");
        alert.showAndWait();
    }

    @FXML
    private void onAddIngredient() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Add Ingredient");
        alert.setHeaderText(null);
        alert.setContentText("Feature coming soon!");
        alert.showAndWait();
    }

    @FXML
    private void onLogout() {
        try {
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateDashboard() {
        try {
            Main.showDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateOrders() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Orders");
        alert.setHeaderText(null);
        alert.setContentText("Coming soon!");
        alert.showAndWait();
    }

    @FXML
    private void onNavigateKitchen() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kitchen Queue");
        alert.setHeaderText(null);
        alert.setContentText("Coming soon!");
        alert.showAndWait();
    }

    @FXML
    private void onNavigateMenuItems() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Menu Items");
        alert.setHeaderText(null);
        alert.setContentText("Coming soon!");
        alert.showAndWait();
    }

    @FXML
    private void onNavigateCombos() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Combos & Promos");
        alert.setHeaderText(null);
        alert.setContentText("Coming soon!");
        alert.showAndWait();
    }

    @FXML
    private void onNavigateInventory() {
        try {
            Main.showInventory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateSales() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sales Reports");
        alert.setHeaderText(null);
        alert.setContentText("Coming soon!");
        alert.showAndWait();
    }

    @FXML
    private void onNavigateStaff() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Staff");
        alert.setHeaderText(null);
        alert.setContentText("Coming soon!");
        alert.showAndWait();
    }
}