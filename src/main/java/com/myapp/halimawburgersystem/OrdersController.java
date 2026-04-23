package com.myapp.halimawburgersystem;

import com.myapp.dao.OrderDAO;
import com.myapp.dao.StaffDAO;
import com.myapp.model.Order;
import com.myapp.model.Staff;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

    private OrderDAO orderDAO = new OrderDAO();
    private StaffDAO staffDAO = new StaffDAO();
    private ObservableList<Order> ordersList = FXCollections.observableArrayList();
    private Order selectedOrder;

    @FXML
    public void initialize() {
        loadUserInfo();
        setActiveNav("Orders");
        setupFilters();
        setupTableColumns();
        loadOrders();
        loadStats();
    }

    private void loadUserInfo() {
        Staff staff = Main.getCurrentStaff();
        if (staff != null) {
            String initials = staff.getInitials();
            String fullName = staff.getName();

            if (userDisplayName != null) userDisplayName.setText(initials);
            if (userDisplayRole != null) userDisplayRole.setText(staff.getRole());

            if (sidebarAvatarText != null) sidebarAvatarText.setText(initials);
            if (sidebarUserName != null) sidebarUserName.setText(fullName);
            if (sidebarUserRole != null) sidebarUserRole.setText(staff.getRole());
        }
    }

    private void setupFilters() {
        cmbOrderType.getItems().addAll("All Types", "Dine-in", "Takeout");
        cmbOrderType.getSelectionModel().select(0);

        cmbDateFilter.getItems().addAll("Today", "Yesterday", "This Week", "Custom");
        cmbDateFilter.getSelectionModel().select(0);
    }

    private void setupTableColumns() {
        colOrderNumber.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colOrderNumber.setCellFactory(column -> new TableCell<Order, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    setText("#" + item);
                    getStyleClass().add("order-number");
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
                    setStyle(null);
                } else {
                    setText("₱" + String.format("%.2f", item));
                    getStyleClass().add("order-total");
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

        colItems.setCellValueFactory(new PropertyValueFactory<>("notes"));

        ordersTable.setItems(ordersList);
        ordersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadOrders() {
        String orderType = cmbOrderType.getValue();
        if (orderType == null) orderType = "All Types";

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        String dateFilter = cmbDateFilter.getValue();
        if (dateFilter == null) dateFilter = "Today";

        LocalDate today = LocalDate.now();
        switch (dateFilter) {
            case "Today":
                startDate = today.atStartOfDay();
                endDate = today.atTime(LocalTime.MAX);
                break;
            case "Yesterday":
                startDate = today.minusDays(1).atStartOfDay();
                endDate = today.minusDays(1).atTime(LocalTime.MAX);
                break;
            case "This Week":
                startDate = today.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
                endDate = today.atTime(LocalTime.MAX);
                break;
            case "Custom":
                if (dateFrom.getValue() != null && dateTo.getValue() != null) {
                    startDate = dateFrom.getValue().atStartOfDay();
                    endDate = dateTo.getValue().atTime(LocalTime.MAX);
                }
                break;
        }

        ordersList.clear();
        if (startDate != null && endDate != null) {
            ordersList.addAll(orderDAO.findByFilters(orderType, startDate, endDate));
        } else {
            ordersList.addAll(orderDAO.findAll());
        }
        ordersTable.setItems(ordersList);
    }

    private void loadStats() {
        int total = orderDAO.getTotalCount();
        int preparing = orderDAO.getCountByStatus("Preparing");
        int completed = orderDAO.getCountByStatus("Done");
        int cancelled = orderDAO.getCountByStatus("Cancelled");

        lblTotalOrders.setText(String.valueOf(total));
        lblPreparing.setText(String.valueOf(preparing));
        lblCompleted.setText(String.valueOf(completed));
        lblCancelled.setText(String.valueOf(cancelled));
    }

    @FXML
    private void onFilterChanged(ActionEvent event) {
        loadOrders();
        loadStats();
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
            loadStats();
        }
    }

    @FXML
    private void onOrderClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                showOrderDetailsDialog(selectedOrder);
            }
        }
    }

    private void showOrderDetailsDialog(Order order) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Order Details - #" + order.getOrderNumber());
        alert.setHeaderText("Order #" + order.getOrderNumber());
        alert.setContentText(
            "Type: " + order.getOrderType() + "\n" +
            "Total: ₱" + String.format("%.2f", order.getTotal()) + "\n" +
            "Payment: " + order.getPaymentType() + "\n" +
            "Status: " + order.getStatus() + "\n" +
            "Notes: " + (order.getNotes() != null ? order.getNotes() : "None")
        );
        alert.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        alert.showAndWait();
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().trim();

        clearAllHighlights();
        clicked.getStyleClass().add("nav-item-active");

        try {
            if (text.contains("Dashboard")) {
                Main.showDashboard();
            } else if (text.contains("Orders") || text.contains("Kitchen")) {
                return;
            } else if (text.contains("Menu Items")) {
                Main.showMenuItems();
            } else if (text.contains("Combos")) {
                Main.showCombos();
            } else if (text.contains("Inventory")) {
                Main.showInventory();
            } else if (text.contains("Sales")) {
                Main.showInventory();
            } else if (text.contains("Staff")) {
                Main.showStaff();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}