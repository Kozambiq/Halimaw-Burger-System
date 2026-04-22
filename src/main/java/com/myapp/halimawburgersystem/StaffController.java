package com.myapp.halimawburgersystem;

import com.myapp.dao.StaffDAO;
import com.myapp.dao.UserDAO;
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
import javafx.scene.control.PasswordField;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.time.LocalTime;
import java.util.List;

import javafx.scene.control.ComboBox;

public class StaffController {

    @FXML private Label lblTotal;
    @FXML private Label lblOnShift;
    @FXML private Label lblOnBreak;
    @FXML private TableView<Staff> staffTable;
    @FXML private TableColumn<Staff, String> colAvatar;
    @FXML private TableColumn<Staff, String> colName;
    @FXML private TableColumn<Staff, String> colEmail;
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
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
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
                } else if ("Disabled".equals(status)) {
                    pill.getStyleClass().add("pill-disabled");
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
                    MenuItem disable = new MenuItem("Disabled");
                    disable.setStyle("-fx-text-fill: #e07070; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    disable.setOnAction(e -> updateStatus(staff.getId(), "Disabled"));
                    menuBtn.getItems().addAll(edit, active, disable);
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

        Label emailLabel = new Label("Email:");
        emailLabel.setStyle(labelStyle);
        TextField emailField = new TextField(staff.getEmail());
        emailField.setStyle(fieldStyle);

        Label passwordLabel = new Label("Password:");
        passwordLabel.setStyle(labelStyle);
        HBox passwordBox = new HBox(4);
        PasswordField passwordField = new PasswordField();
        TextField passwordVisibleField = new TextField();
        final UserDAO userDAO = new UserDAO();
        String originalPassword = "";
        if (userDAO.findPasswordHashByStaffId(staff.getId()).isPresent()) {
            String hash = userDAO.findPasswordHashByStaffId(staff.getId()).get();
            if (hash.startsWith("$2") && hash.length() == 60) {
                originalPassword = "••••••••";
            } else {
                originalPassword = hash;
            }
        }
        passwordField.setText(originalPassword);
        passwordVisibleField.setText(originalPassword);
        passwordField.setStyle(fieldStyle);
        passwordVisibleField.setStyle(fieldStyle);
        passwordVisibleField.setVisible(false);

        final String finalOriginalPassword = originalPassword;

        SVGPath eyeIcon = new SVGPath();
        eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
        eyeIcon.setFill(Color.web("#8a7055"));
        eyeIcon.setStyle("-fx-cursor: hand;");
        eyeIcon.setOnMouseClicked(e -> {
            if (passwordVisibleField.isVisible()) {
                passwordField.setText(passwordVisibleField.getText());
                passwordField.setVisible(true);
                passwordVisibleField.setVisible(false);
                eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
            } else {
                passwordVisibleField.setText(passwordField.getText());
                passwordVisibleField.setVisible(true);
                passwordField.setVisible(false);
                eyeIcon.setContent("M18 6.5L9.5 15 2 6.5l1.5 1.5 6 6-6 6 1.5 1.5 9.5-9.5-6-6zm-6 8.5c-2.76 0-5-2.24-5-5s2.24-5 5-5c.55 0 1.05.09 1.53.24l1.5-1.5c-.71-.28-1.47-.43-2.27-.43-2.76 0-5 2.24-5 5s2.24 5 5 5c.8 0 1.56-.15 2.27-.43l1.5-1.5c-.48-.15-.98-.24-1.53-.24z");
            }
        });

        passwordBox.getChildren().addAll(passwordField, passwordVisibleField, eyeIcon);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        HBox.setHgrow(passwordVisibleField, Priority.ALWAYS);

        Label roleLabel = new Label("Role:");
        roleLabel.setStyle(labelStyle);
        ComboBox<String> roleChoice = new ComboBox<>();
        roleChoice.getItems().addAll("Manager", "Cashier", "Cook");
        roleChoice.setValue(staff.getRole());
        roleChoice.setStyle(fieldStyle);
        roleChoice.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        roleChoice.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });

        Label startLabel = new Label("Shift Start:");
        startLabel.setStyle(labelStyle);
        HBox startBox = new HBox(4);
        ComboBox<Integer> startHour = new ComboBox<>();
        for (int h = 1; h <= 12; h++) startHour.getItems().add(h);
        startHour.setStyle(fieldStyle);
        startHour.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        startHour.setCellFactory(listView -> new javafx.scene.control.ListCell<Integer>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        ComboBox<Integer> startMinute = new ComboBox<>();
        for (int m = 0; m < 60; m += 5) startMinute.getItems().add(m);
        startMinute.setStyle(fieldStyle);
        startMinute.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        startMinute.setCellFactory(listView -> new javafx.scene.control.ListCell<Integer>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        ComboBox<String> startAmPm = new ComboBox<>();
        startAmPm.getItems().addAll("AM", "PM");
        startAmPm.setStyle(fieldStyle);
        startAmPm.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        startAmPm.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        startBox.getChildren().addAll(startHour, startMinute, startAmPm);
        if (staff.getShiftStart() != null) {
            int sh = staff.getShiftStart().getHour();
            startHour.setValue(sh == 0 ? 12 : (sh > 12 ? sh - 12 : sh));
            startMinute.setValue(staff.getShiftStart().getMinute());
            startAmPm.setValue(sh >= 12 ? "PM" : "AM");
        } else {
            startHour.setValue(8);
            startMinute.setValue(0);
            startAmPm.setValue("AM");
        }

        Label endLabel = new Label("Shift End:");
        endLabel.setStyle(labelStyle);
        HBox endBox = new HBox(4);
        ComboBox<Integer> endHour = new ComboBox<>();
        for (int h = 1; h <= 12; h++) endHour.getItems().add(h);
        endHour.setStyle(fieldStyle);
        endHour.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        endHour.setCellFactory(listView -> new javafx.scene.control.ListCell<Integer>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        ComboBox<Integer> endMinute = new ComboBox<>();
        for (int m = 0; m < 60; m += 5) endMinute.getItems().add(m);
        endMinute.setStyle(fieldStyle);
        endMinute.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        endMinute.setCellFactory(listView -> new javafx.scene.control.ListCell<Integer>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        ComboBox<String> endAmPm = new ComboBox<>();
        endAmPm.getItems().addAll("AM", "PM");
        endAmPm.setStyle(fieldStyle);
        endAmPm.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        endAmPm.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        endBox.getChildren().addAll(endHour, endMinute, endAmPm);
        if (staff.getShiftEnd() != null) {
            int eh = staff.getShiftEnd().getHour();
            endHour.setValue(eh == 0 ? 12 : (eh > 12 ? eh - 12 : eh));
            endMinute.setValue(staff.getShiftEnd().getMinute());
            endAmPm.setValue(eh >= 12 ? "PM" : "AM");
        } else {
            endHour.setValue(5);
            endMinute.setValue(0);
            endAmPm.setValue("PM");
        }

        vbox.getChildren().addAll(nameLabel, nameField, emailLabel, emailField, passwordLabel, passwordBox, roleLabel, roleChoice, startLabel, startBox, endLabel, endBox);
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
                    emailField.getText(),
                    passwordVisibleField.isVisible() ? passwordVisibleField.getText() : passwordField.getText(),
                    roleChoice.getValue(),
                    convertTo24Hour(startHour.getValue(), startMinute.getValue(), startAmPm.getValue()),
                    convertTo24Hour(endHour.getValue(), endMinute.getValue(), endAmPm.getValue())
                };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != null && result.length == 6) {
                String name = result[0].trim();
                String email = result[1].trim();
                String password = result[2];
                String role = result[3];
                String startTime = result[4];
                String endTime = result[5];

                if (!name.isEmpty()) {
                    staffDAO.updateName(staff.getId(), name);
                    staff.setName(name);

                    if (!email.isEmpty() && !email.equals(staff.getEmail())) {
                        staffDAO.updateEmail(staff.getId(), email);
                        staff.setEmail(email);
                    }

                    if (password != null && !password.equals(finalOriginalPassword) && !password.isEmpty() && !password.equals("••••••••")) {
                        userDAO.updatePasswordByStaffId(staff.getId(), password);
                    }

                    staffDAO.updateRole(staff.getId(), role);
                    staff.setRole(role);

                    try {
                        LocalTime start = startTime != null ? LocalTime.parse(startTime) : null;
                        LocalTime end = endTime != null ? LocalTime.parse(endTime) : null;
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

        Label emailLabel = new Label("Email:");
        emailLabel.setStyle(labelStyle);
        TextField emailField = new TextField();
        emailField.setStyle(fieldStyle);
        emailField.setPromptText("e.g. staff@halimaw.ph");
        Label emailError = new Label();
        emailError.setStyle(errorStyle);
        emailError.setVisible(false);

        Label passwordLabel = new Label("Password:");
        passwordLabel.setStyle(labelStyle);
        PasswordField passwordField = new PasswordField();
        passwordField.setStyle(fieldStyle);
        passwordField.setPromptText("Min. 6 characters");
        Label passwordError = new Label();
        passwordError.setStyle(errorStyle);
        passwordError.setVisible(false);

        Label roleLabel = new Label("Role:");
        roleLabel.setStyle(labelStyle);
        ComboBox<String> roleChoice = new ComboBox<>();
        roleChoice.getItems().addAll("Manager", "Cashier", "Cook");
        roleChoice.setValue("Cashier");
        roleChoice.setStyle(fieldStyle);
        roleChoice.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        roleChoice.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });

        Label startLabel = new Label("Shift Start:");
        startLabel.setStyle(labelStyle);
        HBox startBox = new HBox(4);
        ComboBox<Integer> startHour = new ComboBox<>();
        for (int h = 1; h <= 12; h++) startHour.getItems().add(h);
        startHour.setValue(8);
        startHour.setStyle(fieldStyle);
        startHour.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        startHour.setCellFactory(listView -> new javafx.scene.control.ListCell<Integer>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        ComboBox<Integer> startMinute = new ComboBox<>();
        for (int m = 0; m < 60; m += 5) startMinute.getItems().add(m);
        startMinute.setValue(0);
        startMinute.setStyle(fieldStyle);
        startMinute.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        startMinute.setCellFactory(listView -> new javafx.scene.control.ListCell<Integer>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        ComboBox<String> startAmPm = new ComboBox<>();
        startAmPm.getItems().addAll("AM", "PM");
        startAmPm.setValue("AM");
        startAmPm.setStyle(fieldStyle);
        startAmPm.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        startAmPm.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        startBox.getChildren().addAll(startHour, startMinute, startAmPm);
        Label startError = new Label();
        startError.setStyle(errorStyle);
        startError.setVisible(false);

        Label endLabel = new Label("Shift End:");
        endLabel.setStyle(labelStyle);
        HBox endBox = new HBox(4);
        ComboBox<Integer> endHour = new ComboBox<>();
        for (int h = 1; h <= 12; h++) endHour.getItems().add(h);
        endHour.setValue(5);
        endHour.setStyle(fieldStyle);
        endHour.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        endHour.setCellFactory(listView -> new javafx.scene.control.ListCell<Integer>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        ComboBox<Integer> endMinute = new ComboBox<>();
        for (int m = 0; m < 60; m += 5) endMinute.getItems().add(m);
        endMinute.setValue(0);
        endMinute.setStyle(fieldStyle);
        endMinute.setButtonCell(new javafx.scene.control.ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        endMinute.setCellFactory(listView -> new javafx.scene.control.ListCell<Integer>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(String.format("%02d", item));
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        ComboBox<String> endAmPm = new ComboBox<>();
        endAmPm.getItems().addAll("AM", "PM");
        endAmPm.setValue("PM");
        endAmPm.setStyle(fieldStyle);
        endAmPm.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#ffffff"));
                }
            }
        });
        endAmPm.setCellFactory(listView -> new javafx.scene.control.ListCell<String>() {
            {
                setBackground(new Background(new BackgroundFill(Color.web("#221a0e"), CornerRadii.EMPTY, Insets.EMPTY)));
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null) {
                    setText(item);
                    setTextFill(Color.web("#f5ede0"));
                }
            }
        });
        endBox.getChildren().addAll(endHour, endMinute, endAmPm);
        Label endError = new Label();
        endError.setStyle(errorStyle);
        endError.setVisible(false);

        vbox.getChildren().addAll(nameLabel, nameField, nameError, emailLabel, emailField, emailError, passwordLabel, passwordField, passwordError, roleLabel, roleChoice, startLabel, startBox, startError, endLabel, endBox, endError);
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
            String email = emailField.getText().trim();
            String password = passwordField.getText();

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

            if (email.isEmpty()) {
                emailError.setText("Required");
                emailError.setVisible(true);
                emailField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else if (!email.contains("@") || !email.contains(".")) {
                emailError.setText("Invalid email format");
                emailError.setVisible(true);
                emailField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else if (staffDAO.existsByEmail(email)) {
                emailError.setText("Email already exists");
                emailError.setVisible(true);
                emailField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else {
                emailError.setVisible(false);
                emailField.setStyle(fieldStyle);
            }

            if (password.isEmpty()) {
                passwordError.setText("Required");
                passwordError.setVisible(true);
                passwordField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else if (password.length() < 6) {
                passwordError.setText("Min. 6 characters");
                passwordError.setVisible(true);
                passwordField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else {
                passwordError.setVisible(false);
                passwordField.setStyle(fieldStyle);
            }

            if (valid && !name.isEmpty() && !email.isEmpty() && email.contains("@") && email.contains(".") && password.length() >= 6) {
                okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
                okButton.setDisable(false);
            } else {
                okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
                okButton.setDisable(true);
            }
        };

        nameField.textProperty().addListener(validator);
        emailField.textProperty().addListener(validator);
        passwordField.textProperty().addListener(validator);

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return new String[]{
                    nameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    roleChoice.getValue(),
                    convertTo24Hour(startHour.getValue(), startMinute.getValue(), startAmPm.getValue()),
                    convertTo24Hour(endHour.getValue(), endMinute.getValue(), endAmPm.getValue())
                };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != null && result.length == 6) {
                String name = result[0].trim();
                String email = result[1].trim();
                String password = result[2];
                String role = result[3];
                String startTime = result[4];
                String endTime = result[5];

                if (!name.isEmpty() && !email.isEmpty() && email.contains("@") && email.contains(".") && password.length() >= 6 && startTime != null && endTime != null) {
                    try {
                        LocalTime start = LocalTime.parse(startTime);
                        LocalTime end = LocalTime.parse(endTime);
                        staffDAO.insert(name, email, password, role, start, end);
                        loadStaff();
                    } catch (Exception e) {
                        System.err.println("Invalid time format: " + e.getMessage());
                    }
                }
            }
        });
    }

    private String convertTo24Hour(Integer hour, Integer minute, String amPm) {
        int h = hour;
        if ("PM".equals(amPm) && hour != 12) h += 12;
        if ("AM".equals(amPm) && hour == 12) h = 0;
        return String.format("%02d:%02d", h, minute);
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