package com.myapp.halimawburgersystem;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;

public class NavController {

    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    @FXML private Label sidebarAvatarText;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;

    @FXML
    private void onLogout(ActionEvent event) {
        try {
            Main.clearSession();
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUserInfo(String initials, String name, String role) {
        if (sidebarAvatarText != null) sidebarAvatarText.setText(initials);
        if (sidebarUserName != null) sidebarUserName.setText(name);
        if (sidebarUserRole != null) sidebarUserRole.setText(role);
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Button source = (Button) event.getSource();
        try {
            switch (source.getText()) {
                case "Dashboard"       -> Main.showDashboard();
                case "Orders"          -> Main.showOrders();
                case "Kitchen Queue"   -> Main.showKitchen();
                case "Menu Items"      -> Main.showMenuItems();
                case "Combos & Promos" -> Main.showCombos();
                case "Inventory"       -> Main.showInventory();
                case "Sales Reports"   -> Main.showSalesReport();
                case "Staff"           -> Main.showStaff();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setActiveNav(String navName) {
        clearAllHighlights();
        if (navName == null) return;

        switch (navName) {
            case "Dashboard"       -> addActive(btnDashboard);
            case "Orders"          -> addActive(btnOrders);
            case "Kitchen Queue"   -> addActive(btnKitchen);
            case "Menu Items"      -> addActive(btnMenuItems);
            case "Combos & Promos" -> addActive(btnCombos);
            case "Inventory"       -> addActive(btnInventory);
            case "Sales Reports"   -> addActive(btnSales);
            case "Staff"           -> addActive(btnStaff);
        }
    }

    private void clearAllHighlights() {
        Button[] buttons = {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
        for (Button btn : buttons) {
            if (btn != null) btn.getStyleClass().remove("nav-item-active");
        }
    }

    private void addActive(Button btn) {
        if (btn != null) btn.getStyleClass().add("nav-item-active");
    }
}