package com.myapp.halimawburgersystem;

import com.myapp.dao.OrderDAO;
import com.myapp.model.Order;
import com.myapp.model.Staff;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Duration;
import java.time.LocalDateTime;
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

        for (Order order : allOrders) {
            String status = order.getStatus();
            if ("New".equals(status)) {
                colNew.getChildren().add(createOrderCard(order));
                n++;
            } else if ("Preparing".equals(status)) {
                colPreparing.getChildren().add(createOrderCard(order));
                p++;
            } else if ("Done".equals(status)) {
                // Done in DB maps to "Ready" in Kanban logic if not yet picked up
                // For now, let's treat "Done" as "Ready" for the kitchen perspective
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
        
        long mins = Duration.between(order.getCreatedAt(), LocalDateTime.now()).toMinutes();
        boolean isUrgent = false;

        // Warning Logic: New > 5m, Preparing > 15m
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

        // Notes/Items
        Label items = new Label(order.getNotes() != null ? order.getNotes() : "No details");
        items.getStyleClass().add("card-items");
        items.setWrapText(true);

        // Footer: Timer | Action Button
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label timer = new Label(mins + "m ago" + (isUrgent ? " ⚠" : ""));
        timer.getStyleClass().add("card-timer");
        if (isUrgent) timer.getStyleClass().add("urgent-text");
        if ("Done".equals(order.getStatus())) {
            timer.setText("Ready " + mins + "m");
            timer.setStyle("-fx-text-fill: #7ec470;");
        }

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        
        Button actionBtn = new Button();
        actionBtn.getStyleClass().add("btn-card");
        
        if ("New".equals(order.getStatus())) {
            actionBtn.setText("Start");
            actionBtn.setOnAction(e -> updateStatus(order, "Preparing"));
        } else if ("Preparing".equals(order.getStatus())) {
            actionBtn.setText("Mark Ready");
            actionBtn.setOnAction(e -> updateStatus(order, "Done"));
        } else if ("Done".equals(order.getStatus())) {
            actionBtn.setText("Complete");
            actionBtn.setStyle("-fx-background-color: #4a8c3f;");
            actionBtn.setOnAction(e -> updateStatus(order, "Completed")); 
        }

        footer.getChildren().addAll(timer, footerSpacer, actionBtn);
        card.getChildren().addAll(header, items, footer);
        
        return card;
    }

    private void updateStatus(Order order, String newStatus) {
        if (orderDAO.updateStatus(order.getId(), newStatus)) {
            loadQueue();
        }
    }

    private void refreshTimers() {
        loadQueue(); // Simple way to refresh UI, or we could surgically update labels
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().trim();
        try {
            if (text.contains("Dashboard")) Main.showDashboard();
            else if (text.contains("Orders")) Main.showOrders();
            else if (text.contains("Menu Items")) Main.showMenuItems();
            else if (text.contains("Combos")) Main.showCombos();
            else if (text.contains("Inventory")) Main.showInventory();
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