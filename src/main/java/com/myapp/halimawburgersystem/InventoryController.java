package com.myapp.halimawburgersystem;

import com.myapp.dao.IngredientDAO;
import com.myapp.model.Ingredient;
import com.myapp.service.InventoryService;
import com.myapp.util.OrderNotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.util.Callback;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InventoryController extends BaseController {

    @FXML private Label lblTotal;
    @FXML private Label lblLowStock;
    @FXML private Label lblOutOfStock;
    @FXML private TableView<Ingredient> inventoryTable;
    @FXML private TableColumn<Ingredient, String> colName;
    @FXML private TableColumn<Ingredient, Integer> colStockLevel;
    @FXML private TableColumn<Ingredient, String> colQuantity;
    @FXML private TableColumn<Ingredient, String> colUnit;
    @FXML private TableColumn<Ingredient, String> colStatus;
    @FXML private TableColumn<Ingredient, String> colActions;
    
    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;
    @FXML private Label topbarDate;

    @FXML private TextField searchField;
    @FXML private Button btnSearch;

    private InventoryService inventoryService = new InventoryService();
    private boolean alreadyLoaded = false;
    private List<Ingredient> allIngredients;
    private ScheduledExecutorService timerService;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        updateTopbarDate();
        setActiveNav("Inventory");
        setupTableColumns();
        setupSearchAutocomplete();
        loadInventory();
        
        // Subscribe to instant updates from orders (stock reservations/deductions)
        OrderNotificationService.subscribe(this::loadInventory);
        
        timerService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        // Polling as a backup (failsafe)
        timerService.scheduleAtFixedRate(() -> Platform.runLater(this::loadInventory), 60, 60, TimeUnit.SECONDS);
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

        List<String> allIngredientNames = inventoryService.searchByName("");

        searchField.textProperty().addListener(obs -> {
            String query = searchField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                suggestionPopup.hide();
                return;
            }

            List<String> matches = allIngredientNames.stream()
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
    private void onSearchIngredient() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadInventory();
            return;
        }

        List<Ingredient> results = inventoryService.findByName(searchText);

        Platform.runLater(() -> {
            if (!results.isEmpty()) {
                inventoryTable.setItems(FXCollections.observableArrayList(results));
                inventoryTable.getSelectionModel().select(0);
                inventoryTable.scrollTo(0);
            }
        });
    }

    @Override
    protected Button[] getNavButtons() {
        return new Button[] {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
    }

    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setCellFactory(col -> new TableCell<Ingredient, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-name"); }
            }
        });

        colStockLevel.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleObjectProperty<>(
                cellData.getValue().getStockPercentage()
            )
        );
        colStockLevel.setCellFactory(col -> new TableCell<Ingredient, Integer>() {
            @Override
            protected void updateItem(Integer percentage, boolean empty) {
                super.updateItem(percentage, empty);
                if (empty || percentage == null) {
                    setGraphic(null); setText(null); return;
                }

                Ingredient ing = getTableView().getItems().get(getIndex());
                String status = ing.getStatus();
                final double BAR_WIDTH = 160.0;
                double maxStock = ing.getMaxStock();
                double minThreshold = ing.getMinThreshold();

                StackPane container = new StackPane();
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPrefWidth(BAR_WIDTH);
                container.setMaxWidth(BAR_WIDTH);

                Region barTrack = new Region();
                barTrack.getStyleClass().add("stock-bar-track");
                barTrack.setPrefHeight(8);
                barTrack.setPrefWidth(BAR_WIDTH);

                Region barFill = new Region();
                barFill.setPrefHeight(8);
                double fillWidth = Math.min(percentage * (BAR_WIDTH / 100.0), BAR_WIDTH);
                barFill.setPrefWidth(fillWidth);
                barFill.setMaxWidth(fillWidth);

                if ("OK".equals(status)) {
                    barFill.getStyleClass().add("stock-bar-fill-ok");
                } else if ("Low".equals(status)) {
                    barFill.getStyleClass().add("stock-bar-fill-low");
                } else {
                    barFill.getStyleClass().add("stock-bar-fill-out");
                }

                Pane markerLayer = new Pane();
                markerLayer.setPrefSize(BAR_WIDTH, 8);
                
                if (maxStock > 0) {
                    // Low Stock Marker (Yellow)
                    javafx.scene.shape.Rectangle lowMarker = new javafx.scene.shape.Rectangle(2.5, 8);
                    lowMarker.getStyleClass().add("stock-marker-low");
                    lowMarker.setLayoutX((minThreshold / maxStock) * BAR_WIDTH);
                    
                    // Critical Stock Marker (Red - 50% of threshold)
                    javafx.scene.shape.Rectangle criticalMarker = new javafx.scene.shape.Rectangle(2.5, 8);
                    criticalMarker.getStyleClass().add("stock-marker-critical");
                    criticalMarker.setLayoutX((minThreshold * 0.5 / maxStock) * BAR_WIDTH);

                    markerLayer.getChildren().addAll(lowMarker, criticalMarker);
                }

                container.getChildren().addAll(barTrack, barFill, markerLayer);
                setGraphic(container);
                setText(null);
            }
        });

        colQuantity.setCellValueFactory(cellData -> {
            double qty = cellData.getValue().getQuantity();
            String formatted = (qty == Math.floor(qty) && !Double.isInfinite(qty))
                ? String.format("%.0f", qty)
                : String.format("%.1f", qty);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
        colQuantity.setCellFactory(col -> new TableCell<Ingredient, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-mono"); }
            }
        });

        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colUnit.setCellFactory(col -> new TableCell<Ingredient, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item.toLowerCase()); getStyleClass().add("cell-mono"); }
            }
        });
        
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        colStatus.setCellValueFactory(cellData -> {
            String availabilityStatus = cellData.getValue().getAvailabilityStatus();
            if ("Unavailable".equals(availabilityStatus)) {
                return new javafx.beans.property.SimpleStringProperty("Disabled");
            }
            return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus());
        });
        
        colStatus.setCellFactory(col -> new TableCell<Ingredient, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null); setText(null); return;
                }

                Label pill = new Label(status.toUpperCase());
                pill.getStyleClass().add("status-pill");

                if ("Disabled".equals(status)) {
                    pill.getStyleClass().add("pill-disabled");
                } else if ("OK".equals(status)) {
                    pill.getStyleClass().add("pill-ok");
                } else if ("Low".equals(status)) {
                    pill.getStyleClass().add("pill-low");
                } else {
                    pill.getStyleClass().add("pill-out");
                }

                setGraphic(pill);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<Ingredient, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); setText(null); return; }

                final Ingredient ingredient = getTableView().getItems().get(getIndex());
                if (ingredient == null) { setGraphic(null); return; }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem addStock = new MenuItem("Restock Item");
                addStock.setOnAction(e -> showAddStockDialog(ingredient));

                MenuItem reduceStock = new MenuItem("Reduce Stock");
                reduceStock.setOnAction(e -> showReduceStockDialog(ingredient));

                MenuItem editThreshold = new MenuItem("Threshold Config");
                editThreshold.setOnAction(e -> showEditThresholdDialog(ingredient));

                String availabilityStatus = ingredient.getAvailabilityStatus();
                if ("Unavailable".equals(availabilityStatus)) {
                    MenuItem enable = new MenuItem("Enable Item");
                    enable.setOnAction(e -> {
                        inventoryService.updateAvailabilityStatus(ingredient.getId(), "Available");
                        OrderNotificationService.broadcastUpdate();
                        loadInventory();
                    });
                    menuBtn.getItems().addAll(addStock, reduceStock, editThreshold, enable);
                } else {
                    MenuItem disable = new MenuItem("Disable Item");
                    disable.setOnAction(e -> {
                        inventoryService.updateAvailabilityStatus(ingredient.getId(), "Unavailable");
                        OrderNotificationService.broadcastUpdate();
                        loadInventory();
                    });
                    menuBtn.getItems().addAll(addStock, reduceStock, editThreshold, disable);
                }

                setGraphic(menuBtn);
                setText(null);
            }
        });
    }

    private void checkAllValid(javafx.scene.control.Button okButton, TextField nameField, TextField unitField, TextField quantityField, TextField minField, TextField maxField, String fieldStyle) {
        boolean valid = true;

        String name = nameField.getText().trim();
        if (name.isEmpty() || !name.matches("^[a-zA-Z\\s]+$")) {
            valid = false;
        } else if (inventoryService.existsByName(name)) {
            valid = false;
        }

        String unit = unitField.getText().trim();
        if (unit.isEmpty() || !unit.matches("^[a-zA-Z]+$")) {
            valid = false;
        }

        try {
            Double.parseDouble(quantityField.getText().trim());
        } catch (NumberFormatException ex) {
            valid = false;
        }

        try {
            double min = Double.parseDouble(minField.getText().trim());
            double qty = Double.parseDouble(quantityField.getText().trim());
            if (min > qty) {
                valid = false;
            }
        } catch (NumberFormatException ex) {
            valid = false;
        }

        try {
            double max = Double.parseDouble(maxField.getText().trim());
            double qty = Double.parseDouble(quantityField.getText().trim());
            if (max < qty) {
                valid = false;
            }
        } catch (NumberFormatException ex) {
            valid = false;
        }

        if (valid) {
            okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
            okButton.setDisable(false);
        } else {
            okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
            okButton.setDisable(true);
        }
    }

    private void loadInventory() {
        try {
            int total = inventoryService.getTotalCount();
            int low = inventoryService.getLowStockCount();
            int out = inventoryService.getOutOfStockCount();
            allIngredients = inventoryService.findAll();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblLowStock.setText(String.valueOf(low));
                lblOutOfStock.setText(String.valueOf(out));
                inventoryTable.setItems(FXCollections.observableArrayList(allIngredients));
            });
        } catch (Exception e) {
            System.err.println("Error loading inventory: " + e.getMessage());
        }
    }

    private static class StockUpdateResult {
        final double quantity;
        final String notes;
        StockUpdateResult(double quantity, String notes) {
            this.quantity = quantity;
            this.notes = notes;
        }
    }

    private void showAddStockDialog(Ingredient ingredient) {
        Dialog<StockUpdateResult> dialog = new Dialog<>();
        dialog.setTitle("RESTOCK INGREDIENT");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

        Label headerLabel = new Label("Restock Ingredient");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        VBox content = new VBox(20);
        content.setPadding(new Insets(24, 40, 24, 40));
        content.setAlignment(Pos.TOP_CENTER);
        content.setPrefWidth(420);

        VBox infoBox = new VBox(16);
        infoBox.getStyleClass().add("dialog-section-card");

        VBox nameBox = new VBox(6);
        Label nameEyebrow = new Label("ITEM BEING RESTOCKED");
        nameEyebrow.getStyleClass().add("dialog-eyebrow");
        Label nameLabel = new Label(ingredient.getName().toUpperCase());
        nameLabel.getStyleClass().add("chip-name");
        nameBox.getChildren().addAll(nameEyebrow, nameLabel);

        VBox qtyBox = new VBox(6);
        Label qtyEyebrow = new Label("QUANTITY TO ADD (" + ingredient.getUnit().toLowerCase() + ")");
        qtyEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField quantityField = new TextField();
        quantityField.setPromptText("0.00");
        quantityField.getStyleClass().add("premium-field");
        Label qtyError = new Label("");
        qtyError.getStyleClass().add("dialog-error");
        qtyError.setVisible(false);
        qtyError.setManaged(false);
        qtyBox.getChildren().addAll(qtyEyebrow, quantityField, qtyError);

        VBox notesBox = new VBox(6);
        HBox notesHeader = new HBox();
        Label notesEyebrow = new Label("NOTES");
        notesEyebrow.getStyleClass().add("dialog-eyebrow");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label charCounter = new Label("0/255");
        charCounter.getStyleClass().add("dialog-eyebrow");
        charCounter.setStyle("-fx-text-fill: #5c4828;");
        notesHeader.getChildren().addAll(notesEyebrow, spacer, charCounter);

        TextField notesField = new TextField();
        notesField.setPromptText("e.g. Supplier Delivery");
        notesField.getStyleClass().add("premium-field");
        notesBox.getChildren().addAll(notesHeader, notesField);

        infoBox.getChildren().addAll(nameBox, qtyBox, notesBox);
        content.getChildren().add(infoBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("CONFIRM");
        okButton.getStyleClass().add("dialog-button-save");
        okButton.setDisable(true);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-cancel");

        boolean isPcs = "pcs".equalsIgnoreCase(ingredient.getUnit());
        String numberPattern = isPcs ? "^\\d+$" : "^\\d*\\.?\\d+$";

        notesField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 255) {
                notesField.setText(oldVal);
            }
            charCounter.setText(notesField.getText().length() + "/255");
            if (notesField.getText().length() >= 240) {
                charCounter.setStyle("-fx-text-fill: #ff6b6b;");
            } else {
                charCounter.setStyle("-fx-text-fill: #5c4828;");
            }
        });

        quantityField.textProperty().addListener(obs -> {
            String qtyText = quantityField.getText().trim();
            String errorMsg = null;
            if (qtyText.isEmpty()) {
                okButton.setDisable(true);
            } else if (!qtyText.matches(numberPattern)) {
                errorMsg = isPcs ? "Whole numbers only" : "Invalid number format";
            } else {
                try {
                    double qty = Double.parseDouble(qtyText);
                    if (qty <= 0) {
                        errorMsg = "Must be greater than 0";
                    } else if (ingredient.getQuantity() + qty > ingredient.getMaxStock()) {
                        errorMsg = "Exceeds capacity (" + ingredient.getMaxStock() + ")";
                    } else {
                        okButton.setDisable(false);
                    }
                } catch (Exception ex) { errorMsg = "Invalid number"; }
            }

            if (errorMsg != null) {
                qtyError.setText(errorMsg);
                qtyError.setVisible(true);
                qtyError.setManaged(true);
                okButton.setDisable(true);
            } else {
                qtyError.setVisible(false);
                qtyError.setManaged(false);
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try { 
                    return new StockUpdateResult(Double.parseDouble(quantityField.getText()), notesField.getText().trim()); 
                } catch (Exception ex) { return null; }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != null && result.quantity > 0) {
                int currentStaffId = Main.getCurrentStaff() != null ? Main.getCurrentStaff().getId() : 1;
                inventoryService.logTransaction(ingredient.getId(), result.quantity, currentStaffId, result.notes);
                OrderNotificationService.broadcastUpdate();
                loadInventory();
            }
        });
    }

    private void showReduceStockDialog(Ingredient ingredient) {
        Dialog<StockUpdateResult> dialog = new Dialog<>();
        dialog.setTitle("REDUCE STOCK");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

        Label headerLabel = new Label("Reduce Stock Level");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        VBox content = new VBox(20);
        content.setPadding(new Insets(24, 40, 24, 40));
        content.setAlignment(Pos.TOP_CENTER);
        content.setPrefWidth(420);

        VBox infoBox = new VBox(16);
        infoBox.getStyleClass().add("dialog-section-card");

        VBox nameBox = new VBox(6);
        Label nameEyebrow = new Label("ITEM BEING REDUCED");
        nameEyebrow.getStyleClass().add("dialog-eyebrow");
        Label nameLabel = new Label(ingredient.getName().toUpperCase());
        nameLabel.getStyleClass().add("chip-name");
        nameBox.getChildren().addAll(nameEyebrow, nameLabel);

        VBox qtyBox = new VBox(6);
        Label qtyEyebrow = new Label("QUANTITY TO REMOVE (" + ingredient.getUnit().toLowerCase() + ")");
        qtyEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField quantityField = new TextField();
        quantityField.setPromptText("0.00");
        quantityField.getStyleClass().add("premium-field");
        Label qtyError = new Label("");
        qtyError.getStyleClass().add("dialog-error");
        qtyError.setVisible(false);
        qtyError.setManaged(false);
        qtyBox.getChildren().addAll(qtyEyebrow, quantityField, qtyError);

        VBox notesBox = new VBox(6);
        HBox notesHeader = new HBox();
        Label notesEyebrow = new Label("NOTES");
        notesEyebrow.getStyleClass().add("dialog-eyebrow");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label charCounter = new Label("0/255");
        charCounter.getStyleClass().add("dialog-eyebrow");
        charCounter.setStyle("-fx-text-fill: #5c4828;");
        notesHeader.getChildren().addAll(notesEyebrow, spacer, charCounter);

        TextField notesField = new TextField();
        notesField.setPromptText("e.g. Spillage / Expiration");
        notesField.getStyleClass().add("premium-field");
        notesBox.getChildren().addAll(notesHeader, notesField);

        infoBox.getChildren().addAll(nameBox, qtyBox, notesBox);
        content.getChildren().add(infoBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("CONFIRM");
        okButton.getStyleClass().add("dialog-button-save");
        okButton.setDisable(true);

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-cancel");

        boolean isPcs = "pcs".equalsIgnoreCase(ingredient.getUnit());
        String numberPattern = isPcs ? "^\\d+$" : "^\\d*\\.?\\d+$";

        notesField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 255) {
                notesField.setText(oldVal);
            }
            charCounter.setText(notesField.getText().length() + "/255");
            if (notesField.getText().length() >= 240) {
                charCounter.setStyle("-fx-text-fill: #ff6b6b;");
            } else {
                charCounter.setStyle("-fx-text-fill: #5c4828;");
            }
        });

        quantityField.textProperty().addListener(obs -> {
            String qtyText = quantityField.getText().trim();
            String errorMsg = null;
            if (qtyText.isEmpty()) {
                okButton.setDisable(true);
            } else if (!qtyText.matches(numberPattern)) {
                errorMsg = isPcs ? "Whole numbers only" : "Invalid number format";
            } else {
                try {
                    double qty = Double.parseDouble(qtyText);
                    if (qty <= 0) {
                        errorMsg = "Must be greater than 0";
                    } else if (qty > ingredient.getQuantity()) {
                        errorMsg = "Not enough stock (" + ingredient.getQuantity() + ")";
                    } else {
                        okButton.setDisable(false);
                    }
                } catch (Exception ex) { errorMsg = "Invalid number"; }
            }

            if (errorMsg != null) {
                qtyError.setText(errorMsg);
                qtyError.setVisible(true);
                qtyError.setManaged(true);
                okButton.setDisable(true);
            } else {
                qtyError.setVisible(false);
                qtyError.setManaged(false);
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try { 
                    return new StockUpdateResult(Double.parseDouble(quantityField.getText()), notesField.getText().trim()); 
                } catch (Exception ex) { return null; }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result != null && result.quantity > 0) {
                int currentStaffId = Main.getCurrentStaff() != null ? Main.getCurrentStaff().getId() : 1;
                inventoryService.logTransaction(ingredient.getId(), -result.quantity, currentStaffId, result.notes);
                OrderNotificationService.broadcastUpdate();
                loadInventory();
            }
        });
    }

    private void showEditThresholdDialog(Ingredient ingredient) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("THRESHOLD CONFIG");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

        Label headerLabel = new Label("Threshold Configuration");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        HBox mainLayout = new HBox(32);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        // LEFT COLUMN: Alert Limits
        VBox colLeft = new VBox(28);
        colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
        colLeft.setPrefWidth(300);

        VBox minBox = new VBox(8);
        Label minEyebrow = new Label("LOW STOCK ALERT (MIN)");
        minEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField minField = new TextField(String.valueOf(ingredient.getMinThreshold()));
        minField.getStyleClass().add("premium-field");
        Label minError = new Label("");
        minError.getStyleClass().add("dialog-error");
        minError.setVisible(false);
        minError.setManaged(false);
        minBox.getChildren().addAll(minEyebrow, minField, minError);

        VBox maxBox = new VBox(8);
        Label maxEyebrow = new Label("MAXIMUM CAPACITY (MAX)");
        maxEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField maxField = new TextField(String.valueOf(ingredient.getMaxStock()));
        maxField.getStyleClass().add("premium-field");
        Label maxError = new Label("");
        maxError.getStyleClass().add("dialog-error");
        maxError.setVisible(false);
        maxError.setManaged(false);
        maxBox.getChildren().addAll(maxEyebrow, maxField, maxError);

        colLeft.getChildren().addAll(minBox, maxBox);

        // RIGHT COLUMN: Context Info
        VBox colRight = new VBox(24);
        colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
        colRight.setPrefWidth(260);

        VBox infoBox = new VBox(12);
        Label infoEyebrow = new Label("CURRENT STATUS");
        infoEyebrow.getStyleClass().add("dialog-eyebrow");
        
        Label nameLbl = new Label(ingredient.getName().toUpperCase());
        nameLbl.getStyleClass().add("chip-name");
        
        HBox stockRow = new HBox(8);
        Label stockVal = new Label(String.valueOf(ingredient.getQuantity()));
        stockVal.getStyleClass().add("cell-mono");
        stockVal.setStyle("-fx-font-size: 18px; -fx-text-fill: #c8500a;");
        Label unitVal = new Label(ingredient.getUnit().toLowerCase());
        unitVal.getStyleClass().add("text-muted");
        stockRow.setAlignment(Pos.BASELINE_LEFT);
        stockRow.getChildren().addAll(stockVal, unitVal);

        infoBox.getChildren().addAll(infoEyebrow, nameLbl, stockRow);
        colRight.getChildren().add(infoBox);

        mainLayout.getChildren().addAll(colLeft, colRight);

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("UPDATE CONFIG");
        okButton.getStyleClass().add("dialog-button-save");

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-cancel");

        javafx.beans.InvalidationListener validator = obs -> {
            boolean valid = true;
            try {
                double min = Double.parseDouble(minField.getText().trim());
                if (min < 0) {
                    minError.setText("Cannot be negative");
                    minError.setVisible(true); minError.setManaged(true);
                    valid = false;
                } else { minError.setVisible(false); minError.setManaged(false); }
            } catch (Exception ex) { valid = false; }

            try {
                double max = Double.parseDouble(maxField.getText().trim());
                if (max < ingredient.getQuantity()) {
                    maxError.setText("Below current stock");
                    maxError.setVisible(true); maxError.setManaged(true);
                    valid = false;
                } else { maxError.setVisible(false); maxError.setManaged(false); }
            } catch (Exception ex) { valid = false; }

            okButton.setDisable(!valid);
        };

        minField.textProperty().addListener(validator);
        maxField.textProperty().addListener(validator);

        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK);

        dialog.showAndWait().ifPresent(result -> {
            if (result) {
                try {
                    inventoryService.updateThreshold(ingredient.getId(), Double.parseDouble(minField.getText()));
                    inventoryService.updateMaxStock(ingredient.getId(), Double.parseDouble(maxField.getText()));
                    OrderNotificationService.broadcastUpdate();
                    loadInventory();
                } catch (Exception ex) {}
            }
        });
    }

    private void updateOkButtonState(javafx.scene.control.Button okButton, TextField minField, TextField maxField, String fieldStyle, double currentQty) {
        boolean valid = true;
        String minText = minField.getText().trim();
        String maxText = maxField.getText().trim();

        if (minText.isEmpty() || maxText.isEmpty()) {
            valid = false;
        } else {
            try {
                double min = Double.parseDouble(minText);
                if (min <= 1) valid = false;
            } catch (NumberFormatException ex) {
                valid = false;
            }
            try {
                double max = Double.parseDouble(maxText);
                if (max < currentQty) valid = false;
            } catch (NumberFormatException ex) {
                valid = false;
            }
        }

        if (valid) {
            okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
            okButton.setDisable(false);
        } else {
            okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
            okButton.setDisable(true);
        }
    }

    private void showDisableConfirmation(Ingredient ingredient) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Disable Ingredient");
        alert.setHeaderText("Disable " + ingredient.getName() + "?");
        alert.setContentText("This ingredient will no longer be available for menu items.");
        alert.setGraphic(null);
        alert.getDialogPane().setGraphic(null);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

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

        javafx.scene.control.ButtonType okType = new javafx.scene.control.ButtonType("Disable");
        javafx.scene.control.ButtonType cancelType = new javafx.scene.control.ButtonType("Cancel");
        alert.getDialogPane().getButtonTypes().setAll(okType, cancelType);

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(okType);
        okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        okButton.setOnAction(e -> {
            inventoryService.updateAvailabilityStatus(ingredient.getId(), "Unavailable");
            OrderNotificationService.broadcastUpdate();
            loadInventory();
        });

        javafx.scene.control.Button cancelButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancelType);
        cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        alert.showAndWait();
    }

    private void showEnableConfirmation(Ingredient ingredient) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Enable Ingredient");
        alert.setHeaderText("Enable " + ingredient.getName() + "?");
        alert.setContentText("This ingredient will be available for menu items.");
        alert.setGraphic(null);
        alert.getDialogPane().setGraphic(null);
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

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
                    n.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
                }
            }
        } else {
            headerText.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px;");
        }

        javafx.scene.Node content = alert.getDialogPane().lookup(".content");
        if (content != null) {
            content.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px;");
        }

        javafx.scene.control.ButtonType okType = new javafx.scene.control.ButtonType("Enable");
        javafx.scene.control.ButtonType cancelType = new javafx.scene.control.ButtonType("Cancel");
        alert.getDialogPane().getButtonTypes().setAll(okType, cancelType);

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(okType);
        okButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        okButton.setOnAction(e -> {
            inventoryService.updateAvailabilityStatus(ingredient.getId(), "Available");
            OrderNotificationService.broadcastUpdate();
            loadInventory();
        });

        javafx.scene.control.Button cancelButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancelType);
        cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        alert.showAndWait();
    }

