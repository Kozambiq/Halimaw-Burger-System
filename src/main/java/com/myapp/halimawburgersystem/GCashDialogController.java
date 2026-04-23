package com.myapp.halimawburgersystem;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class GCashDialogController {

    @FXML private TextField txtReferenceNumber;
    @FXML private Label lblError;
    @FXML private Button btnConfirm;

    private String referenceNumber;

    @FXML
    private void initialize() {
        txtReferenceNumber.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                btnConfirm.setDisable(false);
                lblError.setVisible(false);
                lblError.setManaged(false);
            } else {
                btnConfirm.setDisable(true);
            }
        });
    }

    @FXML
    private void onCancel() {
        referenceNumber = null;
        closeDialog();
    }

    @FXML
    private void onConfirm() {
        String ref = txtReferenceNumber.getText().trim();
        if (ref.isEmpty()) {
            lblError.setText("Reference number is required");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        }
        referenceNumber = ref;
        closeDialog();
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public boolean isConfirmed() {
        return referenceNumber != null;
    }

    private void closeDialog() {
        Stage stage = (Stage) txtReferenceNumber.getScene().getWindow();
        stage.close();
    }
}