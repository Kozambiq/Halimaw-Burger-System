package com.myapp.halimawburgersystem;

import com.myapp.dao.IngredientDAO;
import com.myapp.model.Ingredient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.util.Callback;
import javafx.scene.paint.Color;

import java.util.List;

public class InventoryController {

    @FXML private Label lblTotal;
    @FXML private Label lblLowStock;
    @FXML private Label lblOutOfStock;
    @FXML private TableView<Ingredient> inventoryTable;
    @FXML private TableColumn<Ingredient, String> colName;
    @FXML private TableColumn<Ingredient, Integer> colStockLevel;
    @FXML private TableColumn<Ingredient, String> colQuantity;
    @FXML private TableColumn<Ingredient, String> colUnit;
    @FXML private TableColumn<Ingredient, String> colStatus;
    @FXML private TableColumn<Ingredient, String> colActions;
    
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

        colStockLevel.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(
                cellData.getValue().getStockPercentage()
            )
        );
        colStockLevel.setCellFactory(col -> new TableCell<Ingredient, Integer>() {
            @Override
            protected void updateItem(Integer percentage, boolean empty) {
                super.updateItem(percentage, empty);
                if (empty || percentage == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Ingredient ing = getTableView().getItems().get(getIndex());
                String status = ing.getStatus();
                final double BAR_WIDTH = 180.0;
                double maxStock = ing.getMaxStock();
                double minThreshold = ing.getMinThreshold();

                Pane container = new Pane();
                container.setPrefHeight(10);
                container.setPrefWidth(BAR_WIDTH);

                Pane barTrack = new Pane();
                barTrack.setPrefHeight(6);
                barTrack.setPrefWidth(BAR_WIDTH);
                barTrack.setLayoutY(2);
                barTrack.setBackground(new Background(
                    new BackgroundFill(Color.web("#332615"), new CornerRadii(3.0), Insets.EMPTY)
                ));

                Pane barFill = new Pane();
                barFill.setPrefHeight(6);
                barFill.setLayoutY(2);
                double fillWidth = Math.min(percentage * (BAR_WIDTH / 100.0), BAR_WIDTH);
                barFill.setPrefWidth(fillWidth);

                Color barColor;
                if ("OK".equals(status)) {
                    barColor = Color.web("#4CAF50");
                } else if ("Low".equals(status)) {
                    barColor = Color.web("#FFA726");
                } else {
                    barColor = Color.web("#F44336");
                }
                barFill.setBackground(new Background(
                    new BackgroundFill(barColor, new CornerRadii(3.0), Insets.EMPTY)
                ));

                javafx.scene.shape.Rectangle lowMarker = new javafx.scene.shape.Rectangle(2, 10);
                lowMarker.setFill(Color.web("#EF9F27"));

                javafx.scene.shape.Rectangle criticalMarker = new javafx.scene.shape.Rectangle(2, 10);
                criticalMarker.setFill(Color.web("#E24B4A"));

                if (maxStock > 0) {
                    lowMarker.setLayoutX((minThreshold / maxStock) * BAR_WIDTH);
                    criticalMarker.setLayoutX((minThreshold * 0.5 / maxStock) * BAR_WIDTH);
                } else {
                    lowMarker.setLayoutX(0);
                    criticalMarker.setLayoutX(0);
                }
                lowMarker.setLayoutY(0);
                criticalMarker.setLayoutY(0);

                container.getChildren().addAll(barTrack, barFill, criticalMarker, lowMarker);
                setGraphic(container);
                setText(null);
            }
        });

        colQuantity.setCellValueFactory(cellData -> {
            double qty = cellData.getValue().getQuantity();
            String formatted = (qty == Math.floor(qty) && !Double.isInfinite(qty))
                ? String.format("%.0f", qty)
                : String.format("%.1f", qty);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        colStatus.setCellFactory(col -> new TableCell<Ingredient, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label pill = new Label(status);
                pill.getStyleClass().add("status-pill");

                if ("OK".equals(status)) {
                    pill.getStyleClass().add("pill-ok");
                } else if ("Low".equals(status)) {
                    pill.getStyleClass().add("pill-low");
                } else if ("Out".equals(status)) {
                    pill.getStyleClass().add("pill-out");
                }

                setGraphic(pill);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<Ingredient, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                int rowIndex = getIndex();
                if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                final Ingredient ingredient = getTableView().getItems().get(rowIndex);
                if (ingredient == null) {
                    setGraphic(null);
                    return;
                }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem addStock = new MenuItem("Add Stock");
                addStock.setOnAction(e -> showAddStockDialog(ingredient));

                MenuItem reduceStock = new MenuItem("Reduce Stock");
                reduceStock.setOnAction(e -> showReduceStockDialog(ingredient));

                MenuItem editThreshold = new MenuItem("Edit Threshold");
                editThreshold.setOnAction(e -> showEditThresholdDialog(ingredient));

                MenuItem delete = new MenuItem("Delete");
                delete.setStyle("-fx-text-fill: #e07070;");
                delete.setOnAction(e -> showDeleteConfirmation(ingredient));

                menuBtn.getItems().addAll(addStock, reduceStock, editThreshold, delete);

                setGraphic(menuBtn);
                setText(null);
            }
        });
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

    private void showAddStockDialog(Ingredient ingredient) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Add Stock");
        dialog.setHeaderText("Add stock to: " + ingredient.getName());

        TextField quantityField = new TextField();
        quantityField.setPromptText("Enter quantity");

        dialog.getDialogPane().setContent(quantityField);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    return Double.parseDouble(quantityField.getText());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(qty -> {
            if (qty != null && qty > 0) {
                double newQty = ingredient.getQuantity() + qty;
                ingredientDAO.updateQuantity(ingredient.getId(), newQty);
                loadInventory();
            }
        });
    }

    private void showReduceStockDialog(Ingredient ingredient) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Reduce Stock");
        dialog.setHeaderText("Reduce stock from: " + ingredient.getName());

        TextField quantityField = new TextField();
        quantityField.setPromptText("Enter quantity to reduce");

        dialog.getDialogPane().setContent(quantityField);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    return Double.parseDouble(quantityField.getText());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(qty -> {
            if (qty != null && qty > 0 && qty <= ingredient.getQuantity()) {
                double newQty = ingredient.getQuantity() - qty;
                ingredientDAO.updateQuantity(ingredient.getId(), newQty);
                loadInventory();
            }
        });
    }

    private void showEditThresholdDialog(Ingredient ingredient) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Edit Threshold");
        dialog.setHeaderText("Edit thresholds for: " + ingredient.getName());

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(8);
        javafx.scene.control.Label minLabel = new javafx.scene.control.Label("Min Threshold:");
        TextField minField = new TextField(String.valueOf(ingredient.getMinThreshold()));
        javafx.scene.control.Label maxLabel = new javafx.scene.control.Label("Max Stock:");
        TextField maxField = new TextField(String.valueOf(ingredient.getMaxStock()));

        vbox.getChildren().addAll(minLabel, minField, maxLabel, maxField);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    return Double.parseDouble(minField.getText());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                double newMin = Double.parseDouble(minField.getText());
                double newMax = Double.parseDouble(maxField.getText());
                if (newMin > 0 && newMax > 0) {
                    ingredientDAO.updateThreshold(ingredient.getId(), newMin);
                    ingredientDAO.updateMaxStock(ingredient.getId(), newMax);
                    loadInventory();
                }
            } catch (NumberFormatException ex) {
                System.err.println("Invalid input: " + ex.getMessage());
            }
        });
    }

    private void showDeleteConfirmation(Ingredient ingredient) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Ingredient");
        alert.setHeaderText("Delete " + ingredient.getName() + "?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                ingredientDAO.delete(ingredient.getId());
                loadInventory();
            }
        });
    }

    @FXML
    private void onAddIngredient() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Add New Ingredient");
        dialog.setHeaderText("Enter ingredient details");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Ingredient name");

        TextField unitField = new TextField();
        unitField.setPromptText("e.g., pcs, g, L");

        TextField quantityField = new TextField();
        quantityField.setPromptText("Initial quantity");

        TextField minThresholdField = new TextField();
        minThresholdField.setPromptText("Low stock alert threshold");

        TextField maxStockField = new TextField();
        maxStockField.setPromptText("Maximum stock capacity");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Unit:"), 0, 1);
        grid.add(unitField, 1, 1);
        grid.add(new Label("Quantity:"), 0, 2);
        grid.add(quantityField, 1, 2);
        grid.add(new Label("Min Threshold:"), 0, 3);
        grid.add(minThresholdField, 1, 3);
        grid.add(new Label("Max Stock:"), 0, 4);
        grid.add(maxStockField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return true;
            }
            return false;
        });

        dialog.showAndWait().filter(result -> result).ifPresent(result -> {
            try {
                String name = nameField.getText().trim();
                String unit = unitField.getText().trim();
                double quantity = Double.parseDouble(quantityField.getText().trim());
                double minThreshold = Double.parseDouble(minThresholdField.getText().trim());
                double maxStock = Double.parseDouble(maxStockField.getText().trim());

                if (!name.isEmpty() && !unit.isEmpty()) {
                    ingredientDAO.insert(name, unit, quantity, minThreshold, maxStock);
                    loadInventory();
                }
            } catch (NumberFormatException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("Invalid input");
                err.setContentText("Please enter valid numbers for quantity, threshold, and max stock.");
                err.showAndWait();
            }
        });
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