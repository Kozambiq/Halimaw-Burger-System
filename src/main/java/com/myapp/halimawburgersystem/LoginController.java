package com.myapp.halimawburgersystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button enterButton;
    @FXML private Hyperlink forgotLink;
    @FXML private Hyperlink requestLink;

    @FXML
    private void onEnter(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            return;
        }

        try {
            Main.showDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
    }

    @FXML
    private void onRequestAccess(ActionEvent event) {
    }
}