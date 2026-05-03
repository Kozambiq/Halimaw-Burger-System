package com.myapp.halimawburgersystem;

import com.myapp.dao.StaffDAO;
import com.myapp.dao.UserDAO;
import com.myapp.model.Staff;
import com.myapp.service.StaffService;
import com.myapp.util.OrderNotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.time.LocalTime;
import java.util.List;

import javafx.scene.control.ComboBox;

public class StaffController extends BaseController {

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
    @FXML private Label topbarDate;

    private StaffService staffService = new StaffService();
    private boolean alreadyLoaded = false;
    private List<Staff> allStaff;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        updateTopbarDate();
        setActiveNav("Staff");
        setupTableColumns();
        loadStaff();

        // Subscribe to instant updates (e.g., when other staff change status)
        OrderNotificationService.subscribe(this::loadStaff);
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setCellFactory(col -> new TableCell<Staff, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-name"); }
            }
        });

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colShiftHours.setCellValueFactory(new PropertyValueFactory<>("shiftHours"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<Staff, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null); setText(null); return;
                }

                Label pill = new Label(status.toUpperCase());
                pill.getStyleClass().add("status-pill");

                if ("Active".equals(status)) {
                    pill.getStyleClass().add("pill-active");
                } else if ("Break".equals(status)) {
                    pill.getStyleClass().add("pill-break");
                } else {
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
                if (empty) { setGraphic(null); setText(null); return; }

                Staff staff = getTableView().getItems().get(getIndex());
                if (staff == null) { setGraphic(null); return; }

                StackPane avatarCircle = new StackPane();
                avatarCircle.getStyleClass().add("staff-avatar-circle");
                Label initials = new Label(staff.getInitials().toUpperCase());
                initials.getStyleClass().add("staff-avatar-text");
                avatarCircle.getChildren().add(initials);
                
                HBox box = new HBox(avatarCircle);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<Staff, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); setText(null); return; }

                final Staff staff = getTableView().getItems().get(getIndex());
                if (staff == null) { setGraphic(null); return; }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem edit = new MenuItem("Profile Details");
                edit.setOnAction(e -> showEditStaffDialog(staff));

                if ("Active".equals(staff.getStatus())) {
                    MenuItem onBreak = new MenuItem("Put on Break");
                    onBreak.setOnAction(e -> updateStatus(staff.getId(), "Break"));
                    MenuItem offShift = new MenuItem("End Shift");
                    offShift.setOnAction(e -> updateStatus(staff.getId(), "Off Shift"));
                    menuBtn.getItems().addAll(edit, onBreak, offShift);
                } else if ("Break".equals(staff.getStatus())) {
                    MenuItem active = new MenuItem("Resume Shift");
                    active.setOnAction(e -> updateStatus(staff.getId(), "Active"));
                    MenuItem offShift = new MenuItem("End Shift");
                    offShift.setOnAction(e -> updateStatus(staff.getId(), "Off Shift"));
                    menuBtn.getItems().addAll(edit, active, offShift);
                } else {
                    MenuItem active = new MenuItem("Start Shift");
                    active.setOnAction(e -> updateStatus(staff.getId(), "Active"));
                    MenuItem remove = new MenuItem("Remove Access");
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
        if (staffService.updateStatus(id, status)) {
            // Signal to others (Dashboard) that staff status changed
            OrderNotificationService.broadcastUpdate();
            loadStaff();
        }
    }

    private void loadStaff() {
        try {
            int total = staffService.getTotalCount();
            int active = staffService.getActiveCount();
            int onBreak = staffService.getOnBreakCount();
            allStaff = staffService.findAll();

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
        javafx.scene.layout.StackPane passwordBox = new javafx.scene.layout.StackPane();
        PasswordField passwordField = new PasswordField();
        TextField passwordVisibleField = new TextField();
        String originalPassword = "";
        if (staffService.findPasswordHashByStaffId(staff.getId()).isPresent()) {
            String hash = staffService.findPasswordHashByStaffId(staff.getId()).get();
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
        eyeIcon.setMouseTransparent(true);

        Button togglePassword = new Button();
        togglePassword.setGraphic(eyeIcon);
        togglePassword.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 8 0 0;");
        javafx.scene.layout.StackPane.setAlignment(togglePassword, javafx.geometry.Pos.CENTER_RIGHT);

        passwordBox.getChildren().addAll(passwordField, passwordVisibleField, togglePassword);

        final boolean[] passwordVisible = {false};
        togglePassword.setOnAction(e -> {
            passwordVisible[0] = !passwordVisible[0];
            if (passwordVisible[0]) {
                passwordVisibleField.setText(passwordField.getText());
                passwordVisibleField.setVisible(true);
                passwordField.setVisible(false);
                eyeIcon.setContent("M18 6.5L9.5 15 2 6.5l1.5 1.5 6 6-6 6 1.5 1.5 9.5-9.5-6-6zm-6 8.5c-2.76 0-5-2.24-5-5s2.24-5 5-5c.55 0 1.05.09 1.53.24l1.5-1.5c-.71-.28-1.47-.43-2.27-.43-2.76 0-5 2.24-5 5s2.24 5 5 5c.8 0 1.56-.15 2.27-.43l1.5-1.5c-.48-.15-.98-.24-1.53-.24z");
            } else {
                passwordField.setText(passwordVisibleField.getText());
                passwordField.setVisible(true);
                passwordVisibleField.setVisible(false);
                eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
            }
        });

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
                    passwordVisible[0] ? passwordVisibleField.getText() : passwordField.getText(),
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
                    staffService.updateName(staff.getId(), name);
                    staff.setName(name);

                    if (!email.isEmpty() && !email.equals(staff.getEmail())) {
                        staffService.updateEmail(staff.getId(), email);
                        staff.setEmail(email);
                    }

                    if (password != null && !password.equals(finalOriginalPassword) && !password.isEmpty() && !password.equals("••••••••")) {
                        staffService.updatePasswordByStaffId(staff.getId(), password);
                    }

                    staffService.updateRole(staff.getId(), role);
                    staff.setRole(role);

                    try {
                        LocalTime start = startTime != null ? LocalTime.parse(startTime) : null;
                        LocalTime end = endTime != null ? LocalTime.parse(endTime) : null;
                        if (start != null && end != null) {
                            staffService.updateShift(staff.getId(), start, end);
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
            staffService.delete(staff.getId());
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
            } else if (staffService.existsByName(name)) {
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
            } else if (staffService.existsByEmail(email)) {
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
                        staffService.insert(name, email, password, role, start, end);
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

    @Override
    protected Button[] getNavButtons() {
        return new Button[] {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
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
        try {
            Main.showOrders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateKitchen(ActionEvent event) {
        try {
            Main.showKitchen();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        try {
            Main.showSalesReport();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void updateTopbarDate() {
        if (topbarDate != null) {
            String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d yyyy"));
            topbarDate.setText(date);
        }
    }
}