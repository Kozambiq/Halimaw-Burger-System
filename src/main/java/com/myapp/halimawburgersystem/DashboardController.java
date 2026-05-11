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

import com.myapp.util.OrderNotificationService;

/**
 * Controller for the Main Dashboard module.
 * Acts as a centralized command center, aggregating data from all system modules
 * to display real-time revenue stats, order trends, and critical stock alerts.
 */
public class DashboardController extends BaseController {

    @FXML private Label pageTitle;
    @FXML private Label topbarDate;

    @FXML private Label sidebarAvatarText;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;

    @FXML private Button btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff;

    @FXML private Label lblRevenue;
    @FXML private Label lblRevenueDelta;
    @FXML private Label lblOrders;
    @FXML private Label lblOrdersDelta;

    @FXML private TableView<Order> recentOrdersTable;
    @FXML private TableColumn<Order, Integer> colOrderNumber;
    @FXML private TableColumn<Order, String> colItems;
    @FXML private TableColumn<Order, String> colType;
    @FXML private TableColumn<Order, String> colStatus;

    @FXML private VBox topItemsContainer;
    @FXML private VBox lowStockContainer;
    @FXML private VBox activeStaffContainer;

    private OrderDAO orderDAO = new OrderDAO();
    private MenuItemDAO menuItemDAO = new MenuItemDAO();
    private IngredientDAO ingredientDAO = new IngredientDAO();
    private StaffDAO staffDAO = new StaffDAO();

    private ObservableList<Order> recentOrdersList = FXCollections.observableArrayList();
    private ScheduledExecutorService refreshService;

