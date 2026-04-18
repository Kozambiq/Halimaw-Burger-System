package com.myapp.halimawburgersystem;

import com.myapp.dao.UserDAO;
import com.myapp.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.SVGPath;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private SVGPath eyeIcon;

    private UserDAO userDAO = new UserDAO();
    private boolean passwordVisible = false;

    @FXML
    private void onTogglePassword(ActionEvent event) {
        if (passwordVisible) {
            passwordField.setVisible(true);
            passwordField.setText(passwordVisibleField.getText());
            passwordVisibleField.setVisible(false);
            eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
        } else {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordField.setVisible(false);
            eyeIcon.setContent("M18 6.5L9.5 15 2 6.5l1.5 1.5 6 6-6 6 1.5 1.5 9.5-9.5-6-6zm-6 8.5c-2.76 0-5-2.24-5-5s2.24-5 5-5c.55 0 1.05.09 1.53.24l1.5-1.5c-.71-.28-1.47-.43-2.27-.43-2.76 0-5 2.24-5 5s2.24 5 5 5c.8 0 1.56-.15 2.27-.43l1.5-1.5c-.48-.15-.98-.24-1.53-.24z");
        }
        passwordVisible = !passwordVisible;
    }

    @FXML
    private void onEnter(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordVisible ? passwordVisibleField.getText() : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING,
                    "Missing Credentials",
                    "Please enter both your email address and password.");
            return;
        }

        userDAO.authenticate(email, password).ifPresentOrElse(
            user -> {
                try {
                    Main.showDashboard();
                } catch (Exception e) {
                    showAlert(AlertType.ERROR,
                            "Navigation Error",
                            "Failed to load dashboard: " + e.getMessage());
                }
            },
            () -> showAlert(AlertType.ERROR,
                    "Invalid Credentials",
                    "Invalid email or password. Please try again.")
        );
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        showAlert(AlertType.INFORMATION,
                "Forgot Password",
                "Please contact the system administrator to reset your password.");
    }

    @FXML
    private void onRequestAccess(ActionEvent event) {
        showAlert(AlertType.INFORMATION,
                "Request Access",
                "Please contact the system administrator to request access.");
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}