package com.myapp.halimawburgersystem;

import com.myapp.dao.ComboDAO;
import com.myapp.model.Combo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class CombosController {

    @FXML private Label lblTotal;
    @FXML private Label lblActive;
    @FXML private TableView<Combo> combosTable;
    @FXML private TableColumn<Combo, String> colName;
    @FXML private TableColumn<Combo, String> colIncludes;
    @FXML private TableColumn<Combo, String> colPromoPrice;
    @FXML private TableColumn<Combo, String> colSavings;
    @FXML private TableColumn<Combo, String> colValidUntil;
    @FXML private TableColumn<Combo, String> colStatus;
    @FXML private TableColumn<Combo, String> colActions;

    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    private ComboDAO comboDAO = new ComboDAO();
    private boolean alreadyLoaded = false;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        setActiveNav("Combos & Promos");
        setupTableColumns();
        loadCombos();
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

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colIncludes.setCellValueFactory(new PropertyValueFactory<>("includes"));
        colPromoPrice.setCellValueFactory(new PropertyValueFactory<>("formattedPromoPrice"));
        colSavings.setCellValueFactory(new PropertyValueFactory<>("formattedSavings"));

        colValidUntil.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getValidUntil();
            String formatted = date != null ? date.toString() : "N/A";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        combosTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<Combo, String>() {
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
                } else if ("Scheduled".equals(status)) {
                    pill.getStyleClass().add("pill-low");
                } else if ("Expired".equals(status)) {
                    pill.getStyleClass().add("pill-out");
                }

                setGraphic(pill);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<Combo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Combo combo = getTableView().getItems().get(getIndex());
                if (combo == null) {
                    setGraphic(null);
                    return;
                }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem edit = new MenuItem("Edit");
                edit.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");

                MenuItem delete = new MenuItem("Delete");
                delete.setStyle("-fx-text-fill: #e07070; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                delete.setOnAction(e -> showDeleteConfirmation(combo));

                menuBtn.getItems().addAll(edit, delete);

                setGraphic(menuBtn);
                setText(null);
            }
        });
    }

    private void loadCombos() {
        try {
            int total = comboDAO.getTotalCount();
            int active = comboDAO.getActiveCount();
            List<Combo> combos = comboDAO.findAll();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblActive.setText(String.valueOf(active));
                combosTable.setItems(FXCollections.observableArrayList(combos));
            });
        } catch (Exception e) {
            System.err.println("Error loading combos: " + e.getMessage());
        }
    }

    private void showDeleteConfirmation(Combo combo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Combo");
        alert.setHeaderText("Delete " + combo.getName() + "?");
        alert.setContentText("This action cannot be undone.");
        alert.setGraphic(null);
        alert.getDialogPane().setGraphic(null);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        alert.initOwner(combosTable.getScene().getWindow());

        alert.getDialogPane().applyCss();
        alert.getDialogPane().layout();

        javafx.scene.Node header = alert.getDialogPane().lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: transparent;");
        }

        javafx.scene.control.Label headerText = (javafx.scene.control.Label) alert.getDialogPane().lookup(".header-panel .label");
        if (headerText == null) {
            javafx.scene.Node hdrPanel = alert.getDialogPane().lookup(".header-panel");
            if (hdrPanel != null) {
                for (javafx.scene.Node n : hdrPanel.lookupAll(".label")) {
                    n.setStyle("-fx-text-fill: #e07070; -fx-font-size: 14px;");
                }
            }
        } else {
            headerText.setStyle("-fx-text-fill: #e07070; -fx-font-size: 14px;");
        }

        javafx.scene.Node content = alert.getDialogPane().lookup(".content");
        if (content != null) {
            content.setStyle("-fx-text-fill: #e07070; -fx-font-size: 13px;");
        }

        javafx.scene.control.ButtonType okType = new javafx.scene.control.ButtonType("Delete");
        javafx.scene.control.ButtonType cancelType = new javafx.scene.control.ButtonType("Cancel");
        alert.getDialogPane().getButtonTypes().setAll(okType, cancelType);

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(okType);
        okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        okButton.setOnAction(e -> {
            comboDAO.delete(combo.getId());
            loadCombos();
        });

        javafx.scene.control.Button cancelButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancelType);
        cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        alert.showAndWait();
    }

    @FXML
    private void onAddPromo() {
        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Add Promo");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        dialog.initOwner(combosTable.getScene().getWindow());

        String labelStyle = "-fx-text-fill: #a09070; -fx-font-size: 12px;";
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setMaxWidth(450);

        Label nameLabel = new Label("Promo Name:");
        nameLabel.setStyle(labelStyle);
        TextField nameField = new TextField();
        nameField.setStyle(fieldStyle);
        nameField.setPromptText("e.g., Halimar Meal Deal");

        Label includesLabel = new Label("Includes:");
        includesLabel.setStyle(labelStyle);

        TextField includesField = new TextField();
        includesField.setStyle(fieldStyle);
        includesField.setPromptText("Search menu items...");
        includesField.setPrefWidth(280);

        javafx.scene.control.ListView<String> includesSuggestionList = new javafx.scene.control.ListView<>();
        includesSuggestionList.getStyleClass().add("suggestion-list");
        includesSuggestionList.setFixedCellSize(24);
        includesSuggestionList.setMaxHeight(150);
        includesSuggestionList.setPrefWidth(280);

        javafx.stage.Popup includesSuggestionPopup = new javafx.stage.Popup();
        includesSuggestionPopup.setAutoHide(true);
        includesSuggestionPopup.getContent().add(includesSuggestionList);

        List<String> allMenuItemNames = comboDAO.searchMenuItems("");

        includesField.textProperty().addListener(obs -> {
            String query = includesField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                includesSuggestionPopup.hide();
                return;
            }

            List<String> matches = allMenuItemNames.stream()
                    .filter(name -> name.toLowerCase().contains(query))
                    .limit(10)
                    .collect(java.util.stream.Collectors.toList());

            if (!matches.isEmpty()) {
                int rowCount = Math.min(matches.size(), 6);
                includesSuggestionList.setItems(FXCollections.observableArrayList(matches));
                includesSuggestionList.setPrefHeight(rowCount * 24 + 8);
                javafx.geometry.Bounds bounds = includesField.localToScreen(includesField.getBoundsInLocal());
                includesSuggestionPopup.show(includesField, bounds.getMinX(), bounds.getMaxY());
            } else {
                includesSuggestionPopup.hide();
            }
        });

        includesSuggestionList.setOnMouseClicked(e -> {
            if (!includesSuggestionList.getSelectionModel().isEmpty()) {
                String selected = includesSuggestionList.getSelectionModel().getSelectedItem();
                includesField.setText(selected);
                includesSuggestionPopup.hide();
            }
        });

        includesField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                includesSuggestionPopup.hide();
            }
        });

        Label originalPriceLabel = new Label("Original Price:");
        originalPriceLabel.setStyle(labelStyle);
        TextField originalPriceField = new TextField();
        originalPriceField.setStyle(fieldStyle);
        originalPriceField.setPromptText("e.g., 325.00");

        Label promoPriceLabel = new Label("Promo Price:");
        promoPriceLabel.setStyle(labelStyle);
        TextField promoPriceField = new TextField();
        promoPriceField.setStyle(fieldStyle);
        promoPriceField.setPromptText("e.g., 280.00");

        Label validUntilLabel = new Label("Valid Until:");
        validUntilLabel.setStyle(labelStyle);
        DatePicker validUntilPicker = new DatePicker();
        validUntilPicker.setStyle(fieldStyle);

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(includesLabel, 0, 1);
        grid.add(includesField, 1, 1);
        grid.add(originalPriceLabel, 0, 2);
        grid.add(originalPriceField, 1, 2);
        grid.add(promoPriceLabel, 0, 3);
        grid.add(promoPriceField, 1, 3);
        grid.add(validUntilLabel, 0, 4);
        grid.add(validUntilPicker, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setMinWidth(550);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("Save");
        okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return true;
            }
            return false;
        });

        dialog.showAndWait().filter(result -> result).ifPresent(result -> {
            String name = nameField.getText().trim();
            String includes = includesField.getText().trim();
            String originalPriceText = originalPriceField.getText().trim();
            String promoPriceText = promoPriceField.getText().trim();
            LocalDate validUntil = validUntilPicker.getValue();

            if (!name.isEmpty() && !includes.isEmpty()) {
                try {
                    double originalPrice = Double.parseDouble(originalPriceText);
                    double promoPrice = Double.parseDouble(promoPriceText);
                    Date validUntilDate = validUntil != null ? Date.valueOf(validUntil) : null;

                    comboDAO.insert(name, includes, promoPrice, originalPrice, validUntilDate);
                    loadCombos();
                } catch (NumberFormatException ex) {
                    Alert err = new Alert(Alert.AlertType.ERROR);
                    err.setTitle("Error");
                    err.setHeaderText("Invalid price");
                    err.setContentText("Please enter valid numbers for prices.");
                    err.showAndWait();
                }
            }
        });
    }

    @FXML
    private void onLogout() {
        try {
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateDashboard() {
        try {
            Main.showDashboard();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateOrders() {
        System.out.println("Orders - Coming soon");
    }

    @FXML
    private void onNavigateKitchen() {
        System.out.println("Kitchen Queue - Coming soon");
    }

    @FXML
    private void onNavigateMenuItems() {
        try {
            Main.showMenuItems();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateCombos() {
        try {
            Main.showCombos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateInventory() {
        try {
            Main.showInventory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateSales() {
        System.out.println("Sales Reports - Coming soon");
    }

    @FXML
    private void onNavigateStaff() {
        System.out.println("Staff - Coming soon");
    }
}