    // DEBOUNCE STATE: Prevents rapid-fire refreshes during high-volume order bursts
    private java.util.concurrent.atomic.AtomicLong lastRefreshRequest = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * Initializes the dashboard. Sets up metrics, recent orders table,
     * and establishes the real-time synchronization strategy with debouncing.
     */
    @FXML
    public void initialize() {
        updateTopbarDate();
        setupUserInfo();
        setupTableColumns();
        loadDashboardData();

        // SYNC STRATEGY: Instant updates with a 300ms debounce buffer
        OrderNotificationService.subscribe(() -> {
            long now = System.currentTimeMillis();
            lastRefreshRequest.set(now);
            
            // Wait 300ms before actually refreshing to catch multiple rapid updates
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                if (lastRefreshRequest.get() == now) {
                    Platform.runLater(this::loadDashboardData);
                }
            }, 300, TimeUnit.MILLISECONDS);
        });

        // FAILSAFE POLLING: Periodic refresh every 60 seconds
        refreshService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        refreshService.scheduleAtFixedRate(() -> Platform.runLater(this::loadDashboardData), 60, 60, TimeUnit.SECONDS);
    }

    private void setupUserInfo() {
        Staff staff = Main.getCurrentStaff();
        if (staff != null) {
            sidebarAvatarText.setText(staff.getInitials());
            sidebarUserName.setText(staff.getName());
            sidebarUserRole.setText(staff.getRole());
        }
    }

    private void updateTopbarDate() {
        if (topbarDate != null) {
            String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d yyyy"));
            topbarDate.setText(date);
        }
    }

    /**
     * Configures the Recent Orders table with status-specific badges.
     */
    private void setupTableColumns() {
        colOrderNumber.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getOrderNumber()));
        colOrderNumber.setCellFactory(column -> new TableCell<Order, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
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
                if (empty || item == null) setText(null);
                else {
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
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-text-fill: #c4a882;");
                }
            }
        });

        // STATUS BADGE LOGIC: Dynamic color-coding based on current order state
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
    }

    /**
     * Aggregates data from multiple DAOs in a single background Task.
     * Prevents UI freezing by offloading heavy relational queries to a separate thread.
     */
    public void loadDashboardData() {
        javafx.concurrent.Task<DashboardData> loadTask = new javafx.concurrent.Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                DashboardData data = new DashboardData();
                // REVENUE AGGREGATION
                data.todayRevenue = orderDAO.getTodayRevenue();
                data.yesterdayRevenue = orderDAO.getYesterdayRevenue();
                data.todayOrders = orderDAO.getTodayOrderCount();
                data.yesterdayOrders = orderDAO.getYesterdayOrderCount();
                
                // ORDER HISTORY (Recent 10)
                LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
                LocalDateTime end = LocalDateTime.now();
                List<Order> orders = orderDAO.findByFilters(null, start, end);
                data.recentOrders = orders.size() > 10 ? orders.subList(0, 10) : orders;
                
                // PRODUCT & STOCK ANALYTICS
                data.topSellingItems = menuItemDAO.getTopSellingItems(5);
                data.criticalStock = ingredientDAO.findCriticalStock();
                data.lowStock = ingredientDAO.findLowStock();
                data.activeStaff = staffDAO.findActiveStaff();
                
                return data;
            }
        };

        loadTask.setOnSucceeded(e -> {
            DashboardData data = loadTask.getValue();
            updateRevenueStats(data);
            recentOrdersList.setAll(data.recentOrders);
            updateTopItems(data.topSellingItems);
            updateStockAlerts(data.criticalStock, data.lowStock);
            updateActiveStaff(data.activeStaff);
        });

        new Thread(loadTask).start();
    }

    private static class DashboardData {
        double todayRevenue, yesterdayRevenue;
        int todayOrders, yesterdayOrders;
        List<Order> recentOrders;
        List<MenuItemDAO.TopSellingItem> topSellingItems;
        List<Ingredient> criticalStock, lowStock;
        List<Staff> activeStaff;
    }

    /**
     * Calculates and displays current revenue stats along with percentage change
     * compared to the previous day. Includes visual indicators (arrows) for trend.
     */
    private void updateRevenueStats(DashboardData data) {
        lblRevenue.setText("₱" + String.format("%,.0f", data.todayRevenue));

        if (data.yesterdayRevenue > 0) {
            double revenueChange = ((data.todayRevenue - data.yesterdayRevenue) / data.yesterdayRevenue) * 100;
            if (revenueChange >= 0) {
                lblRevenueDelta.setText("↑ +" + String.format("%.0f", revenueChange) + "% vs yesterday");
                lblRevenueDelta.getStyleClass().removeAll("delta-down");
                lblRevenueDelta.getStyleClass().add("delta-up");
            } else {
                lblRevenueDelta.setText("↓ " + String.format("%.0f", Math.abs(revenueChange)) + "% vs yesterday");
                lblRevenueDelta.getStyleClass().removeAll("delta-up");
                lblRevenueDelta.getStyleClass().add("delta-down");
            }
        } else {
            lblRevenueDelta.setText("");
        }

        lblOrders.setText(String.valueOf(data.todayOrders));

        if (data.yesterdayOrders > 0) {
            double ordersChange = ((double)(data.todayOrders - data.yesterdayOrders) / data.yesterdayOrders) * 100;
            if (ordersChange >= 0) {
                lblOrdersDelta.setText("↑ +" + String.format("%.0f", ordersChange) + "% vs yesterday");
                lblOrdersDelta.getStyleClass().removeAll("delta-down");
                lblOrdersDelta.getStyleClass().add("delta-up");
            } else {
                lblOrdersDelta.setText("↓ " + String.format("%.0f", Math.abs(ordersChange)) + "% vs yesterday");
                lblOrdersDelta.getStyleClass().removeAll("delta-up");
                lblOrdersDelta.getStyleClass().add("delta-down");
            }
        } else {
            lblOrdersDelta.setText("");
        }
    }

    /**
     * Renders a list of top selling items using progress bars to represent
     * sales volume relative to the most popular item.
     */
    private void updateTopItems(List<MenuItemDAO.TopSellingItem> items) {
        topItemsContainer.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("No sales data yet");
            empty.setStyle("-fx-text-fill: #8a7055; -fx-font-size: 12px; -fx-font-style: italic;");
            topItemsContainer.getChildren().add(empty);
            return;
        }

        int maxSold = items.get(0).getTotalSold();

        for (MenuItemDAO.TopSellingItem item : items) {
            VBox itemRow = new VBox(4);
            HBox labels = new HBox();
            Label name = new Label(item.getName());
            name.getStyleClass().add("sales-progress-label");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label sold = new Label(item.getTotalSold() + " sold");
            sold.getStyleClass().add("sales-progress-value");
            labels.getChildren().addAll(name, spacer, sold);

            javafx.scene.control.ProgressBar pb = new javafx.scene.control.ProgressBar((double) item.getTotalSold() / maxSold);
            pb.getStyleClass().add("sales-progress-bar");
            pb.setMaxWidth(Double.MAX_VALUE);

            itemRow.getChildren().addAll(labels, pb);
            topItemsContainer.getChildren().add(itemRow);
        }
    }

    /**
     * Renders inventory alerts. Prioritizes 'Critical' (Out of stock) over 'Low Stock'.
     */
    private void updateStockAlerts(List<Ingredient> critical, List<Ingredient> low) {
        lowStockContainer.getChildren().clear();
        int maxDisplay = 5;
        int count = 0;

        for (Ingredient ing : critical) {
            if (count >= maxDisplay) break;
            lowStockContainer.getChildren().add(createStockRow(ing.getName(), "OUT", "out"));
            count++;
        }

        for (Ingredient ing : low) {
            if (count >= maxDisplay) break;
            lowStockContainer.getChildren().add(createStockRow(ing.getName(), (int)ing.getQuantity() + " " + ing.getUnit(), "low"));
            count++;
        }

        if (lowStockContainer.getChildren().isEmpty()) {
            Label empty = new Label("No low stock alerts");
            empty.setStyle("-fx-text-fill: #8a7055; -fx-font-size: 12px; -fx-font-style: italic;");
            lowStockContainer.getChildren().add(empty);
        }
    }

    private HBox createStockRow(String name, String qty, String status) {
        HBox tile = new HBox();
        tile.setAlignment(Pos.CENTER_LEFT);
        tile.getStyleClass().add("activity-tile");
        if ("out".equals(status)) tile.getStyleClass().add("activity-tile-out");
        else if ("low".equals(status)) tile.getStyleClass().add("activity-tile-low");

        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-font-weight: 600;");
        Label statusLabel = new Label("out".equals(status) ? "Critical" : "Running Low");
        statusLabel.setStyle("-fx-text-fill: " + ("out".equals(status) ? "#e07070" : "#e8b84b") + "; -fx-font-size: 10px;");
        info.getChildren().addAll(nameLabel, statusLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label qtyLabel = new Label(qty);
        qtyLabel.getStyleClass().add("sales-progress-value");

        tile.getChildren().addAll(info, spacer, qtyLabel);
        return tile;
    }

    private void updateActiveStaff(List<Staff> activeStaff) {
        activeStaffContainer.getChildren().clear();
        int maxDisplay = 5;
        int count = 0;

        for (Staff staff : activeStaff) {
            if (count >= maxDisplay) break;
            activeStaffContainer.getChildren().add(createStaffChip(staff));
            count++;
        }

        if (activeStaffContainer.getChildren().isEmpty()) {
            Label empty = new Label("No active staff");
            empty.setStyle("-fx-text-fill: #8a7055; -fx-font-size: 12px; -fx-font-style: italic;");
            activeStaffContainer.getChildren().add(empty);
        }
    }

    private HBox createStaffChip(Staff staff) {
        HBox tile = new HBox();
        tile.setAlignment(Pos.CENTER_LEFT);
        tile.setSpacing(12);
        tile.getStyleClass().add("activity-tile");

        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: rgba(200, 80, 10, 0.1); -fx-border-color: #c8500a; -fx-border-width: 1; -fx-background-radius: 50; -fx-border-radius: 50; -fx-min-width: 32; -fx-min-height: 32;");
        Label avatarText = new Label(staff.getInitials());
        avatarText.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #c8500a;");
        avatar.getChildren().add(avatarText);

        VBox info = new VBox(2);
        Label name = new Label(staff.getName());
        name.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-font-weight: 600;");
        Label role = new Label(staff.getRole().toUpperCase());
        role.setStyle("-fx-text-fill: #8a7055; -fx-font-size: 9px; -fx-font-weight: 800; -fx-letter-spacing: 0.05em;");
        info.getChildren().addAll(name, role);

        tile.getChildren().addAll(avatar, info);
        return tile;
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

    @Override
    protected Button[] getNavButtons() {
        return new Button[] {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
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