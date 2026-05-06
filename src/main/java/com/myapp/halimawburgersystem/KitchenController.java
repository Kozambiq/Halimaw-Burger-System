package com.myapp.halimawburgersystem;

import com.myapp.dao.OrderDAO;
import com.myapp.dao.StaffDAO;
import com.myapp.model.Order;
import com.myapp.model.OrderItem;
import com.myapp.model.Staff;
import com.myapp.util.OrderNotificationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
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

public class KitchenController extends BaseController {

    @FXML private Label pageTitle;
    @FXML private Label topbarDate;
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
        updateTopbarDate();
        loadUserInfo();
        setActiveNav("Kitchen Queue");
        loadQueue();
        
        // Subscribe to instant notifications from the Cashier and Cook Panel
        OrderNotificationService.subscribe(this::loadQueue);
        
        // Refresh every 60 seconds as a backup (failsafe)
        timerService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        timerService.scheduleAtFixedRate(() -> Platform.runLater(this::loadQueue), 60, 60, TimeUnit.SECONDS);
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

    private void updateTopbarDate() {
        if (topbarDate != null) {
            String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d yyyy"));
            topbarDate.setText(date);
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
                if (secsSinceCancel > 30) continue;

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
        VBox card = new VBox(12);
        card.getStyleClass().add("order-card");
        
        if ("Cancelled".equals(order.getStatus())) {
            card.getStyleClass().add("cancelled-card");
        } else if ("Done".equals(order.getStatus())) {
            card.getStyleClass().add("done-card");
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

        // Header: #OrderNumber | Type
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label orderNum = new Label("#" + String.format("%04d", order.getOrderNumber()));
        orderNum.getStyleClass().add("card-order");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label type = new Label(order.getOrderType().toUpperCase());
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
            qty.getStyleClass().add("item-qty");
            
            Label name = new Label(item.getItemName());
            name.getStyleClass().add("item-name");
            
            Region itemSpacer = new Region();
            HBox.setHgrow(itemSpacer, Priority.ALWAYS);
            
            itemRow.getChildren().addAll(qty, name, itemSpacer);
            itemsContainer.getChildren().add(itemRow);
        }
        
        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            Label notesLabel = new Label(order.getNotes());
            notesLabel.getStyleClass().add("card-note");
            notesLabel.setWrapText(true);
            notesLabel.setMaxWidth(280);
            itemsContainer.getChildren().add(notesLabel);
        }
        
        card.getChildren().add(itemsContainer);

        // Footer: Timer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setSpacing(10);
        
        String timerText = mins + "m ago";
        if ("Cancelled".equals(order.getStatus())) timerText = "CANCELLED";
        else if ("Done".equals(order.getStatus())) timerText = "Ready " + mins + "m";

        Label timer = new Label(timerText + (isUrgent ? " ⚠" : ""));
        timer.getStyleClass().add("card-timer");
        if (isUrgent) timer.getStyleClass().add("urgent-text");

        footer.getChildren().add(timer);
        card.getChildren().add(footer);

        card.setOnMouseClicked(event -> {
            if (!"Cancelled".equals(order.getStatus())) {
                showCancelConfirmation(order);
            }
        });

        return card;
    }

    private void showCancelConfirmation(Order order) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("CANCEL ORDER");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

        Label headerLabel = new Label("Cancel Order #" + String.format("%04d", order.getOrderNumber()) + "?");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30, 40, 30, 40));
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(400);

        VBox infoBox = new VBox(12);
        infoBox.getStyleClass().add("dialog-section-card");
        infoBox.setAlignment(Pos.CENTER);

        Label warningIcon = new Label("⚠");
        warningIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: #ff6b6b;");
        
        Label message = new Label("Are you sure you want to cancel this order? This action will mark the order as cancelled and notify the cashier.");
        message.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 14px; -fx-text-alignment: center;");
        message.setWrapText(true);
        message.setMaxWidth(320);

        infoBox.getChildren().addAll(warningIcon, message);
        content.getChildren().add(infoBox);

        dialog.getDialogPane().setContent(content);
        
        ButtonType cancelBtnType = new ButtonType("KEEP ORDER", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmBtnType = new ButtonType("CANCEL ORDER", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelBtnType, confirmBtnType);

        Button confirmBtn = (Button) dialog.getDialogPane().lookupButton(confirmBtnType);
        confirmBtn.getStyleClass().add("dialog-button-save");
        confirmBtn.setStyle("-fx-background-color: #ff6b6b;"); // Override with red for cancellation

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelBtnType);
        cancelBtn.getStyleClass().add("dialog-button-cancel");

        dialog.setResultConverter(btn -> btn == confirmBtnType);

        dialog.showAndWait().ifPresent(confirmed -> {
            if (confirmed) {
                updateStatus(order, "Cancelled");
            }
        });
    }

    private void updateStatus(Order order, String newStatus) {
        if ("Cancelled".equals(newStatus) && "New".equals(order.getStatus())) {
            orderDAO.releaseReservationsForOrder(order.getId());
        }

        if ("Preparing".equals(newStatus) && "New".equals(order.getStatus())) {
            orderDAO.deductIngredientsForOrder(order.getId());
        }

        if (orderDAO.updateStatus(order.getId(), newStatus)) {
            // Signal to others (Dashboard, Cook Panel) that an order status changed
            OrderNotificationService.broadcastUpdate();
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

    @Override
    protected Button[] getNavButtons() {
        return new Button[] {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
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