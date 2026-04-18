package com.myapp.halimawburgersystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label pageTitle;

    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    @FXML
    public void initialize() {
        if (btnDashboard != null) {
            btnDashboard.getStyleClass().add("nav-item-active");
        }
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().trim();

        clearAllHighlights();
        clicked.getStyleClass().add("nav-item-active");

        if (pageTitle != null) {
            pageTitle.setText(text);
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