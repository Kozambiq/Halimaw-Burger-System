package com.myapp.halimawburgersystem;

import com.myapp.dao.IngredientDAO;
import com.myapp.model.Ingredient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
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

                Pane barContainer = new Pane();
                barContainer.setPrefHeight(6);
                barContainer.setPrefWidth(180);
                barContainer.setMaxWidth(180);
                barContainer.setBackground(new Background(
                    new BackgroundFill(Color.web("#3a2a15"), new CornerRadii(3.0), Insets.EMPTY)
                ));

                Pane barFill = new Pane();
                barFill.setPrefHeight(6);
                barFill.setPrefWidth(percentage * 1.8);
                barFill.setMaxWidth(180);
                barFill.setLayoutY(0);

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

                barContainer.getChildren().add(barFill);

                setGraphic(barContainer);
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

                HBox box = new HBox(4);
                box.setAlignment(Pos.CENTER);

                Button btnAdd = new Button();
                btnAdd.getStyleClass().add("btn-action-add");
                SVGPath plusIcon = new SVGPath();
                plusIcon.setContent("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
                plusIcon.getStyleClass().add("action-svg");
                btnAdd.setGraphic(plusIcon);

                Button btnMinus = new Button();
                btnMinus.getStyleClass().add("btn-action-minus");
                SVGPath minusIcon = new SVGPath();
                minusIcon.setContent("M19 13H5v-2h14v2z");
                minusIcon.getStyleClass().add("action-svg");
                btnMinus.setGraphic(minusIcon);

                Button btnDelete = new Button();
                btnDelete.getStyleClass().add("btn-action-delete");
                SVGPath trashIcon = new SVGPath();
                trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
                trashIcon.getStyleClass().add("action-svg");
                btnDelete.setGraphic(trashIcon);

                box.getChildren().addAll(btnAdd, btnMinus, btnDelete);
                setGraphic(box);
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