@FXML
private void onAddIngredient() {
    Dialog<Boolean> dialog = new Dialog<>();
    dialog.setTitle("NEW INGREDIENT");
    dialog.getDialogPane().getStyleClass().add("dialog-pane");

    dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
    dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

    Label headerLabel = new Label("Add New Ingredient");
    headerLabel.getStyleClass().add("dialog-header-text");
    dialog.getDialogPane().setHeader(headerLabel);

    HBox mainLayout = new HBox(32);
    mainLayout.setPadding(new Insets(30, 40, 30, 40));
    mainLayout.setAlignment(Pos.TOP_CENTER);

    // LEFT COLUMN: Basic Identity
    VBox colLeft = new VBox(28);
    colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
    colLeft.setPrefWidth(340);

    VBox nameBox = new VBox(8);
    Label nameEyebrow = new Label("INGREDIENT NAME");
    nameEyebrow.getStyleClass().add("dialog-eyebrow");
    TextField nameField = new TextField();
    nameField.setPromptText("e.g. Ground Beef");
    nameField.getStyleClass().add("premium-field");
    Label nameError = new Label("");
    nameError.getStyleClass().add("dialog-error");
    nameError.setVisible(false); nameError.setManaged(false);
    nameBox.getChildren().addAll(nameEyebrow, nameField, nameError);

    VBox unitBox = new VBox(8);
    Label unitEyebrow = new Label("MEASUREMENT UNIT");
    unitEyebrow.getStyleClass().add("dialog-eyebrow");
    TextField unitField = new TextField();
    unitField.setPromptText("e.g. grams, pcs, kg");
    unitField.getStyleClass().add("premium-field");
    Label unitError = new Label("");
    unitError.getStyleClass().add("dialog-error");
    unitError.setVisible(false); unitError.setManaged(false);
    unitBox.getChildren().addAll(unitEyebrow, unitField, unitError);

    colLeft.getChildren().addAll(nameBox, unitBox);

    // RIGHT COLUMN: Stock Configuration
    VBox colRight = new VBox(28);
    colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
    colRight.setPrefWidth(340);

    VBox qtyBox = new VBox(8);
    Label qtyEyebrow = new Label("INITIAL QUANTITY");
    qtyEyebrow.getStyleClass().add("dialog-eyebrow");
    TextField qtyField = new TextField();
    qtyField.setPromptText("0.00");
    qtyField.getStyleClass().add("premium-field");
    Label qtyError = new Label("");
    qtyError.getStyleClass().add("dialog-error");
    qtyError.setVisible(false); qtyError.setManaged(false);
    qtyBox.getChildren().addAll(qtyEyebrow, qtyField, qtyError);

    HBox limitsBox = new HBox(20);
    VBox minBox = new VBox(8);
    Label minEyebrow = new Label("MIN ALERT");
    minEyebrow.getStyleClass().add("dialog-eyebrow");
    TextField minField = new TextField();
    minField.getStyleClass().add("premium-field");
    Label minError = new Label("");
    minError.getStyleClass().add("dialog-error");
    minError.setVisible(false); minError.setManaged(false);
    minBox.getChildren().addAll(minEyebrow, minField, minError);

    VBox maxBox = new VBox(8);
    Label maxEyebrow = new Label("MAX CAPACITY");
    maxEyebrow.getStyleClass().add("dialog-eyebrow");
    TextField maxField = new TextField();
    maxField.getStyleClass().add("premium-field");
    Label maxError = new Label("");
    maxError.getStyleClass().add("dialog-error");
    maxError.setVisible(false); maxError.setManaged(false);
    maxBox.getChildren().addAll(maxEyebrow, maxField, maxError);

    limitsBox.getChildren().addAll(minBox, maxBox);
    HBox.setHgrow(minBox, javafx.scene.layout.Priority.ALWAYS);
    HBox.setHgrow(maxBox, javafx.scene.layout.Priority.ALWAYS);

    colRight.getChildren().addAll(qtyBox, limitsBox);
    mainLayout.getChildren().addAll(colLeft, colRight);

    dialog.getDialogPane().setContent(mainLayout);
    dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);

    Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
    okButton.setText("SAVE INGREDIENT");
    okButton.getStyleClass().add("dialog-button-save");
    okButton.setDisable(true);

    Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
    cancelButton.getStyleClass().add("dialog-button-cancel");

    javafx.beans.InvalidationListener validator = obs -> {
        boolean valid = true;
        String name = nameField.getText().trim();
        if (name.isEmpty()) valid = false;
        else if (inventoryService.existsByName(name)) {
            nameError.setText("Already exists");
            nameError.setVisible(true); nameError.setManaged(true);
            valid = false;
        } else { nameError.setVisible(false); nameError.setManaged(false); }

        if (unitField.getText().trim().isEmpty()) valid = false;

        double qty = 0;
        double min = 0;
        double max = 0;
        boolean qtyParsed = false, minParsed = false, maxParsed = false;

        try { qty = Double.parseDouble(qtyField.getText().trim()); qtyParsed = true; } catch (Exception e) { valid = false; }
        try { min = Double.parseDouble(minField.getText().trim()); minParsed = true; } catch (Exception e) { valid = false; }
        try { max = Double.parseDouble(maxField.getText().trim()); maxParsed = true; } catch (Exception e) { valid = false; }
        
        if (minParsed) {
            if (min < 0) {
                minError.setText("Min Alert cannot be negative");
                minError.setVisible(true); minError.setManaged(true);
                valid = false;
            } else if (maxParsed && min > max) {
                minError.setText("Cannot exceed max capacity");
                minError.setVisible(true); minError.setManaged(true);
                valid = false;
            } else {
                minError.setVisible(false); minError.setManaged(false);
            }
        }

        if (maxParsed) {
            if (qtyParsed && max < qty) {
                maxError.setText("Cannot be lower than initial quantity");
                maxError.setVisible(true); maxError.setManaged(true);
                valid = false;
            } else {
                maxError.setVisible(false); maxError.setManaged(false);
            }
        }

        okButton.setDisable(!valid);
    };

    nameField.textProperty().addListener(validator);
    unitField.textProperty().addListener(validator);
    qtyField.textProperty().addListener(validator);
    minField.textProperty().addListener(validator);
    maxField.textProperty().addListener(validator);

    dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK);

    dialog.showAndWait().ifPresent(result -> {
        if (result) {
            try {
                inventoryService.insert(nameField.getText().trim(), unitField.getText().trim(),
                    Double.parseDouble(qtyField.getText().trim()),
                    Double.parseDouble(minField.getText().trim()),
                    Double.parseDouble(maxField.getText().trim()));
                OrderNotificationService.broadcastUpdate();
                loadInventory();            } catch (Exception ex) {}
        }
    });
}
    @FXML
    private void onRestock() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Restock");
        alert.setHeaderText(null);
        alert.setContentText("Select an ingredient first, then restock.");
        alert.showAndWait();
    }

    @FXML
    private void onLogout() {
        try {
            if (timerService != null) timerService.shutdown();
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

    private void updateTopbarDate() {
        if (topbarDate != null) {
            String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d yyyy"));
            topbarDate.setText(date);
        }
    }
}