package com.myapp.halimawburgersystem;

import com.myapp.dao.OrderDAO;
import com.myapp.dao.StaffDAO;
import com.myapp.model.Order;
import com.myapp.model.OrderItem;
import com.myapp.model.Staff;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrdersController {

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

    @FXML private ComboBox<String> cmbOrderType;
    @FXML private ComboBox<String> cmbDateFilter;
    @FXML private HBox datePickerBox;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;

    @FXML private Label lblTotalOrders;
    @FXML private Label lblPreparing;
    @FXML private Label lblCompleted;
    @FXML private Label lblCancelled;

    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, Integer> colOrderNumber;
    @FXML private TableColumn<Order, String> colItems;
    @FXML private TableColumn<Order, String> colType;
    @FXML private TableColumn<Order, Double> colTotal;
    @FXML private TableColumn<Order, LocalDateTime> colTime;
    @FXML private TableColumn<Order, String> colStatus;

    // Order Details Modal Fields
    @FXML private VBox detailsOverlay;
    @FXML private Label detailsOrderNum;
    @FXML private Label detailsDate;
    @FXML private VBox detailsItemsContainer;
    @FXML private Label detailsType;
    @FXML private Label detailsPayment;
    @FXML private Label detailsRef;
    @FXML private Label detailsRefLabel;
    @FXML private Label detailsStaff;
    @FXML private VBox detailsNotesBox;
    @FXML private Label detailsNotes;
    @FXML private Label detailsSubtotal;
    @FXML private Label detailsDiscount;
    @FXML private Label detailsTotal;
    @FXML private Label detailsStatus;
    @FXML private Button btnPrintOrder;
    @FXML private Button btnCancelOrder;

    private OrderDAO orderDAO = new OrderDAO();
    private StaffDAO staffDAO = new StaffDAO();
    private ObservableList<Order> ordersList = FXCollections.observableArrayList();
    private Order selectedOrder;
    private ScheduledExecutorService refreshService;

    @FXML
    public void initialize() {
        setupUserInfo();
        setupFilters();
        setupTableColumns();
        loadOrders();
        
        // Dynamic Refresh
        refreshService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        refreshService.scheduleAtFixedRate(() -> Platform.runLater(this::loadOrders), 10, 10, TimeUnit.SECONDS);
    }

    private void setupUserInfo() {
        Staff staff = Main.getCurrentStaff();
        if (staff != null) {
            String initials = staff.getInitials();
            userDisplayName.setText(initials);
            userDisplayRole.setText(staff.getRole());
            sidebarAvatarText.setText(initials);
            sidebarUserName.setText(staff.getName());
            sidebarUserRole.setText(staff.getRole());
        }
    }

    private void setupFilters() {
        cmbOrderType.setItems(FXCollections.observableArrayList("All Types", "Dine-in", "Takeout"));
        cmbOrderType.setValue("All Types");

        cmbDateFilter.setItems(FXCollections.observableArrayList("Today", "Yesterday", "Last 7 Days", "This Month", "Custom"));
        cmbDateFilter.setValue("Today");
    }

    private void setupTableColumns() {
        colOrderNumber.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
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

        colType.setCellValueFactory(new PropertyValueFactory<>("orderType"));

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colTotal.setCellFactory(column -> new TableCell<Order, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("₱" + String.format("%.2f", item));
                    setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #f5ede0;");
                }
            }
        });

        colTime.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colTime.setCellFactory(column -> new TableCell<Order, LocalDateTime>() {
            private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(formatter));
                }
            }
        });

        colItems.setCellValueFactory(new PropertyValueFactory<>("itemsSummary"));
        colItems.setCellFactory(column -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setWrapText(true);
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
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
                        case "Done": badge.getStyleClass().add("badge-green"); break;
                        case "Completed": badge.getStyleClass().add("badge-completed"); break;
                        case "Cancelled": badge.getStyleClass().add("badge-red"); break;
                        default: badge.getStyleClass().add("badge-green");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        ordersTable.setItems(ordersList);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void loadOrders() {
        String orderType = cmbOrderType.getValue();
        if (orderType == null) orderType = "All Types";
        
        String dateFilter = cmbDateFilter.getValue();
        LocalDateTime start = null;
        LocalDateTime end = LocalDateTime.now();

        if ("Today".equals(dateFilter)) {
            start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        } else if ("Yesterday".equals(dateFilter)) {
            start = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0);
            end = LocalDateTime.now().minusDays(1).withHour(23).withMinute(59).withSecond(59);
        } else if ("Last 7 Days".equals(dateFilter)) {
            start = LocalDateTime.now().minusDays(7);
        } else if ("This Month".equals(dateFilter)) {
            start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        } else if ("Custom".equals(dateFilter)) {
            if (dateFrom.getValue() != null) start = dateFrom.getValue().atStartOfDay();
            if (dateTo.getValue() != null) end = dateTo.getValue().atTime(23, 59, 59);
        }

        List<Order> results = orderDAO.findByFilters(orderType, start, end);
        ordersList.setAll(results);
        loadStats();
    }

    private void loadStats() {
        lblTotalOrders.setText(String.valueOf(orderDAO.getTotalCount()));
        lblPreparing.setText(String.valueOf(orderDAO.getCountByStatus("Preparing")));
        lblCompleted.setText(String.valueOf(orderDAO.getCountByStatus("Completed")));
        lblCancelled.setText(String.valueOf(orderDAO.getCountByStatus("Cancelled")));
    }

    @FXML
    private void onOrderClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                showOrderDetails(selectedOrder);
            }
        }
    }

    private void showOrderDetails(Order order) {
        detailsOrderNum.setText("#" + String.format("%04d", order.getOrderNumber()));
        detailsDate.setText(order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM d, yyyy · h:mm a")));
        
        detailsType.setText(order.getOrderType());
        detailsPayment.setText(order.getPaymentType());
        
        if ("GCash".equals(order.getPaymentType())) {
            detailsRefLabel.setVisible(true);
            detailsRefLabel.setManaged(true);
            detailsRef.setVisible(true);
            detailsRef.setManaged(true);
            detailsRef.setText(order.getReferenceNumber() != null ? order.getReferenceNumber() : "-");
        } else {
            detailsRefLabel.setVisible(false);
            detailsRefLabel.setManaged(false);
            detailsRef.setVisible(false);
            detailsRef.setManaged(false);
        }
        
        Staff staff = staffDAO.findById(order.getStaffId());
        detailsStaff.setText(staff != null ? staff.getName() : "Unknown");
        
        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            detailsNotesBox.setVisible(true);
            detailsNotesBox.setManaged(true);
            detailsNotes.setText(order.getNotes());
        } else {
            detailsNotesBox.setVisible(false);
            detailsNotesBox.setManaged(false);
        }
        
        detailsSubtotal.setText("₱" + String.format("%.2f", order.getSubtotal()));
        detailsDiscount.setText("-₱" + String.format("%.2f", order.getDiscount()));
        detailsTotal.setText("₱" + String.format("%.2f", order.getTotal()));
        
        // Hide cancel button if already cancelled or completed
        if ("Cancelled".equals(order.getStatus()) || "Completed".equals(order.getStatus())) {
            btnCancelOrder.setVisible(false);
            btnCancelOrder.setManaged(false);
        } else {
            btnCancelOrder.setVisible(true);
            btnCancelOrder.setManaged(true);
        }
        
        detailsStatus.setText(order.getStatus().toUpperCase());
        detailsStatus.getStyleClass().removeAll("badge-amber", "badge-blue", "badge-green", "badge-completed", "badge-red");
        switch (order.getStatus()) {
            case "New": detailsStatus.getStyleClass().add("badge-amber"); break;
            case "Preparing": detailsStatus.getStyleClass().add("badge-blue"); break;
            case "Done": detailsStatus.getStyleClass().add("badge-green"); break;
            case "Completed": detailsStatus.getStyleClass().add("badge-completed"); break;
            case "Cancelled": detailsStatus.getStyleClass().add("badge-red"); break;
        }

        // Load items
        detailsItemsContainer.getChildren().clear();
        List<OrderItem> items = orderDAO.findItemsByOrderId(order.getId());
        for (OrderItem item : items) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(12);
            
            Label qty = new Label(item.getQuantity() + "x");
            qty.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #d4591e; -fx-font-weight: bold; -fx-min-width: 30;");
            
            Label name = new Label(item.getItemName());
            name.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label price = new Label("₱" + String.format("%.2f", item.getTotalPrice()));
            price.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #c4a882;");
            
            row.getChildren().addAll(qty, name, spacer, price);
            detailsItemsContainer.getChildren().add(row);
        }

        detailsOverlay.setVisible(true);
    }

    @FXML
    private void onCloseDetails() {
        detailsOverlay.setVisible(false);
    }

    @FXML
    private void onCancelOrder() {
        if (selectedOrder != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Cancel Order");
            confirm.setHeaderText("Cancel Order #" + String.format("%04d", selectedOrder.getOrderNumber()) + "?");
            confirm.setContentText("This action cannot be undone.");
            confirm.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1;");
            
            if (confirm.showAndWait().get() == ButtonType.OK) {
                if (orderDAO.updateStatus(selectedOrder.getId(), "Cancelled")) {
                    loadOrders();
                    onCloseDetails();
                }
            }
        }
    }

    @FXML
    private void onFilterChanged(ActionEvent event) {
        loadOrders();
    }

    @FXML
    private void onDateFilterChanged(ActionEvent event) {
        String filter = cmbDateFilter.getValue();
        if ("Custom".equals(filter)) {
            datePickerBox.setVisible(true);
            datePickerBox.setManaged(true);
        } else {
            datePickerBox.setVisible(false);
            datePickerBox.setManaged(false);
            loadOrders();
        }
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().trim();
        try {
            if (text.contains("Dashboard")) Main.showDashboard();
            else if (text.contains("Orders")) return;
            else if (text.contains("Kitchen")) Main.showKitchen();
            else if (text.contains("Menu Items")) Main.showMenuItems();
            else if (text.contains("Combos")) Main.showCombos();
            else if (text.contains("Inventory")) Main.showInventory();
            else if (text.contains("Staff")) Main.showStaff();
        } catch (Exception e) {
            e.printStackTrace();
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

    public void setActiveNav(String navName) {
        Button[] buttons = {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
        for (Button btn : buttons) {
            if (btn != null) btn.getStyleClass().remove("nav-item-active");
        }
        if (btnOrders != null) btnOrders.getStyleClass().add("nav-item-active");
    }
}