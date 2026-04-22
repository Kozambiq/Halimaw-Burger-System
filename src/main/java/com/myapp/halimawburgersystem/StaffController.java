package com.myapp.halimawburgersystem;

import com.myapp.dao.StaffDAO;
import com.myapp.model.Staff;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.time.LocalTime;
import java.util.List;

public class StaffController {

    @FXML private Label lblTotal;
    @FXML private Label lblOnShift;
    @FXML private Label lblOnBreak;
    @FXML private TableView<Staff> staffTable;
    @FXML private TableColumn<Staff, String> colAvatar;
    @FXML private TableColumn<Staff, String> colName;
    @FXML private TableColumn<Staff, String> colRole;
    @FXML private TableColumn<Staff, String> colShiftHours;
    @FXML private TableColumn<Staff, String> colStatus;
    @FXML private TableColumn<Staff, String> colActions;

    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    private StaffDAO staffDAO = new StaffDAO();
    private boolean alreadyLoaded = false;
    private List<Staff> allStaff;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        setActiveNav("Staff");
        setupTableColumns();
        loadStaff();
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colShiftHours.setCellValueFactory(new PropertyValueFactory<>("shiftHours"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<Staff, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label pill = new Label(status);
                pill.getStyleClass().add("status-pill");

                if ("Active".equals(status)) {
                    pill.getStyleClass().add("pill-ok");
                } else if ("Break".equals(status)) {
                    pill.getStyleClass().add("pill-low");
                } else if ("Off Shift".equals(status)) {
                    pill.getStyleClass().add("pill-off");
                }

                setGraphic(pill);
                setText(null);
            }
        });

        colAvatar.setCellFactory(col -> new TableCell<Staff, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Staff staff = getTableView().getItems().get(getIndex());
                if (staff == null) {
                    setGraphic(null);
                    return;
                }

                Label avatar = new Label(staff.getInitials());
                avatar.getStyleClass().add("staff-avatar");
                setGraphic(avatar);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<Staff, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                int rowIndex = getIndex();
                if (rowIndex < 0 || rowIndex >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                final Staff staff = getTableView().getItems().get(rowIndex);
                if (staff == null) {
                    setGraphic(null);
                    return;
                }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem edit = new MenuItem("Edit");
                edit.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                edit.setOnAction(e -> showEditStaffDialog(staff));

                if ("Active".equals(staff.getStatus())) {
                    MenuItem onBreak = new MenuItem("Put on Break");
                    onBreak.setStyle("-fx-text-fill: #e8b84b; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    onBreak.setOnAction(e -> updateStatus(staff.getId(), "Break"));
                    MenuItem offShift = new MenuItem("End Shift");
                    offShift.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    offShift.setOnAction(e -> updateStatus(staff.getId(), "Off Shift"));
                    menuBtn.getItems().addAll(edit, onBreak, offShift);
                } else if ("Break".equals(staff.getStatus())) {
                    MenuItem active = new MenuItem("Start Shift");
                    active.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    active.setOnAction(e -> updateStatus(staff.getId(), "Active"));
                    MenuItem offShift = new MenuItem("End Shift");
                    offShift.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    offShift.setOnAction(e -> updateStatus(staff.getId(), "Off Shift"));
                    menuBtn.getItems().addAll(edit, active, offShift);
                } else {
                    MenuItem active = new MenuItem("Start Shift");
                    active.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    active.setOnAction(e -> updateStatus(staff.getId(), "Active"));
                    MenuItem remove = new MenuItem("Remove");
                    remove.setStyle("-fx-text-fill: #e07070; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    remove.setOnAction(e -> showRemoveConfirmation(staff));
                    menuBtn.getItems().addAll(edit, active, remove);
                }

                setGraphic(menuBtn);
                setText(null);
            }
        });

        staffTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void updateStatus(int id, String status) {
        staffDAO.updateStatus(id, status);
        loadStaff();
    }

    private void loadStaff() {
        try {
            int total = staffDAO.getTotalCount();
            int active = staffDAO.getActiveCount();
            int onBreak = staffDAO.getOnBreakCount();
            allStaff = staffDAO.findAll();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblOnShift.setText(String.valueOf(active));
                lblOnBreak.setText(String.valueOf(onBreak));
                staffTable.setItems(FXCollections.observableArrayList(allStaff));
            });
        } catch (Exception e) {
            System.err.println("Error loading staff: " + e.getMessage());
        }
    }

    private void showEditStaffDialog(Staff staff) {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Edit Staff Member");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        String labelStyle = "-fx-text-fill: #a09070; -fx-font-size: 12px;";
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(8);

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle(labelStyle);
        TextField nameField = new TextField(staff.getName());
        nameField.setStyle(fieldStyle);

        Label roleLabel = new Label("Role:");
        roleLabel.setStyle(labelStyle);
        javafx.scene.control.ChoiceBox<String> roleChoice = new javafx.scene.control.ChoiceBox<>();
        roleChoice.getItems().addAll("Manager", "Cashier", "Cook");
        roleChoice.setValue(staff.getRole());
        roleChoice.setStyle(fieldStyle);

        Label startLabel = new Label("Shift Start (HH:mm):");
        startLabel.setStyle(labelStyle);
        TextField startField = new TextField(staff.getShiftStart() != null ? staff.getShiftStart().toString() : "");
        startField.setStyle(fieldStyle);

        Label endLabel = new Label("Shift End (HH:mm):");
        endLabel.setStyle(labelStyle);
        TextField endField = new TextField(staff.getShiftEnd() != null ? staff.getShiftEnd().toString() : "");
        endField.setStyle(fieldStyle);

        vbox.getChildren().addAll(nameLabel, nameField, roleLabel, roleChoice, startLabel, startField, endLabel, endField);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return new String[]{
                    nameField.getText(),
                    roleChoice.getValue(),
                    startField.getText(),
                    endField.getText()
                };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != null && result.length == 4) {
                String name = result[0].trim();
                String role = result[1];
                String startTime = result[2].trim();
                String endTime = result[3].trim();

                if (!name.isEmpty()) {
                    staffDAO.updateRole(staff.getId(), role);
                    staff.setRole(role);

                    try {
                        LocalTime start = startTime.isEmpty() ? null : LocalTime.parse(startTime);
                        LocalTime end = endTime.isEmpty() ? null : LocalTime.parse(endTime);
                        if (start != null && end != null) {
                            staffDAO.updateShift(staff.getId(), start, end);
                        }
                    } catch (Exception e) {
                        System.err.println("Invalid time format: " + e.getMessage());
                    }

                    loadStaff();
                }
            }
        });
    }

    private void showRemoveConfirmation(Staff staff) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove Staff");
        alert.setHeaderText("Remove " + staff.getName() + "?");
        alert.setContentText("This staff member will be removed from the system.");
        alert.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        ButtonType removeType = new ButtonType("Remove");
        ButtonType cancelType = new ButtonType("Cancel");
        alert.getDialogPane().getButtonTypes().setAll(removeType, cancelType);

        Button removeBtn = (Button) alert.getDialogPane().lookupButton(removeType);
        removeBtn.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        removeBtn.setOnAction(e -> {
            staffDAO.delete(staff.getId());
            loadStaff();
        });

        Button cancelBtn = (Button) alert.getDialogPane().lookupButton(cancelType);
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        alert.showAndWait();
    }

    @FXML
    private void onAddStaff() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Add Staff Member");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        String labelStyle = "-fx-text-fill: #a09070; -fx-font-size: 12px;";
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";
        String errorStyle = "-fx-text-fill: #e07070; -fx-font-size: 11px;";

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(8);

        Label nameLabel = new Label("Full Name:");
        nameLabel.setStyle(labelStyle);
        TextField nameField = new TextField();
        nameField.setStyle(fieldStyle);
        nameField.setPromptText("e.g. Juan dela Cruz");
        Label nameError = new Label();
        nameError.setStyle(errorStyle);
        nameError.setVisible(false);

        Label roleLabel = new Label("Role:");
        roleLabel.setStyle(labelStyle);
        javafx.scene.control.ChoiceBox<String> roleChoice = new javafx.scene.control.ChoiceBox<>();
        roleChoice.getItems().addAll("Manager", "Cashier", "Cook");
        roleChoice.setValue("Cashier");
        roleChoice.setStyle(fieldStyle);

        Label startLabel = new Label("Shift Start (HH:mm):");
        startLabel.setStyle(labelStyle);
        TextField startField = new TextField();
        startField.setStyle(fieldStyle);
        startField.setPromptText("e.g. 08:00");
        Label startError = new Label();
        startError.setStyle(errorStyle);
        startError.setVisible(false);

        Label endLabel = new Label("Shift End (HH:mm):");
        endLabel.setStyle(labelStyle);
        TextField endField = new TextField();
        endField.setStyle(fieldStyle);
        endField.setPromptText("e.g. 17:00");
        Label endError = new Label();
        endError.setStyle(errorStyle);
        endError.setVisible(false);

        vbox.getChildren().addAll(nameLabel, nameField, nameError, roleLabel, roleChoice, startLabel, startField, startError, endLabel, endField, endError);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        okButton.setDisable(true);

        javafx.beans.InvalidationListener validator = obs -> {
            boolean valid = true;
            String name = nameField.getText().trim();
            String start = startField.getText().trim();
            String end = endField.getText().trim();

            if (name.isEmpty()) {
                nameError.setText("Required");
                nameError.setVisible(true);
                nameField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else if (staffDAO.existsByName(name)) {
                nameError.setText("Already exists");
                nameError.setVisible(true);
                nameField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else {
                nameError.setVisible(false);
                nameField.setStyle(fieldStyle);
            }

            try {
                if (!start.isEmpty()) {
                    LocalTime.parse(start);
                }
                startError.setVisible(false);
            } catch (Exception e) {
                startError.setText("Invalid format (HH:mm)");
                startError.setVisible(true);
                valid = false;
            }

            try {
                if (!end.isEmpty()) {
                    LocalTime.parse(end);
                }
                endError.setVisible(false);
            } catch (Exception e) {
                endError.setText("Invalid format (HH:mm)");
                endError.setVisible(true);
                valid = false;
            }

            if (valid && !name.isEmpty() && !start.isEmpty() && !end.isEmpty()) {
                okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
                okButton.setDisable(false);
            } else {
                okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
                okButton.setDisable(true);
            }
        };

        nameField.textProperty().addListener(validator);
        startField.textProperty().addListener(validator);
        endField.textProperty().addListener(validator);

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return new String[]{
                    nameField.getText(),
                    roleChoice.getValue(),
                    startField.getText(),
                    endField.getText()
                };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != null && result.length == 4) {
                String name = result[0].trim();
                String role = result[1];
                String startTime = result[2].trim();
                String endTime = result[3].trim();

                if (!name.isEmpty() && !startTime.isEmpty() && !endTime.isEmpty()) {
                    try {
                        LocalTime start = LocalTime.parse(startTime);
                        LocalTime end = LocalTime.parse(endTime);
                        staffDAO.insert(name, role, start, end);
                        loadStaff();
                    } catch (Exception e) {
                        System.err.println("Invalid time format: " + e.getMessage());
                    }
                }
            }
        });
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
    private void onNavigateDashboard(ActionEvent event) {
        try {
            Main.showDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateOrders(ActionEvent event) {
    }

    @FXML
    private void onNavigateKitchen(ActionEvent event) {
    }

    @FXML
    private void onNavigateMenuItems(ActionEvent event) {
        try {
            Main.showMenuItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateCombos(ActionEvent event) {
        try {
            Main.showCombos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateInventory(ActionEvent event) {
        try {
            Main.showInventory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateSales(ActionEvent event) {
    }

    @FXML
    private void onNavigateStaff(ActionEvent event) {
        setActiveNav("Staff");
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