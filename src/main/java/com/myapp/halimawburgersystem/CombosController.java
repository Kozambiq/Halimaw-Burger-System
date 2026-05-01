package com.myapp.halimawburgersystem;

import com.myapp.dao.ComboDAO;
import com.myapp.model.Combo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

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
        suggestionList.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        suggestionList.setFixedCellSize(40);
        suggestionList.setPrefWidth(300);

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
        colName.setCellFactory(col -> new TableCell<Combo, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-name"); }
            }
        });

        colIncludes.setCellValueFactory(new PropertyValueFactory<>("includes"));
        
        colPromoPrice.setCellValueFactory(new PropertyValueFactory<>("formattedPromoPrice"));
        colPromoPrice.setCellFactory(col -> new TableCell<Combo, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-price"); }
            }
        });

        colSavings.setCellValueFactory(new PropertyValueFactory<>("formattedSavings"));
        colSavings.setCellFactory(col -> new TableCell<Combo, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-savings"); }
            }
        });

        colValidUntil.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getValidUntil();
            String formatted = date != null ? date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "N/A";
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
        colValidUntil.setCellFactory(col -> new TableCell<Combo, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-date"); }
            }
        });

        combosTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<Combo, String>() {
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
                } else {
                    pill.getStyleClass().add("pill-expired");
                }

                setGraphic(pill);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<Combo, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); setText(null); return; }

                Combo combo = getTableView().getItems().get(getIndex());
                if (combo == null) { setGraphic(null); return; }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem edit = new MenuItem("Edit Details");
                edit.setOnAction(e -> onEditPromo(combo));

                boolean isActive = "Active".equals(combo.getStatus());
                MenuItem toggle = new MenuItem(isActive ? "Disable Promo" : "Enable Promo");
                toggle.setOnAction(e -> {
                    comboDAO.updateStatus(combo.getId(), isActive ? "Disabled" : "Active");
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
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("EDIT PROMO");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

        Label headerLabel = new Label("Edit Promo Details");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        HBox mainLayout = new HBox(32);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        VBox colLeft = new VBox(28);
        colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
        colLeft.setPrefWidth(340);

        VBox nameBox = new VBox(8);
        Label nameEyebrow = new Label("PROMO NAME");
        nameEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField nameField = new TextField(existingCombo.getName());
        nameField.getStyleClass().add("premium-field");
        nameBox.getChildren().addAll(nameEyebrow, nameField);

        VBox promoPriceBox = new VBox(8);
        Label promoPriceEyebrow = new Label("PROMO PRICE (PHP)");
        promoPriceEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField promoPriceField = new TextField(String.format("%.2f", existingCombo.getPromoPrice()));
        promoPriceField.getStyleClass().add("premium-field");
        promoPriceBox.getChildren().addAll(promoPriceEyebrow, promoPriceField);

        VBox dateBox = new VBox(8);
        Label dateEyebrow = new Label("VALID UNTIL");
        dateEyebrow.getStyleClass().add("dialog-eyebrow");
        DatePicker validUntilPicker = new DatePicker(existingCombo.getValidUntil());
        validUntilPicker.setMaxWidth(Double.MAX_VALUE);
        validUntilPicker.getStyleClass().add("premium-field");
        dateBox.getChildren().addAll(dateEyebrow, validUntilPicker);

        colLeft.getChildren().addAll(nameBox, promoPriceBox, dateBox);

        VBox colRight = new VBox(24);
        colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
        colRight.setPrefWidth(400);

        Label itemsEyebrow = new Label("COMBO INCLUSIONS");
        itemsEyebrow.getStyleClass().add("dialog-eyebrow");

        VBox searchArea = new VBox(12);
        HBox searchInputs = new HBox(10);
        searchInputs.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox itemSearchWrapper = new HBox(10);
        itemSearchWrapper.getStyleClass().add("search-bar-group");
        itemSearchWrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(itemSearchWrapper, javafx.scene.layout.Priority.ALWAYS);

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.getStyleClass().add("search-icon-svg");
        searchIcon.setScaleX(0.7); searchIcon.setScaleY(0.7);

        TextField itemSearchField = new TextField();
        itemSearchField.getStyleClass().add("premium-field");
        itemSearchField.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 8 0 8 0;");
        itemSearchField.setPromptText("Search menu items...");
        HBox.setHgrow(itemSearchField, javafx.scene.layout.Priority.ALWAYS);
        itemSearchWrapper.getChildren().addAll(searchIcon, itemSearchField);

        Button addBtn = new Button("ADD");
        addBtn.getStyleClass().add("btn-recipe-add");

        searchInputs.getChildren().addAll(itemSearchWrapper, addBtn);

        VBox originalPriceBox = new VBox(8);
        Label origPriceEyebrow = new Label("TOTAL ORIGINAL PRICE (AUTO)");
        origPriceEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField originalPriceField = new TextField(String.format("%.2f", existingCombo.getOriginalPrice()));
        originalPriceField.setEditable(false);
        originalPriceField.getStyleClass().add("premium-field");
        originalPriceField.setStyle(originalPriceField.getStyle() + "-fx-opacity: 0.7;");
        originalPriceBox.getChildren().addAll(origPriceEyebrow, originalPriceField);

        searchArea.getChildren().addAll(itemsEyebrow, searchInputs, originalPriceBox);

        javafx.scene.control.ScrollPane itemsScroll = new javafx.scene.control.ScrollPane();
        itemsScroll.setFitToWidth(true);
        itemsScroll.setPrefHeight(180);
        itemsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        
        VBox selectedItemsContainer = new VBox(10);
        selectedItemsContainer.setPadding(new Insets(4, 0, 0, 0));
        itemsScroll.setContent(selectedItemsContainer);

        colRight.getChildren().addAll(searchArea, itemsScroll);
        mainLayout.getChildren().addAll(colLeft, colRight);

        List<String> selectedItems = new java.util.ArrayList<>(java.util.Arrays.asList(existingCombo.getIncludes().split(" \\+ ")));
        for (String item : selectedItems) {
            selectedItemsContainer.getChildren().add(createItemChip(item, selectedItems, selectedItemsContainer, originalPriceField));
        }

        List<String> allMenuItemNames = comboDAO.searchMenuItems("");
        javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.getStyleClass().add("suggestion-list");
        suggestionList.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        suggestionList.setFixedCellSize(40);
        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionList);

        itemSearchField.textProperty().addListener(obs -> {
            String q = itemSearchField.getText().trim().toLowerCase();
            if (q.isEmpty()) { suggestionPopup.hide(); return; }
            List<String> matches = allMenuItemNames.stream().filter(n -> n.toLowerCase().contains(q)).collect(Collectors.toList());
            if (!matches.isEmpty()) {
                suggestionList.setItems(FXCollections.observableArrayList(matches));
                suggestionList.setPrefHeight(Math.min(matches.size(), 5) * 40 + 8);
                suggestionList.setPrefWidth(itemSearchWrapper.getWidth());
                javafx.geometry.Bounds bounds = itemSearchWrapper.localToScreen(itemSearchWrapper.getBoundsInLocal());
                suggestionPopup.show(itemSearchWrapper, bounds.getMinX(), bounds.getMaxY() + 4);
            } else { suggestionPopup.hide(); }
        });

        suggestionList.setOnMouseClicked(e -> {
            if (!suggestionList.getSelectionModel().isEmpty()) {
                itemSearchField.setText(suggestionList.getSelectionModel().getSelectedItem());
                suggestionPopup.hide();
            }
        });

        addBtn.setOnAction(e -> {
            String item = itemSearchField.getText().trim();
            if (item.isEmpty() || selectedItems.contains(item)) return;
            selectedItems.add(item);
            selectedItemsContainer.getChildren().add(createItemChip(item, selectedItems, selectedItemsContainer, originalPriceField));
            double total = 0;
            for (String s : selectedItems) total += comboDAO.getMenuItemPrice(s);
            originalPriceField.setText(String.format("%.2f", total));
            itemSearchField.clear();
        });

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("UPDATE PROMO");
        okButton.getStyleClass().add("dialog-button-save");
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-cancel");

        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK);

        okButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String pPrice = promoPriceField.getText().trim();
            LocalDate date = validUntilPicker.getValue();
            if (name.isEmpty() || pPrice.isEmpty() || date == null || selectedItems.isEmpty()) {
                e.consume(); return;
            }
            try {
                double promoPrice = Double.parseDouble(pPrice);
                double originalPrice = Double.parseDouble(originalPriceField.getText());
                comboDAO.update(existingCombo.getId(), name, String.join(" + ", selectedItems), promoPrice, originalPrice, java.sql.Date.valueOf(date));
                loadCombos();
                dialog.close();
            } catch (Exception ex) { e.consume(); }
        });

        dialog.showAndWait();
    }

    @FXML
    private void onAddPromo() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("CREATE NEW PROMO");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        // Load Stylesheets
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

        // Header
        Label headerLabel = new Label("Create New Promo");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        // Main Layout
        HBox mainLayout = new HBox(32);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        // LEFT COLUMN: Promo Identity
        VBox colLeft = new VBox(28);
        colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
        colLeft.setPrefWidth(340);

        VBox nameBox = new VBox(8);
        Label nameEyebrow = new Label("PROMO NAME");
        nameEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Halimaw Meal Deal");
        nameField.getStyleClass().add("premium-field");
        nameBox.getChildren().addAll(nameEyebrow, nameField);

        VBox promoPriceBox = new VBox(8);
        Label promoPriceEyebrow = new Label("PROMO PRICE (PHP)");
        promoPriceEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField promoPriceField = new TextField();
        promoPriceField.setPromptText("0.00");
        promoPriceField.getStyleClass().add("premium-field");
        promoPriceBox.getChildren().addAll(promoPriceEyebrow, promoPriceField);

        VBox dateBox = new VBox(8);
        Label dateEyebrow = new Label("VALID UNTIL");
        dateEyebrow.getStyleClass().add("dialog-eyebrow");
        DatePicker validUntilPicker = new DatePicker();
        validUntilPicker.setMaxWidth(Double.MAX_VALUE);
        validUntilPicker.getStyleClass().add("premium-field");
        dateBox.getChildren().addAll(dateEyebrow, validUntilPicker);

        colLeft.getChildren().addAll(nameBox, promoPriceBox, dateBox);

        // RIGHT COLUMN: Combo Items
        VBox colRight = new VBox(24);
        colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
        colRight.setPrefWidth(400);

        Label itemsEyebrow = new Label("COMBO INCLUSIONS");
        itemsEyebrow.getStyleClass().add("dialog-eyebrow");

        VBox searchArea = new VBox(12);
        HBox searchInputs = new HBox(10);
        searchInputs.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox itemSearchWrapper = new HBox(10);
        itemSearchWrapper.getStyleClass().add("search-bar-group");
        itemSearchWrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(itemSearchWrapper, javafx.scene.layout.Priority.ALWAYS);

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.getStyleClass().add("search-icon-svg");
        searchIcon.setScaleX(0.7); searchIcon.setScaleY(0.7);

        TextField itemSearchField = new TextField();
        itemSearchField.getStyleClass().add("premium-field");
        itemSearchField.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 8 0 8 0;");
        itemSearchField.setPromptText("Search menu items...");
        HBox.setHgrow(itemSearchField, javafx.scene.layout.Priority.ALWAYS);
        itemSearchWrapper.getChildren().addAll(searchIcon, itemSearchField);

        Button addBtn = new Button("ADD");
        addBtn.getStyleClass().add("btn-recipe-add");

        searchInputs.getChildren().addAll(itemSearchWrapper, addBtn);

        VBox originalPriceBox = new VBox(8);
        Label origPriceEyebrow = new Label("TOTAL ORIGINAL PRICE (AUTO)");
        origPriceEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField originalPriceField = new TextField();
        originalPriceField.setEditable(false);
        originalPriceField.getStyleClass().add("premium-field");
        originalPriceField.setStyle(originalPriceField.getStyle() + "-fx-opacity: 0.7;");
        originalPriceBox.getChildren().addAll(origPriceEyebrow, originalPriceField);

        searchArea.getChildren().addAll(itemsEyebrow, searchInputs, originalPriceBox);

        javafx.scene.control.ScrollPane itemsScroll = new javafx.scene.control.ScrollPane();
        itemsScroll.setFitToWidth(true);
        itemsScroll.setPrefHeight(180);
        itemsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        
        VBox selectedItemsContainer = new VBox(10);
        selectedItemsContainer.setPadding(new Insets(4, 0, 0, 0));
        itemsScroll.setContent(selectedItemsContainer);

        colRight.getChildren().addAll(searchArea, itemsScroll);
        mainLayout.getChildren().addAll(colLeft, colRight);

        // Logic
        List<String> selectedItems = new java.util.ArrayList<>();
        List<String> allMenuItemNames = comboDAO.searchMenuItems("");

        // Autocomplete
        javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.getStyleClass().add("suggestion-list");
        suggestionList.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        suggestionList.setFixedCellSize(40);
        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionList);

        itemSearchField.textProperty().addListener(obs -> {
            String q = itemSearchField.getText().trim().toLowerCase();
            if (q.isEmpty()) { suggestionPopup.hide(); return; }
            List<String> matches = allMenuItemNames.stream().filter(n -> n.toLowerCase().contains(q)).collect(Collectors.toList());
            if (!matches.isEmpty()) {
                suggestionList.setItems(FXCollections.observableArrayList(matches));
                suggestionList.setPrefHeight(Math.min(matches.size(), 5) * 40 + 8);
                suggestionList.setPrefWidth(itemSearchWrapper.getWidth());
                javafx.geometry.Bounds bounds = itemSearchWrapper.localToScreen(itemSearchWrapper.getBoundsInLocal());
                suggestionPopup.show(itemSearchWrapper, bounds.getMinX(), bounds.getMaxY() + 4);
            } else { suggestionPopup.hide(); }
        });

        suggestionList.setOnMouseClicked(e -> {
            if (!suggestionList.getSelectionModel().isEmpty()) {
                itemSearchField.setText(suggestionList.getSelectionModel().getSelectedItem());
                suggestionPopup.hide();
            }
        });

        addBtn.setOnAction(e -> {
            String item = itemSearchField.getText().trim();
            if (item.isEmpty() || selectedItems.contains(item)) return;
            
            selectedItems.add(item);
            selectedItemsContainer.getChildren().add(createItemChip(item, selectedItems, selectedItemsContainer, originalPriceField));
            
            double total = 0;
            for (String s : selectedItems) total += comboDAO.getMenuItemPrice(s);
            originalPriceField.setText(String.format("%.2f", total));
            itemSearchField.clear();
        });

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("SAVE PROMO");
        okButton.getStyleClass().add("dialog-button-save");
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-cancel");

        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK);

        okButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String pPrice = promoPriceField.getText().trim();
            LocalDate date = validUntilPicker.getValue();
            if (name.isEmpty() || pPrice.isEmpty() || date == null || selectedItems.isEmpty()) {
                e.consume(); return;
            }
            try {
                double promoPrice = Double.parseDouble(pPrice);
                double originalPrice = Double.parseDouble(originalPriceField.getText());
                comboDAO.insert(name, String.join(" + ", selectedItems), promoPrice, originalPrice, java.sql.Date.valueOf(date));
                loadCombos();
                dialog.close();
            } catch (Exception ex) { e.consume(); }
        });

        dialog.showAndWait();
    }

    private HBox createItemChip(String itemName, List<String> selectedItems, VBox container, TextField originalPriceInput) {
        HBox chip = new HBox(12);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("ingredient-chip");

        Label name = new Label(itemName.toUpperCase());
        name.getStyleClass().add("chip-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("btn-chip-remove");
        removeBtn.setOnAction(e -> {
            selectedItems.remove(itemName);
            container.getChildren().remove(chip);
            
            double newTotal = 0;
            for (String menuItem : selectedItems) {
                newTotal += comboDAO.getMenuItemPrice(menuItem);
            }
            originalPriceInput.setText(selectedItems.isEmpty() ? "" : String.format("%.2f", newTotal));
        });

        chip.getChildren().addAll(name, spacer, removeBtn);
        return chip;
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