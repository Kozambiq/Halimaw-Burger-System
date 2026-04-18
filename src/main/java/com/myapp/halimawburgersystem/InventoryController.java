package com.myapp.halimawburgersystem;

import com.myapp.dao.IngredientDAO;
import com.myapp.model.Ingredient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

    private IngredientDAO ingredientDAO = new IngredientDAO();
    private boolean alreadyLoaded = false;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;
        
        setupTableColumns();
        loadInventory();
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

    private void loadInventory() {
        try {
            int total = ingredientDAO.getTotalCount();
            int low = ingredientDAO.getLowStockCount();
            int out = ingredientDAO.getOutOfStockCount();
            List<Ingredient> ingredients = ingredientDAO.findAll();

            final int fTotal = total;
            final int fLow = low;
            final int fOut = out;

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(fTotal));
                lblLowStock.setText(String.valueOf(fLow));
                lblOutOfStock.setText(String.valueOf(fOut));
                
                // Force create new list instance
                inventoryTable.setItems(FXCollections.observableArrayList(ingredients));
            });
        } catch (Exception e) {
            System.err.println("Error loading inventory: " + e.getMessage());
            alreadyLoaded = false;
        }
    }
}