package com.myapp.halimawburgersystem;

import com.myapp.dao.OrderDAO;
import com.myapp.dao.StaffDAO;
import com.myapp.model.Order;
import com.myapp.model.OrderItem;
import com.myapp.model.Staff;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KitchenController {

    @FXML private Label pageTitle;
    @FXML private Label userDisplayName;
    @FXML private Label userDisplayRole;
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

    @FXML private VBox colNew;
    @FXML private VBox colPreparing;
    @FXML private VBox colReady;

    @FXML private Label countNew;
    @FXML private Label countPreparing;
    @FXML private Label countReady;

    private OrderDAO orderDAO = new OrderDAO();
    private StaffDAO staffDAO = new StaffDAO();
    private ScheduledExecutorService timerService;

    @FXML
    public void initialize() {
        loadUserInfo();
        setActiveNav("Kitchen Queue");
        loadQueue();
        
        // Refresh every 5 seconds for dynamic updates
        timerService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        timerService.scheduleAtFixedRate(() -> Platform.runLater(this::loadQueue), 5, 5, TimeUnit.SECONDS);
    }

    private void loadUserInfo() {
        Staff staff = Main.getCurrentStaff();
        if (staff != null) {
            String initials = staff.getInitials();
            if (userDisplayName != null) userDisplayName.setText(initials);
            if (userDisplayRole != null) userDisplayRole.setText(staff.getRole());
            if (sidebarAvatarText != null) sidebarAvatarText.setText(initials);
            if (sidebarUserName != null) sidebarUserName.setText(staff.getName());
            if (sidebarUserRole != null) sidebarUserRole.setText(staff.getRole());
        }
    }

    public void loadQueue() {
        List<Order> allOrders = orderDAO.findAll();
        
        colNew.getChildren().clear();
        colPreparing.getChildren().clear();
        colReady.getChildren().clear();

        int n = 0, p = 0, r = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Order order : allOrders) {
            String status = order.getStatus();
            
            if ("Cancelled".equals(status)) {
                LocalDateTime cancelledAt = order.getCancelledAt();
                if (cancelledAt == null) {
                    cancelledAt = now;
                }
                
                long secsSinceCancel = Duration.between(cancelledAt, now).getSeconds();
                if (secsSinceCancel > 60) continue; 

                VBox card = createOrderCard(order);
                colNew.getChildren().add(0, card);
                continue;
            }

            if ("New".equals(status)) {
                colNew.getChildren().add(createOrderCard(order));
                n++;
            } else if ("Preparing".equals(status)) {
                colPreparing.getChildren().add(createOrderCard(order));
                p++;
            } else if ("Done".equals(status)) {
                colReady.getChildren().add(createOrderCard(order));
                r++;
            }
        }

        countNew.setText(String.valueOf(n));
        countPreparing.setText(String.valueOf(p));
        countReady.setText(String.valueOf(r));
    }

    private VBox createOrderCard(Order order) {
        VBox card = new VBox(8);
        card.getStyleClass().add("order-card");
        
        if ("Cancelled".equals(order.getStatus())) {
            card.getStyleClass().add("cancelled-card");
        }
        
        long mins = Duration.between(order.getCreatedAt(), LocalDateTime.now()).toMinutes();
        boolean isUrgent = false;

        if ("New".equals(order.getStatus()) && mins >= 5) {
            isUrgent = true;
        } else if ("Preparing".equals(order.getStatus()) && mins >= 15) {
            isUrgent = true;
        }
        
        if (isUrgent) {
            card.getStyleClass().add("urgent");
        }
        
        if ("Done".equals(order.getStatus())) {
            card.setStyle("-fx-border-color: #4a8c3f; -fx-background-color: rgba(74, 140, 63, 0.15);");
        }

        // Header: #OrderNumber | Type
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label orderNum = new Label("#" + String.format("%04d", order.getOrderNumber()));
        orderNum.getStyleClass().add("card-order");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label type = new Label(order.getOrderType());
        type.getStyleClass().add("card-type");
        header.getChildren().addAll(orderNum, spacer, type);

        card.getChildren().add(header);

        VBox itemsContainer = new VBox(8);
        itemsContainer.getStyleClass().add("card-items-container");
        
        List<OrderItem> items = orderDAO.findItemsByOrderId(order.getId());
        for (OrderItem item : items) {
            HBox itemRow = new HBox(8);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            
            Label qty = new Label(item.getQuantity() + "x");
            qty.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #d4591e; -fx-font-weight: bold; -fx-min-width: 30;");
            
            Label name = new Label(item.getItemName());
            name.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 12px;");
            
            Region itemSpacer = new Region();
            HBox.setHgrow(itemSpacer, Priority.ALWAYS);
            
            Label price = new Label("₱" + String.format("%.2f", item.getTotalPrice()));
            price.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #c4a882; -fx-font-size: 12px;");
            
            itemRow.getChildren().addAll(qty, name, itemSpacer, price);
            itemsContainer.getChildren().add(itemRow);
        }
        
        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            Label notesLabel = new Label("Note: " + order.getNotes());
            notesLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #c4a882;");
            itemsContainer.getChildren().add(notesLabel);
        }
        
        card.getChildren().add(itemsContainer);

        // Footer: Timer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        
        String timerText = mins + "m ago";
        if ("Cancelled".equals(order.getStatus())) timerText = "CANCELLED";
        else if ("Done".equals(order.getStatus())) timerText = "Ready " + mins + "m";

        Label timer = new Label(timerText + (isUrgent ? " ⚠" : ""));
        timer.getStyleClass().add("card-timer");
        if (isUrgent) timer.getStyleClass().add("urgent-text");
        if ("Done".equals(order.getStatus())) timer.setStyle("-fx-text-fill: #7ec470;");
        if ("Cancelled".equals(order.getStatus())) timer.setStyle("-fx-text-fill: #e07070;");

        footer.getChildren().add(timer);
        card.getChildren().add(footer);

        return card;
    }

    private void updateStatus(Order order, String newStatus) {
        if (orderDAO.updateStatus(order.getId(), newStatus)) {
            loadQueue();
        }
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().trim();
        try {
            if (text.contains("Dashboard")) Main.showDashboard();
            else if (text.contains("Orders")) Main.showOrders();
            else if (text.contains("Kitchen")) return;
            else if (text.contains("Menu Items")) Main.showMenuItems();
            else if (text.contains("Combos")) Main.showCombos();
            else if (text.contains("Inventory")) Main.showInventory();
            else if (text.contains("Sales")) Main.showSalesReport();
            else if (text.contains("Staff")) Main.showStaff();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setActiveNav(String navName) {
        Button[] buttons = {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
        for (Button btn : buttons) {
            if (btn != null) btn.getStyleClass().remove("nav-item-active");
        }
        if (btnKitchen != null) btnKitchen.getStyleClass().add("nav-item-active");
    }

    @FXML
    private void onLogout(ActionEvent event) {
        try {
            if (timerService != null) timerService.shutdown();
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}