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

/**
 * Controller for the Kitchen/Order fulfillment module.
 * Implements a real-time Kanban-style queue for monitoring and managing
 * active orders from 'New' to 'Ready' status.
 */
public class KitchenController extends BaseController {

    @FXML private Label pageTitle;
    @FXML private Label topbarDate;
    @FXML private Label userDisplayName, userDisplayRole, sidebarAvatarText, sidebarUserName, sidebarUserRole;

    @FXML private Button btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff;

    // KANBAN COLUMNS
    @FXML private VBox colNew, colPreparing, colReady;

    // METRICS
    @FXML private Label countNew, countPreparing, countReady;

    private OrderDAO orderDAO = new OrderDAO();
    private StaffDAO staffDAO = new StaffDAO();
    private ScheduledExecutorService timerService;

    /**
     * Initializes the kitchen interface. Sets up instant sync and failsafe polling.
     */
    @FXML
    public void initialize() {
        updateTopbarDate();
        loadUserInfo();
        setActiveNav("Kitchen Queue");
        loadQueue();
        
        // INSTANT SYNC: Refreshes the queue immediately when a new order is placed at the POS
        OrderNotificationService.subscribe(this::loadQueue);
        
        // FAILSAFE POLLING: Periodic refresh every 60 seconds
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

    /**
     * Fetches active orders and sorts them into the appropriate Kanban column based on status.
     */
    public void loadQueue() {
        List<Order> activeOrders = orderDAO.findActiveOrders();
        
        colNew.getChildren().clear();
        colPreparing.getChildren().clear();
        colReady.getChildren().clear();

        int n = 0, p = 0, r = 0;

        for (Order order : activeOrders) {
            String status = order.getStatus();
            
            if ("Cancelled".equals(status)) {
                // Recent cancellations are shown at the top of the NEW column
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

    /**
     * Creates a visual card for a single order.
     * Includes logic for URGENCY detection and timer display.
     */
    private VBox createOrderCard(Order order) {
        VBox card = new VBox(12);
        card.getStyleClass().add("order-card");
        
        if ("Cancelled".equals(order.getStatus())) {
            card.getStyleClass().add("cancelled-card");
        } else if ("Done".equals(order.getStatus())) {
            card.getStyleClass().add("done-card");
        }
        
        // URGENCY LOGIC: Highlights cards if they've been in a state for too long
        long mins = Duration.between(order.getCreatedAt(), LocalDateTime.now()).toMinutes();
        boolean isUrgent = false;

        if ("New".equals(order.getStatus()) && mins >= 5) {
            isUrgent = true; // Unassigned for 5+ mins
        } else if ("Preparing".equals(order.getStatus()) && mins >= 15) {
            isUrgent = true; // Cooking for 15+ mins
        }
        
        if (isUrgent) {
            card.getStyleClass().add("urgent");
        }

        // Card Header: Order # and Type
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

        // Item List: Shows quantities and product names
        VBox itemsContainer = new VBox(8);
        itemsContainer.getStyleClass().add("card-items-container");
        
        List<OrderItem> items = order.getItems();
        if (items == null) {
            items = orderDAO.findItemsByOrderId(order.getId());
        }
        
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

        // Footer: Elapsed time and Warning icon
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

        return card;
    }

    /**
     * Transitions an order to a new status.
     * Handles specialized side-effects like ingredient deduction.
     */
    private void updateStatus(Order order, String newStatus) {
        // CANCELLATION: Releases any stock that was previously reserved
        if ("Cancelled".equals(newStatus) && "New".equals(order.getStatus())) {
            orderDAO.releaseReservationsForOrder(order.getId());
        }

        // PREPARATION: Physically deducts ingredients from the main inventory table
        if ("Preparing".equals(newStatus) && "New".equals(order.getStatus())) {
            orderDAO.deductIngredientsForOrder(order.getId());
        }

        if (orderDAO.updateStatus(order.getId(), newStatus)) {
            // INSTANT BROADCAST: Notifies other modules (Cook Panel, Dashboard) of the change
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