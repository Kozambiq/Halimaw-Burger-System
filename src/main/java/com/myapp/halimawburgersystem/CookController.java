package com.myapp.halimawburgersystem;

import com.myapp.dao.OrderDAO;
import com.myapp.model.Order;
import com.myapp.model.OrderItem;
import com.myapp.model.Staff;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.myapp.util.OrderNotificationService;

public class CookController {

    @FXML private Label pageTitle;
    @FXML private Label topbarDate;
    @FXML private Label userDisplayName;
    @FXML private Label userDisplayRole;
    @FXML private Label sidebarAvatarText;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;

    @FXML private Button btnKitchen;

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
        updateTopbarDate();
        loadUserInfo();
        loadQueue();
        
        // Subscribe to instant notifications from the Cashier and Kitchen
        OrderNotificationService.subscribe(this::loadQueue);
        
        timerService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        
        // Polling as a backup (failsafe)
        timerService.scheduleAtFixedRate(() -> {
            try {
                Platform.runLater(this::loadQueue);
            } catch (Exception e) {
                System.err.println("Error in Cook Queue timer: " + e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
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
        
        if ("Preparing".equals(order.getStatus())) {
            card.getStyleClass().add("preparing-pulse");
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
            Label notesLabel = new Label("NOTE: " + order.getNotes().toUpperCase());
            notesLabel.getStyleClass().add("card-note-urgent");
            notesLabel.setWrapText(true);
            notesLabel.setMaxWidth(280);
            itemsContainer.getChildren().add(notesLabel);
        }
        
        card.getChildren().add(itemsContainer);

        // Footer: Timer + Actions
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setSpacing(10);
        
        String timerText = mins + "m ago";
        if ("Cancelled".equals(order.getStatus())) timerText = "CANCELLED";
        else if ("Done".equals(order.getStatus())) timerText = "Ready " + mins + "m";

        Label timer = new Label(timerText + (isUrgent ? " ⚠" : ""));
        timer.getStyleClass().add("card-timer");
        if (isUrgent) timer.getStyleClass().add("urgent-text");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        footer.getChildren().addAll(timer, footerSpacer);

        // Action Buttons
        if (!"Cancelled".equals(order.getStatus())) {
            Button actionBtn = new Button();
            actionBtn.getStyleClass().add("btn-card-action");
            
            if ("New".equals(order.getStatus())) {
                actionBtn.setText("START COOKING");
                actionBtn.getStyleClass().add("btn-card-start");
                
                String warning = orderDAO.checkThresholdWarnings(order.getId());
                final String finalWarning = warning;
                actionBtn.setOnAction(e -> {
                    if (finalWarning != null) {
                        Alert warn = new Alert(Alert.AlertType.WARNING);
                        warn.setTitle("Low Stock Warning");
                        warn.setHeaderText("Starting this order will cause low stock for:");
                        warn.setContentText(finalWarning);
                        warn.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1;");
                        warn.showAndWait();
                    }
                    updateStatus(order, "Preparing");
                });
            } else if ("Preparing".equals(order.getStatus())) {
                actionBtn.setText("MARK AS READY");
                actionBtn.getStyleClass().add("btn-card-ready");
                actionBtn.setOnAction(e -> updateStatus(order, "Done"));
            } else if ("Done".equals(order.getStatus())) {
                actionBtn.setText("COMPLETE");
                actionBtn.getStyleClass().add("btn-card-complete");
                actionBtn.setOnAction(e -> updateStatus(order, "Completed"));
            }
            
            footer.getChildren().add(actionBtn);
        }

        card.getChildren().add(footer);

        return card;
    }

    private void updateStatus(Order order, String newStatus) {
        if ("Cancelled".equals(newStatus) && "New".equals(order.getStatus())) {
            orderDAO.releaseReservationsForOrder(order.getId());
        }

        if ("Preparing".equals(newStatus) && "New".equals(order.getStatus())) {
            orderDAO.deductIngredientsForOrder(order.getId());
        }
        
        if (orderDAO.updateStatus(order.getId(), newStatus)) {
            // Signal to others (Dashboard, Kitchen) that an order status changed
            OrderNotificationService.broadcastUpdate();
            loadQueue();
        }
    }

    @FXML
    private void onNavigate(ActionEvent event) {
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