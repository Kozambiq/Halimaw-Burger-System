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
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;

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

    @FXML private TextField searchField;
    @FXML private Button btnSearch;

    private ComboDAO comboDAO = new ComboDAO();
    private boolean alreadyLoaded = false;
    private List<Combo> allCombos;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        setActiveNav("Combos & Promos");
        setupTableColumns();
        setupSearchAutocomplete();
        loadCombos();
    }

    private void setupSearchAutocomplete() {
        javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.getStyleClass().add("suggestion-list");
        suggestionList.setFixedCellSize(32);
        suggestionList.setMaxHeight(200);
        suggestionList.setPrefWidth(300);
        suggestionList.setStyle(
            "-fx-background-color: #2e2410;" +
            "-fx-border-color: #4a3820;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 0 4 0;"
        );
        suggestionList.setCellFactory(list -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setTextFill(Color.valueOf("#f5ede0"));
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
                }
            }
        });

        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionList);

        List<String> allComboNames = comboDAO.searchByName("");

        searchField.textProperty().addListener(obs -> {
            String query = searchField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                suggestionPopup.hide();
                return;
            }

            List<String> matches = allComboNames.stream()
                    .filter(name -> name.toLowerCase().contains(query))
                    .collect(Collectors.toList());

            if (!matches.isEmpty()) {
                int rowCount = Math.min(matches.size(), 6);
                suggestionList.setItems(FXCollections.observableArrayList(matches));
                suggestionList.setPrefHeight(rowCount * 32 + 8);
                javafx.geometry.Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal());
                suggestionPopup.show(searchField, bounds.getMinX(), bounds.getMaxY());
            } else {
                suggestionPopup.hide();
            }
        });

        suggestionList.setOnMouseClicked(e -> {
            if (!suggestionList.getSelectionModel().isEmpty()) {
                String selected = suggestionList.getSelectionModel().getSelectedItem();
                searchField.setText(selected);
                suggestionPopup.hide();
            }
        });

        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                suggestionPopup.hide();
            }
        });
    }

    @FXML
    private void onSearchCombo() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadCombos();
            return;
        }

        List<Combo> results = comboDAO.findByName(searchText);

        Platform.runLater(() -> {
            if (!results.isEmpty()) {
                combosTable.setItems(FXCollections.observableArrayList(results));
                combosTable.getSelectionModel().select(0);
                combosTable.scrollTo(0);
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

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colIncludes.setCellValueFactory(new PropertyValueFactory<>("includes"));
        colPromoPrice.setCellValueFactory(new PropertyValueFactory<>("formattedPromoPrice"));
        colSavings.setCellValueFactory(new PropertyValueFactory<>("formattedSavings"));

        colValidUntil.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getValidUntil();
            String formatted = date != null ? date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) : "N/A";
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
                } else if ("Disabled".equals(status)) {
                    pill.getStyleClass().add("pill-disabled");
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
                edit.setOnAction(e -> onEditPromo(combo));

                boolean isActive = "Active".equals(combo.getStatus());

                MenuItem toggle = new MenuItem(isActive ? "Disable" : "Enable");
                toggle.setStyle(isActive
                    ? "-fx-text-fill: #e07070; -fx-font-size: 13px; -fx-padding: 8 16 8 16;"
                    : "-fx-text-fill: #7ec470; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                toggle.setOnAction(e -> {
                    String newStatus = isActive ? "Disabled" : "Active";
                    comboDAO.updateStatus(combo.getId(), newStatus);
                    loadCombos();
                });

                menuBtn.getItems().addAll(edit, toggle);

                setGraphic(menuBtn);
                setText(null);
            }
        });
    }

    private void loadCombos() {
        try {
            int total = comboDAO.getTotalCount();
            int active = comboDAO.getActiveCount();
            allCombos = comboDAO.findAll();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblActive.setText(String.valueOf(active));
                combosTable.setItems(FXCollections.observableArrayList(allCombos));
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

    private void onEditPromo(Combo existingCombo) {
        javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Edit Promo");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        dialog.initOwner(combosTable.getScene().getWindow());

        String labelStyle = "-fx-text-fill: #a09070; -fx-font-size: 12px;";
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7050; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";
        String errorFieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7050; -fx-border-color: #e07070; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";
        String errorStyle = "-fx-text-fill: #e07070; -fx-font-size: 11px; -fx-padding: 4 0 0 0;";
        String removeBtnStyle = "-fx-background-color: transparent; -fx-text-fill: #c8500a; -fx-font-size: 14px; -fx-cursor: hand;";

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setMaxWidth(450);

        Label nameLabel = new Label("Promo Name:");
        nameLabel.setStyle(labelStyle);
        TextField nameField = new TextField(existingCombo.getName());
        nameField.setStyle(fieldStyle);

        Label includesLabel = new Label("Includes:");
        includesLabel.setStyle(labelStyle);

        HBox includesBox = new HBox(8);
        includesBox.setAlignment(Pos.CENTER_LEFT);

        TextField includesField = new TextField();
        includesField.setStyle(fieldStyle);
        includesField.setPromptText("Search menu items...");
        includesField.setPrefWidth(220);

        Label includesError = new Label();
        includesError.setStyle(errorStyle);
        includesError.setVisible(false);

        javafx.scene.control.Button addItemBtn = new javafx.scene.control.Button("Add");
        addItemBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 11px; -fx-cursor: hand;");

        includesBox.getChildren().addAll(includesField, addItemBtn);

        javafx.scene.control.ListView<String> includesSuggestionList = new javafx.scene.control.ListView<>();
        includesSuggestionList.getStyleClass().add("suggestion-list");
        includesSuggestionList.setFixedCellSize(32);
        includesSuggestionList.setMaxHeight(200);
        includesSuggestionList.setPrefWidth(300);
        includesSuggestionList.setStyle(
            "-fx-background-color: #2e2410;" +
            "-fx-border-color: #4a3820;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 0 4 0;"
        );
        includesSuggestionList.setCellFactory(list -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setTextFill(Color.valueOf("#f5ede0"));
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
                }
            }
        });

        javafx.stage.Popup includesSuggestionPopup = new javafx.stage.Popup();
        includesSuggestionPopup.setAutoHide(true);
        includesSuggestionPopup.getContent().add(includesSuggestionList);

        List<String> allMenuItemNames = comboDAO.searchMenuItems("");

        includesField.textProperty().addListener(obs -> {
            includesError.setVisible(false);
            includesField.setStyle(fieldStyle);

            String query = includesField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                includesSuggestionPopup.hide();
                return;
            }

            List<String> matches = allMenuItemNames.stream()
                    .filter(name -> name.toLowerCase().contains(query))
                    .collect(java.util.stream.Collectors.toList());

            if (!matches.isEmpty()) {
                int rowCount = Math.min(matches.size(), 6);
                includesSuggestionList.setItems(FXCollections.observableArrayList(matches));
                includesSuggestionList.setPrefHeight(rowCount * 32 + 8);
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

        VBox selectedItemsBox = new VBox(5);
        selectedItemsBox.setMinHeight(60);

        List<String> selectedItems = new java.util.ArrayList<>(java.util.Arrays.asList(existingCombo.getIncludes().split(" \\+ ")));

        TextField originalPriceInput = new TextField(String.format("%.2f", existingCombo.getOriginalPrice()));
        originalPriceInput.setStyle(fieldStyle);
        originalPriceInput.setEditable(false);

        for (String item : selectedItems) {
            HBox itemRow = new HBox(5);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Label itemLabel = new javafx.scene.control.Label(item.trim());
            itemLabel.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 12px;");

            javafx.scene.control.Button removeBtn = new javafx.scene.control.Button("X");
            removeBtn.setStyle(removeBtnStyle);
            final String itemName = item.trim();
            removeBtn.setOnAction(ev -> {
                selectedItems.remove(itemName);
                selectedItemsBox.getChildren().remove(itemRow);

                double newTotal = 0;
                for (String menuItem : selectedItems) {
                    newTotal += comboDAO.getMenuItemPrice(menuItem);
                }
                originalPriceInput.setText(selectedItems.isEmpty() ? "" : String.format("%.2f", newTotal));
            });

            itemRow.getChildren().addAll(itemLabel, removeBtn);
            selectedItemsBox.getChildren().add(itemRow);
        }

        Label originalPriceLabel = new Label("Original Price:");
        originalPriceLabel.setStyle(labelStyle);

        Label promoPriceLabel = new Label("Promo Price:");
        promoPriceLabel.setStyle(labelStyle);
        TextField promoPriceField = new TextField(String.format("%.2f", existingCombo.getPromoPrice()));
        promoPriceField.setStyle(fieldStyle);
        promoPriceField.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, e -> {
            String text = e.getCharacter();
            if (!text.matches("[0-9.]") || (text.equals(".") && promoPriceField.getText().contains("."))) {
                e.consume();
            }
        });

        Label validUntilLabel = new Label("Valid Until:");
        validUntilLabel.setStyle(labelStyle);
        DatePicker validUntilPicker = new DatePicker(existingCombo.getValidUntil());
        validUntilPicker.setStyle(fieldStyle);

        Label dateError = new Label("Date cannot be in the past");
        dateError.setStyle(errorStyle);
        dateError.setVisible(false);

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(includesLabel, 0, 1);
        grid.add(includesBox, 1, 1);
        grid.add(includesError, 1, 2);
        grid.add(originalPriceLabel, 0, 3);
        grid.add(originalPriceInput, 1, 3);
        grid.add(promoPriceLabel, 0, 4);
        grid.add(promoPriceField, 1, 4);
        grid.add(validUntilLabel, 0, 5);
        grid.add(validUntilPicker, 1, 5);
        grid.add(dateError, 1, 6);
        grid.add(selectedItemsBox, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setMinWidth(550);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("Save");
        okButton.setDisable(true);
        okButton.setStyle("-fx-background-color: #555555; -fx-text-fill: #888888; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: arrow;");

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        Runnable validateForm = () -> {
            String name = nameField.getText().trim();
            String promoPrice = promoPriceField.getText().trim();
            LocalDate validUntil = validUntilPicker.getValue();

            boolean dateInPast = validUntil != null && validUntil.isBefore(LocalDate.now());

            if (dateInPast) {
                dateError.setVisible(true);
                validUntilPicker.setStyle(errorFieldStyle);
            } else {
                dateError.setVisible(false);
                validUntilPicker.setStyle(fieldStyle);
            }

            boolean valid = !name.isEmpty() && !selectedItems.isEmpty() && !promoPrice.isEmpty();

            if (valid) {
                try {
                    Double.parseDouble(promoPrice);
                } catch (NumberFormatException ex) {
                    valid = false;
                }
            }

            if (validUntil == null || dateInPast) {
                valid = false;
            }

            okButton.setDisable(!valid);
            okButton.setStyle(valid
                ? "-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;"
                : "-fx-background-color: #555555; -fx-text-fill: #888888; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: arrow;");
        };

        nameField.textProperty().addListener(obs -> validateForm.run());
        promoPriceField.textProperty().addListener(obs -> validateForm.run());
        validUntilPicker.valueProperty().addListener(obs -> validateForm.run());

        addItemBtn.setOnAction(e -> {
            String item = includesField.getText().trim();
            if (item.isEmpty()) {
                return;
            }

            selectedItems.add(item);
            includesField.clear();
            includesError.setVisible(false);
            includesField.setStyle(fieldStyle);

            double totalPrice = 0;
            for (String menuItem : selectedItems) {
                totalPrice += comboDAO.getMenuItemPrice(menuItem);
            }
            originalPriceInput.setText(String.format("%.2f", totalPrice));

            validateForm.run();

            HBox itemRow = new HBox(5);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Label itemLabel = new javafx.scene.control.Label(item);
            itemLabel.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 12px;");

            javafx.scene.control.Button removeBtn = new javafx.scene.control.Button("X");
            removeBtn.setStyle(removeBtnStyle);
            removeBtn.setOnAction(ev -> {
                selectedItems.remove(item);
                selectedItemsBox.getChildren().remove(itemRow);

                double newTotal = 0;
                for (String menuItem : selectedItems) {
                    newTotal += comboDAO.getMenuItemPrice(menuItem);
                }
                originalPriceInput.setText(selectedItems.isEmpty() ? "" : String.format("%.2f", newTotal));
            });

            itemRow.getChildren().addAll(itemLabel, removeBtn);
            selectedItemsBox.getChildren().add(itemRow);
        });

        validateForm.run();

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return true;
            }
            return false;
        });

        dialog.showAndWait().filter(result -> result).ifPresent(result -> {
            String name = nameField.getText().trim();
            String includes = String.join(" + ", selectedItems);
            String originalPriceText = originalPriceInput.getText().trim();
            String promoPriceText = promoPriceField.getText().trim();
            LocalDate validUntil = validUntilPicker.getValue();

            try {
                double originalPrice = Double.parseDouble(originalPriceText);
                double promoPrice = Double.parseDouble(promoPriceText);
                Date validUntilDate = Date.valueOf(validUntil);

                comboDAO.update(existingCombo.getId(), name, includes, promoPrice, originalPrice, validUntilDate);
                loadCombos();
            } catch (NumberFormatException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("Invalid price");
                err.setContentText("Please enter valid numbers for prices.");
                err.showAndWait();
            }
        });
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
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7050; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";
        String errorFieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7050; -fx-border-color: #e07070; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";
        String errorStyle = "-fx-text-fill: #e07070; -fx-font-size: 11px; -fx-padding: 4 0 0 0;";
        String removeBtnStyle = "-fx-background-color: transparent; -fx-text-fill: #c8500a; -fx-font-size: 14px; -fx-cursor: hand;";

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

        HBox includesBox = new HBox(8);
        includesBox.setAlignment(Pos.CENTER_LEFT);

        TextField includesField = new TextField();
        includesField.setStyle(fieldStyle);
        includesField.setPromptText("Search menu items...");
        includesField.setPrefWidth(220);

        Label includesError = new Label();
        includesError.setStyle(errorStyle);
        includesError.setVisible(false);

        javafx.scene.control.Button addItemBtn = new javafx.scene.control.Button("Add");
        addItemBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 11px; -fx-cursor: hand;");

        includesBox.getChildren().addAll(includesField, addItemBtn);

        javafx.scene.control.ListView<String> includesSuggestionList = new javafx.scene.control.ListView<>();
        includesSuggestionList.getStyleClass().add("suggestion-list");
        includesSuggestionList.setFixedCellSize(32);
        includesSuggestionList.setMaxHeight(200);
        includesSuggestionList.setPrefWidth(300);
        includesSuggestionList.setStyle(
            "-fx-background-color: #2e2410;" +
            "-fx-border-color: #4a3820;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 4 0 4 0;"
        );
        includesSuggestionList.setCellFactory(list -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setTextFill(Color.valueOf("#f5ede0"));
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
                }
            }
        });

        javafx.stage.Popup includesSuggestionPopup = new javafx.stage.Popup();
        includesSuggestionPopup.setAutoHide(true);
        includesSuggestionPopup.getContent().add(includesSuggestionList);

        List<String> allMenuItemNames = comboDAO.searchMenuItems("");

        includesField.textProperty().addListener(obs -> {
            includesError.setVisible(false);
            includesField.setStyle(fieldStyle);

            String query = includesField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                includesSuggestionPopup.hide();
                return;
            }

            List<String> matches = allMenuItemNames.stream()
                    .filter(name -> name.toLowerCase().contains(query))
                    .collect(java.util.stream.Collectors.toList());

            if (!matches.isEmpty()) {
                int rowCount = Math.min(matches.size(), 6);
                includesSuggestionList.setItems(FXCollections.observableArrayList(matches));
                includesSuggestionList.setPrefHeight(rowCount * 32 + 8);
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

        VBox selectedItemsBox = new VBox(5);
        selectedItemsBox.setMinHeight(60);

        List<String> selectedItems = new java.util.ArrayList<>();

        TextField originalPriceInput = new TextField();
        originalPriceInput.setStyle(fieldStyle);
        originalPriceInput.setPromptText("Auto-filled from items");
        originalPriceInput.setEditable(false);

        addItemBtn.setOnAction(e -> {
            String item = includesField.getText().trim();
            if (item.isEmpty()) {
                return;
            }

            selectedItems.add(item);
            includesField.clear();
            includesError.setVisible(false);
            includesField.setStyle(fieldStyle);

            double totalPrice = 0;
            for (String menuItem : selectedItems) {
                totalPrice += comboDAO.getMenuItemPrice(menuItem);
            }
            originalPriceInput.setText(String.format("%.2f", totalPrice));

            HBox itemRow = new HBox(5);
            itemRow.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.control.Label itemLabel = new javafx.scene.control.Label(item);
            itemLabel.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 12px;");

            javafx.scene.control.Button removeBtn = new javafx.scene.control.Button("X");
            removeBtn.setStyle(removeBtnStyle);
            removeBtn.setOnAction(ev -> {
                selectedItems.remove(item);
                selectedItemsBox.getChildren().remove(itemRow);

                double newTotal = 0;
                for (String menuItem : selectedItems) {
                    newTotal += comboDAO.getMenuItemPrice(menuItem);
                }
                originalPriceInput.setText(selectedItems.isEmpty() ? "" : String.format("%.2f", newTotal));
            });

            itemRow.getChildren().addAll(itemLabel, removeBtn);
            selectedItemsBox.getChildren().add(itemRow);
        });

        Label originalPriceLabel = new Label("Original Price:");
        originalPriceLabel.setStyle(labelStyle);

        Label promoPriceLabel = new Label("Promo Price:");
        promoPriceLabel.setStyle(labelStyle);
        TextField promoPriceField = new TextField();
        promoPriceField.setStyle(fieldStyle);
        promoPriceField.setPromptText("e.g., 280.00");
        promoPriceField.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, e -> {
            String text = e.getCharacter();
            if (!text.matches("[0-9.]") || (text.equals(".") && promoPriceField.getText().contains("."))) {
                e.consume();
            }
        });

        Label validUntilLabel = new Label("Valid Until:");
        validUntilLabel.setStyle(labelStyle);
        DatePicker validUntilPicker = new DatePicker();
        validUntilPicker.setStyle(fieldStyle);

        Label dateError = new Label("Date cannot be in the past");
        dateError.setStyle(errorStyle);
        dateError.setVisible(false);

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(includesLabel, 0, 1);
        grid.add(includesBox, 1, 1);
        grid.add(includesError, 1, 2);
        grid.add(originalPriceLabel, 0, 3);
        grid.add(originalPriceInput, 1, 3);
        grid.add(promoPriceLabel, 0, 4);
        grid.add(promoPriceField, 1, 4);
        grid.add(validUntilLabel, 0, 5);
        grid.add(validUntilPicker, 1, 5);
        grid.add(dateError, 1, 6);
        grid.add(selectedItemsBox, 1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setMinWidth(550);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("Save");
        okButton.setDisable(true);
        okButton.setStyle("-fx-background-color: #555555; -fx-text-fill: #888888; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: arrow;");

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        Runnable validateForm = () -> {
            String name = nameField.getText().trim();
            String promoPrice = promoPriceField.getText().trim();
            LocalDate validUntil = validUntilPicker.getValue();

            boolean dateInPast = validUntil != null && validUntil.isBefore(LocalDate.now());

            if (dateInPast) {
                dateError.setVisible(true);
                validUntilPicker.setStyle(errorFieldStyle);
            } else {
                dateError.setVisible(false);
                validUntilPicker.setStyle(fieldStyle);
            }

            boolean valid = !name.isEmpty() && !selectedItems.isEmpty() && !promoPrice.isEmpty();

            if (valid) {
                try {
                    Double.parseDouble(promoPrice);
                } catch (NumberFormatException ex) {
                    valid = false;
                }
            }

            if (validUntil == null || dateInPast) {
                valid = false;
            }

            okButton.setDisable(!valid);
            okButton.setStyle(valid
                ? "-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;"
                : "-fx-background-color: #555555; -fx-text-fill: #888888; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: arrow;");
        };

        nameField.textProperty().addListener(obs -> validateForm.run());
        promoPriceField.textProperty().addListener(obs -> validateForm.run());
        validUntilPicker.valueProperty().addListener(obs -> validateForm.run());

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return true;
            }
            return false;
        });

        dialog.showAndWait().filter(result -> result).ifPresent(result -> {
            String name = nameField.getText().trim();
            String includes = String.join(" + ", selectedItems);
            String originalPriceText = originalPriceInput.getText().trim();
            String promoPriceText = promoPriceField.getText().trim();
            LocalDate validUntil = validUntilPicker.getValue();

            try {
                double originalPrice = Double.parseDouble(originalPriceText);
                double promoPrice = Double.parseDouble(promoPriceText);
                Date validUntilDate = Date.valueOf(validUntil);

                comboDAO.insert(name, includes, promoPrice, originalPrice, validUntilDate);
                loadCombos();
            } catch (NumberFormatException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("Invalid price");
                err.setContentText("Please enter valid numbers for prices.");
                err.showAndWait();
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
        try {
            Main.showOrders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateKitchen() {
        try {
            Main.showKitchen();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        try {
            Main.showSalesReport();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNavigateStaff() {
        try {
            Main.showStaff();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}