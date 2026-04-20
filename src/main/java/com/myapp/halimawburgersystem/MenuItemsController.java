package com.myapp.halimawburgersystem;

import com.myapp.dao.MenuItemDAO;
import com.myapp.model.MenuItemModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.MenuItem;

import java.util.List;

public class MenuItemsController {

    @FXML private Label lblTotal;
    @FXML private Label lblAvailable;
    @FXML private Label lblLowStock;
    @FXML private Label lblOutOfStock;
    @FXML private TableView<MenuItemModel> menuItemsTable;
    @FXML private TableColumn<MenuItemModel, String> colName;
    @FXML private TableColumn<MenuItemModel, String> colCategory;
    @FXML private TableColumn<MenuItemModel, String> colPrice;
    @FXML private TableColumn<MenuItemModel, String> colAvailability;
    @FXML private TableColumn<MenuItemModel, String> colActions;

    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    private MenuItemDAO menuItemDAO = new MenuItemDAO();
    private boolean alreadyLoaded = false;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        setActiveNav("Menu Items");
        setupTableColumns();
        loadMenuItems();
    }

    public void setActiveNav(String navName) {
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
        colName.setCellValueFactory(new PropertyValueFactory<MenuItemModel, String>("name"));

        colCategory.setCellValueFactory(new PropertyValueFactory<MenuItemModel, String>("category"));

        colPrice.setCellValueFactory(new PropertyValueFactory<MenuItemModel, String>("formattedPrice"));

        menuItemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colAvailability.setCellValueFactory(new PropertyValueFactory<MenuItemModel, String>("availability"));
        colAvailability.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
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

                if ("Available".equals(status)) {
                    pill.getStyleClass().add("pill-ok");
                } else if ("Low Stock".equals(status)) {
                    pill.getStyleClass().add("pill-low");
                } else if ("Out of Stock".equals(status)) {
                    pill.getStyleClass().add("pill-out");
                }

                setGraphic(pill);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem edit = new MenuItem("Edit");
                edit.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");

                MenuItem availability = new MenuItem("Availability");
                availability.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");

                MenuItem delete = new MenuItem("Delete");
                delete.setStyle("-fx-text-fill: #e07070; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");

                menuBtn.getItems().addAll(edit, availability, delete);

                setGraphic(menuBtn);
                setText(null);
            }
        });
    }

    private void loadMenuItems() {
        try {
            int total = menuItemDAO.getTotalCount();
            int available = menuItemDAO.getAvailableCount();
            int low = menuItemDAO.getLowStockCount();
            int out = menuItemDAO.getOutOfStockCount();
            List<MenuItemModel> items = menuItemDAO.findAllWithIngredientStatus();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblAvailable.setText(String.valueOf(available));
                lblLowStock.setText(String.valueOf(low));
                lblOutOfStock.setText(String.valueOf(out));
                menuItemsTable.setItems(FXCollections.observableArrayList(items));
            });
        } catch (Exception e) {
            System.err.println("Error loading menu items: " + e.getMessage());
        }
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
        System.out.println("Orders - Coming soon");
    }

    @FXML
    private void onNavigateKitchen() {
        System.out.println("Kitchen Queue - Coming soon");
    }

    @FXML
    private void onNavigateMenuItems() {
        try {
            Main.showMenuItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateCombos() {
        System.out.println("Combos & Promos - Coming soon");
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
        System.out.println("Sales Reports - Coming soon");
    }

    @FXML
    private void onNavigateStaff() {
        System.out.println("Staff - Coming soon");
    }
}