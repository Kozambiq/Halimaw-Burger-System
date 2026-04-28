package com.myapp.halimawburgersystem;

import com.myapp.dao.StaffDAO;
import com.myapp.dao.UserDAO;
import com.myapp.model.Staff;
import com.myapp.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private SVGPath eyeIcon;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label loginError;

    private UserDAO userDAO = new UserDAO();
    private StaffDAO staffDAO = new StaffDAO();
    private boolean passwordVisible = false;

    @FXML
    private void initialize() {
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #6d5a40; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 14 12 14; -fx-font-size: 14px;";
        String errorBorderStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #6d5a40; -fx-border-color: #e07070; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 14 12 14; -fx-font-size: 14px;";

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                emailError.setVisible(false);
                emailError.setManaged(false);
                emailField.setStyle(fieldStyle);
            } else if (!isValidEmail(email)) {
                emailError.setText("Email format is invalid");
                emailError.setVisible(true);
                emailError.setManaged(true);
                emailField.setStyle(errorBorderStyle);
            } else {
                emailError.setVisible(false);
                emailError.setManaged(false);
                emailField.setStyle(fieldStyle);
            }
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            String password = passwordField.getText();
            if (password.isEmpty()) {
                passwordError.setVisible(false);
                passwordError.setManaged(false);
                passwordField.setStyle(fieldStyle);
                passwordVisibleField.setStyle(fieldStyle);
            } else {
                passwordError.setVisible(false);
                passwordError.setManaged(false);
                passwordField.setStyle(fieldStyle);
                passwordVisibleField.setStyle(fieldStyle);
            }
        });

        passwordVisibleField.textProperty().addListener((obs, oldVal, newVal) -> {
            String password = passwordVisibleField.getText();
            if (password.isEmpty()) {
                passwordError.setVisible(false);
                passwordError.setManaged(false);
                passwordField.setStyle(fieldStyle);
                passwordVisibleField.setStyle(fieldStyle);
            } else {
                passwordError.setVisible(false);
                passwordError.setManaged(false);
                passwordField.setStyle(fieldStyle);
                passwordVisibleField.setStyle(fieldStyle);
            }
        });
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.indexOf('@') < email.lastIndexOf('.') && email.lastIndexOf('.') > email.indexOf('@');
    }

    private String sanitizeInput(String input) {
        if (input == null) return "";
        return input.replaceAll("[;'\"\\\\]", "");
    }

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
        String email = sanitizeInput(emailField.getText().trim());
        String password = sanitizeInput(passwordVisible ? passwordVisibleField.getText() : passwordField.getText());

        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #6d5a40; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 14 12 14; -fx-font-size: 14px;";
        String errorBorderStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #6d5a40; -fx-border-color: #e07070; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12 14 12 14; -fx-font-size: 14px;";

        boolean valid = true;

        if (email.isEmpty()) {
            emailError.setText("Email format is invalid");
            emailError.setVisible(true);
            emailError.setManaged(true);
            emailField.setStyle(errorBorderStyle);
            valid = false;
        } else if (!isValidEmail(email)) {
            emailError.setText("Email format is invalid");
            emailError.setVisible(true);
            emailError.setManaged(true);
            emailField.setStyle(errorBorderStyle);
            valid = false;
        }

        if (password.isEmpty()) {
            passwordError.setText("Required");
            passwordError.setVisible(true);
            passwordError.setManaged(true);
            passwordField.setStyle(errorBorderStyle);
            passwordVisibleField.setStyle(errorBorderStyle);
            valid = false;
        }

        if (!valid) {
            return;
        }

        loginError.setVisible(false);
        loginError.setManaged(false);

        userDAO.authenticate(email, password).ifPresentOrElse(
            user -> {
                try {
                    Main.setCurrentUser(user);
                    Staff staff = staffDAO.findById(user.getStaffId());
                    Main.setCurrentStaff(staff);

                    if ("Cashier".equals(user.getRole())) {
                        Main.showCashier();
                    } else if ("Cook".equals(user.getRole())) {
                        Main.showCook();
                    } else {
                        Main.showDashboard();
                    }
                } catch (Exception e) {
                    showStyledAlert(AlertType.ERROR,
                            "Navigation Error",
                            "Failed to load: " + e.getMessage());
                }
            },
            () -> {
                loginError.setVisible(true);
                loginError.setManaged(true);
            }
        );
    }

    private void showStyledAlert(AlertType type, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(type == AlertType.ERROR ? "Error" : "Warning");
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-text-fill: #e07070; -fx-font-size: 14px; -fx-font-weight: bold;");
        headerLabel.setAlignment(Pos.CENTER);
        headerLabel.setPadding(new Insets(35, 0, 0, 0));
        alert.getDialogPane().setHeader(headerLabel);
        
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #e07070; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        alert.getDialogPane().setGraphic(null);

        javafx.scene.Node contentNode = alert.getDialogPane().lookup(".content");
        if (contentNode != null) {
            contentNode.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px;");
        }

        alert.getDialogPane().getButtonTypes().clear();
        alert.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.OK);

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");

        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setResizable(false);
        stage.initStyle(StageStyle.UTILITY);
        
        alert.showAndWait();
    }

    @FXML
    private void onForgotPassword(ActionEvent event) {
        showStyledAlert(AlertType.INFORMATION, "Forgot Password", "Please contact the system administrator to reset your password.");
    }

    @FXML
    private void onRequestAccess(ActionEvent event) {
        showStyledAlert(AlertType.INFORMATION, "Request Access", "Please contact the system administrator to request access.");
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}