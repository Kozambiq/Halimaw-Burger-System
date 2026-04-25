package com.myapp.halimawburgersystem;

import com.myapp.dao.IngredientDAO;
import com.myapp.dao.MenuItemDAO;
import com.myapp.dao.OrderDAO;
import com.myapp.dao.StaffDAO;
import com.myapp.model.Ingredient;
import com.myapp.model.Order;
import com.myapp.model.Staff;
import com.myapp.model.MenuItemModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DashboardController {

    @FXML private Label pageTitle;

    @FXML private Label sidebarAvatarText;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;

    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    @FXML private Label lblRevenue;
    @FXML private Label lblRevenueDelta;
    @FXML private Label lblOrders;
    @FXML private Label lblOrdersDelta;

    @FXML private TableView<Order> recentOrdersTable;
    @FXML private TableColumn<Order, Integer> colOrderNumber;
    @FXML private TableColumn<Order, String> colItems;
    @FXML private TableColumn<Order, String> colType;
    @FXML private TableColumn<Order, String> colStatus;

    @FXML private TableView<MenuItemDAO.TopSellingItem> topItemsTable;
    @FXML private TableColumn<MenuItemDAO.TopSellingItem, String> colItemName;
    @FXML private TableColumn<MenuItemDAO.TopSellingItem, Integer> colItemSold;

    @FXML private VBox lowStockContainer;
    @FXML private VBox activeStaffContainer;

    private OrderDAO orderDAO = new OrderDAO();
    private MenuItemDAO menuItemDAO = new MenuItemDAO();
    private IngredientDAO ingredientDAO = new IngredientDAO();
    private StaffDAO staffDAO = new StaffDAO();

    private ObservableList<Order> recentOrdersList = FXCollections.observableArrayList();
    private ObservableList<MenuItemDAO.TopSellingItem> topItemsList = FXCollections.observableArrayList();
    private ScheduledExecutorService refreshService;

    @FXML
    public void initialize() {
        setupUserInfo();
        setupTableColumns();
        loadDashboardData();

        refreshService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        refreshService.scheduleAtFixedRate(() -> Platform.runLater(this::loadRecentOrders), 10, 10, TimeUnit.SECONDS);
    }

    private void setupUserInfo() {
        Staff staff = Main.getCurrentStaff();
        if (staff != null) {
            sidebarAvatarText.setText(staff.getInitials());
            sidebarUserName.setText(staff.getName());
            sidebarUserRole.setText(staff.getRole());
        }
    }

    private void setupTableColumns() {
        colOrderNumber.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getOrderNumber()));
        colOrderNumber.setCellFactory(column -> new TableCell<Order, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("#" + String.format("%04d", item));
                    setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #d4591e; -fx-font-weight: bold;");
                }
            }
        });

        colItems.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getItemsSummary() != null ? data.getValue().getItemsSummary() : ""));
        colItems.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setWrapText(true);
                    setStyle("-fx-text-fill: #c4a882;");
                }
            }
        });

        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getOrderType() != null ? data.getValue().getOrderType() : ""));
        colType.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #c4a882;");
                }
            }
        });

        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus() != null ? data.getValue().getStatus() : ""));
        colStatus.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    badge.getStyleClass().add("badge");
                    switch (item) {
                        case "New": badge.getStyleClass().add("badge-amber"); break;
                        case "Preparing": badge.getStyleClass().add("badge-blue"); break;
                        case "Done": case "Completed": badge.getStyleClass().add("badge-green"); break;
                        case "Cancelled": badge.getStyleClass().add("badge-red"); break;
                        default: badge.getStyleClass().add("badge-green");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        recentOrdersTable.setItems(recentOrdersList);
        recentOrdersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colItemName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        colItemName.setCellFactory(column -> new TableCell<MenuItemDAO.TopSellingItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #f5ede0; -fx-font-weight: 500;");
                }
            }
        });

        colItemSold.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getTotalSold()));
        colItemSold.setCellFactory(column -> new TableCell<MenuItemDAO.TopSellingItem, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #c8500a;");
                }
            }
        });

        topItemsTable.setItems(topItemsList);
        topItemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadDashboardData() {
        loadRevenueStats();
        loadRecentOrders();
        loadTopItems();
        loadLowStock();
        loadActiveStaff();
    }

    private void loadRevenueStats() {
        double todayRevenue = orderDAO.getTodayRevenue();
        double yesterdayRevenue = orderDAO.getYesterdayRevenue();
        int todayOrders = orderDAO.getTodayOrderCount();
        int yesterdayOrders = orderDAO.getYesterdayOrderCount();

        lblRevenue.setText("₱" + String.format("%,.0f", todayRevenue));

        if (yesterdayRevenue > 0) {
            double revenueChange = ((todayRevenue - yesterdayRevenue) / yesterdayRevenue) * 100;
            if (revenueChange >= 0) {
                lblRevenueDelta.setText("↑ +" + String.format("%.0f", revenueChange) + "% vs yesterday");
                lblRevenueDelta.getStyleClass().removeAll("delta-down");
                lblRevenueDelta.getStyleClass().add("delta-up");
            } else {
                lblRevenueDelta.setText("↓ " + String.format("%.0f", revenueChange) + "% vs yesterday");
                lblRevenueDelta.getStyleClass().removeAll("delta-up");
                lblRevenueDelta.getStyleClass().add("delta-down");
            }
        } else {
            lblRevenueDelta.setText("");
        }

        lblOrders.setText(String.valueOf(todayOrders));

        if (yesterdayOrders > 0) {
            double ordersChange = ((double)(todayOrders - yesterdayOrders) / yesterdayOrders) * 100;
            if (ordersChange >= 0) {
                lblOrdersDelta.setText("↑ +" + String.format("%.0f", ordersChange) + "% vs yesterday");
                lblOrdersDelta.getStyleClass().removeAll("delta-down");
                lblOrdersDelta.getStyleClass().add("delta-up");
            } else {
                lblOrdersDelta.setText("↓ " + String.format("%.0f", ordersChange) + "% vs yesterday");
                lblOrdersDelta.getStyleClass().removeAll("delta-up");
                lblOrdersDelta.getStyleClass().add("delta-down");
            }
        } else {
            lblOrdersDelta.setText("");
        }
    }

    private void loadRecentOrders() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = LocalDateTime.now();
        List<Order> orders = orderDAO.findByFilters(null, start, end);
        recentOrdersList.setAll(orders.size() > 10 ? orders.subList(0, 10) : orders);
    }

    private void loadTopItems() {
        List<MenuItemDAO.TopSellingItem> items = menuItemDAO.getTopSellingItems(5);
        topItemsList.setAll(items);
    }

    private void loadLowStock() {
        lowStockContainer.getChildren().clear();
        List<Ingredient> critical = ingredientDAO.findCriticalStock();
        List<Ingredient> low = ingredientDAO.findLowStock();

        int maxDisplay = 5;
        int count = 0;

        for (Ingredient ing : critical) {
            if (count >= maxDisplay) break;
            HBox row = createStockRow(ing.getName(), "0 " + ing.getUnit(), "out");
            lowStockContainer.getChildren().add(row);
            count++;
        }

        for (Ingredient ing : low) {
            if (count >= maxDisplay) break;
            HBox row = createStockRow(ing.getName(), (int)ing.getQuantity() + " " + ing.getUnit(), "low");
            lowStockContainer.getChildren().add(row);
            count++;
        }

        if (lowStockContainer.getChildren().isEmpty()) {
            Label empty = new Label("No low stock alerts");
            empty.setStyle("-fx-text-fill: #8a7055; -fx-font-size: 12px; -fx-font-style: italic;");
            lowStockContainer.getChildren().add(empty);
        }
    }

    private HBox createStockRow(String name, String qty, String status) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(8);
        row.setStyle("-fx-border-color: transparent transparent #5c4828 transparent; -fx-border-width: 0 0 1 0;");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #c4a882; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label qtyLabel = new Label(qty);
        qtyLabel.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-font-size: 11px;");

        if ("out".equals(status)) {
            nameLabel.setStyle("-fx-text-fill: #e07070;");
            qtyLabel.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #e07070;");
        } else if ("low".equals(status)) {
            nameLabel.setStyle("-fx-text-fill: #e8b84b;");
            qtyLabel.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-font-size: 11px; -fx-text-fill: #e8b84b;");
        }

        row.getChildren().addAll(nameLabel, spacer, qtyLabel);
        return row;
    }

    private void loadActiveStaff() {
        activeStaffContainer.getChildren().clear();
        List<Staff> activeStaff = staffDAO.findActiveStaff();

        int maxDisplay = 5;
        int count = 0;

        for (Staff staff : activeStaff) {
            if (count >= maxDisplay) break;
            HBox chip = createStaffChip(staff);
            activeStaffContainer.getChildren().add(chip);
            count++;
        }

        if (activeStaffContainer.getChildren().isEmpty()) {
            Label empty = new Label("No active staff");
            empty.setStyle("-fx-text-fill: #8a7055; -fx-font-size: 12px; -fx-font-style: italic;");
            activeStaffContainer.getChildren().add(empty);
        }
    }

    private HBox createStaffChip(Staff staff) {
        HBox chip = new HBox();
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setSpacing(10);

        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: rgba(212, 89, 30, 0.12); -fx-border-color: #5c4828; -fx-border-width: 1; -fx-background-radius: 14; -fx-border-radius: 14; -fx-min-width: 28; -fx-min-height: 28; -fx-max-width: 28; -fx-max-height: 28;");
        Label avatarText = new Label(staff.getInitials());
        avatarText.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #c8500a;");
        avatar.getChildren().add(avatarText);

        VBox info = new VBox();
        info.setSpacing(2);

        Label name = new Label(staff.getName());
        name.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 12px; -fx-font-weight: 500;");

        Label role = new Label(staff.getRole());
        role.setStyle("-fx-text-fill: #8a7055; -fx-font-size: 10px;");

        info.getChildren().addAll(name, role);

        chip.getChildren().addAll(avatar, info);
        return chip;
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        if (refreshService != null) refreshService.shutdown();
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().trim();

        clearAllHighlights();
        clicked.getStyleClass().add("nav-item-active");

        if (pageTitle != null) {
            pageTitle.setText(text);
        }

        if (text.contains("Inventory")) {
            try {
                Main.showInventory();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (text.contains("Menu Items")) {
            try {
                Main.showMenuItems();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (text.contains("Combos")) {
            try {
                Main.showCombos();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (text.contains("Staff")) {
            try {
                Main.showStaff();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (text.contains("Kitchen")) {
            try {
                Main.showKitchen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (text.contains("Orders")) {
            try {
                Main.showOrders();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (text.contains("Sales")) {
            try {
                Main.showSalesReport();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    @FXML
    private void onLogout(ActionEvent event) {
        try {
            if (refreshService != null) refreshService.shutdown();
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}