package com.myapp.halimawburgersystem;

import com.myapp.dao.IngredientDAO;
import com.myapp.model.Ingredient;
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
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.util.Callback;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.stream.Collectors;

public class InventoryController {

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

    @FXML private TextField searchField;
    @FXML private Button btnSearch;

    private IngredientDAO ingredientDAO = new IngredientDAO();
    private boolean alreadyLoaded = false;
    private List<Ingredient> allIngredients;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        setActiveNav("Inventory");
        setupTableColumns();
        setupSearchAutocomplete();
        loadInventory();
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

        List<String> allIngredientNames = ingredientDAO.searchByName("");

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

        List<Ingredient> results = ingredientDAO.findByName(searchText);

        Platform.runLater(() -> {
            if (!results.isEmpty()) {
                inventoryTable.setItems(FXCollections.observableArrayList(results));
                inventoryTable.getSelectionModel().select(0);
                inventoryTable.scrollTo(0);
            }
        });
    }

    private void setActiveNav(String navName) {
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
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Ingredient ing = getTableView().getItems().get(getIndex());
                String status = ing.getStatus();
                final double BAR_WIDTH = 180.0;
                double maxStock = ing.getMaxStock();
                double minThreshold = ing.getMinThreshold();

                Pane container = new Pane();
                container.setPrefHeight(10);
                container.setPrefWidth(BAR_WIDTH);

                Pane barTrack = new Pane();
                barTrack.setPrefHeight(6);
                barTrack.setPrefWidth(BAR_WIDTH);
                barTrack.setLayoutY(2);
                barTrack.setBackground(new Background(
                    new BackgroundFill(Color.web("#2d2010"), new CornerRadii(3.0), Insets.EMPTY)
                ));

                Pane barFill = new Pane();
                barFill.setPrefHeight(6);
                barFill.setLayoutY(2);
                double fillWidth = Math.min(percentage * (BAR_WIDTH / 100.0), BAR_WIDTH);
                barFill.setPrefWidth(fillWidth);

                Color barColor;
                if ("OK".equals(status)) {
                    barColor = Color.web("#4CAF50");
                } else if ("Low".equals(status)) {
                    barColor = Color.web("#FFA726");
                } else {
                    barColor = Color.web("#F44336");
                }
                barFill.setBackground(new Background(
                    new BackgroundFill(barColor, new CornerRadii(3.0), Insets.EMPTY)
                ));

                javafx.scene.shape.Rectangle lowMarker = new javafx.scene.shape.Rectangle(2, 10);
                lowMarker.setFill(Color.web("#EF9F27"));

                javafx.scene.shape.Rectangle criticalMarker = new javafx.scene.shape.Rectangle(2, 10);
                criticalMarker.setFill(Color.web("#E24B4A"));

                if (maxStock > 0) {
                    lowMarker.setLayoutX((minThreshold / maxStock) * BAR_WIDTH);
                    criticalMarker.setLayoutX((minThreshold * 0.5 / maxStock) * BAR_WIDTH);
                } else {
                    lowMarker.setLayoutX(0);
                    criticalMarker.setLayoutX(0);
                }
                lowMarker.setLayoutY(0);
                criticalMarker.setLayoutY(0);

                container.getChildren().addAll(barTrack, barFill, criticalMarker, lowMarker);
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
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        colStatus.setCellValueFactory(cellData -> {
            String availabilityStatus = cellData.getValue().getAvailabilityStatus();
            if ("Unavailable".equals(availabilityStatus)) {
                return new javafx.beans.property.SimpleStringProperty("Unavailable");
            }
            return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus());
        });
        
        colStatus.setCellFactory(col -> new TableCell<Ingredient, String>() {
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

                if ("Unavailable".equals(status)) {
                    pill.getStyleClass().add("pill-out");
                } else if ("OK".equals(status)) {
                    pill.getStyleClass().add("pill-ok");
                } else if ("Low".equals(status)) {
                    pill.getStyleClass().add("pill-low");
                } else if ("Out".equals(status)) {
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

                final Ingredient ingredient = getTableView().getItems().get(rowIndex);
                if (ingredient == null) {
                    setGraphic(null);
                    return;
                }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem addStock = new MenuItem("Add Stock");
                addStock.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                addStock.setOnAction(e -> showAddStockDialog(ingredient));

                MenuItem reduceStock = new MenuItem("Reduce Stock");
                reduceStock.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                reduceStock.setOnAction(e -> showReduceStockDialog(ingredient));

                MenuItem editThreshold = new MenuItem("Edit Threshold");
                editThreshold.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                editThreshold.setOnAction(e -> showEditThresholdDialog(ingredient));

                String availabilityStatus = ingredient.getAvailabilityStatus();
                if ("Unavailable".equals(availabilityStatus)) {
                    MenuItem enable = new MenuItem("Enable");
                    enable.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    enable.setOnAction(e -> showEnableConfirmation(ingredient));
                    menuBtn.getItems().addAll(addStock, reduceStock, editThreshold, enable);
                } else {
                    MenuItem disable = new MenuItem("Disable");
                    disable.setStyle("-fx-text-fill: #e07070; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                    disable.setOnAction(e -> showDisableConfirmation(ingredient));
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
        } else if (ingredientDAO.existsByName(name)) {
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
            int total = ingredientDAO.getTotalCount();
            int low = ingredientDAO.getLowStockCount();
            int out = ingredientDAO.getOutOfStockCount();
            allIngredients = ingredientDAO.findAll();

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

    private void showAddStockDialog(Ingredient ingredient) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Add Stock");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        javafx.scene.layout.VBox addContent = new javafx.scene.layout.VBox(8);
        javafx.scene.control.Label addTitle = new javafx.scene.control.Label("Add stock to: " + ingredient.getName());
        addTitle.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 14px; -fx-font-weight: bold;");
        addContent.getChildren().add(addTitle);

        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";

        TextField quantityField = new TextField();
        quantityField.setStyle(fieldStyle);
        quantityField.setPromptText("Enter quantity");

        javafx.scene.control.Label qtyError = new javafx.scene.control.Label();
        qtyError.setStyle("-fx-text-fill: #e07070; -fx-font-size: 11px;");
        qtyError.setVisible(false);

        addContent.getChildren().addAll(quantityField, qtyError);
        dialog.getDialogPane().setContent(addContent);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        okButton.setDisable(true);

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: #4a3820; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        boolean isPcs = "pcs".equalsIgnoreCase(ingredient.getUnit());
        double maxStock = ingredient.getMaxStock();

        String numberPattern = isPcs ? "^\\d+$" : "^\\d*\\.?\\d+$";

        javafx.beans.InvalidationListener qtyValidator = obs -> {
            String qtyText = quantityField.getText().trim();
            boolean valid = true;
            String errorMsg = null;

            if (qtyText.isEmpty()) {
                valid = false;
            } else if (!qtyText.matches(numberPattern)) {
                if (isPcs) {
                    errorMsg = "Whole numbers only";
                } else {
                    errorMsg = "Enter valid number";
                }
                valid = false;
            } else {
                try {
                    double qty = Double.parseDouble(qtyText);
                    if (ingredient.getQuantity() + qty > maxStock) {
                        errorMsg = "Max stock limit would be exceeded";
                        valid = false;
                    }
                } catch (NumberFormatException ex) {
                    errorMsg = "Enter valid number";
                    valid = false;
                }
            }

            if (errorMsg != null) {
                qtyError.setText(errorMsg);
                qtyError.setVisible(true);
                quantityField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else {
                qtyError.setVisible(false);
                quantityField.setStyle(fieldStyle);
            }

            if (valid && !qtyText.isEmpty()) {
                okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
                okButton.setDisable(false);
            } else {
                okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
                okButton.setDisable(true);
            }
        };

        quantityField.textProperty().addListener(qtyValidator);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    return Double.parseDouble(quantityField.getText());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(qty -> {
            if (qty != null && qty > 0) {
                double newQty = ingredient.getQuantity() + qty;
                ingredientDAO.updateQuantity(ingredient.getId(), newQty);
                loadInventory();
            }
        });
    }

    private void showReduceStockDialog(Ingredient ingredient) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Reduce Stock");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        javafx.scene.layout.VBox reduceContent = new javafx.scene.layout.VBox(8);
        javafx.scene.control.Label reduceTitle = new javafx.scene.control.Label("Reduce stock from: " + ingredient.getName());
        reduceTitle.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 14px; -fx-font-weight: bold;");
        reduceContent.getChildren().add(reduceTitle);

        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";

        TextField quantityField = new TextField();
        quantityField.setStyle(fieldStyle);
        quantityField.setPromptText("Enter quantity to reduce");

        javafx.scene.control.Label qtyError = new javafx.scene.control.Label();
        qtyError.setStyle("-fx-text-fill: #e07070; -fx-font-size: 11px;");
        qtyError.setVisible(false);

        reduceContent.getChildren().addAll(quantityField, qtyError);
        dialog.getDialogPane().setContent(reduceContent);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        okButton.setDisable(true);

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        boolean isPcs = "pcs".equalsIgnoreCase(ingredient.getUnit());
        double currentQty = ingredient.getQuantity();

        String numberPattern = isPcs ? "^\\d+$" : "^\\d*\\.?\\d+$";

        javafx.beans.InvalidationListener qtyValidator = obs -> {
            String qtyText = quantityField.getText().trim();
            boolean valid = true;
            String errorMsg = null;

            if (qtyText.isEmpty()) {
                valid = false;
            } else if (!qtyText.matches(numberPattern)) {
                if (isPcs) {
                    errorMsg = "Whole numbers only";
                } else {
                    errorMsg = "Enter valid number";
                }
                valid = false;
            } else {
                try {
                    double qty = Double.parseDouble(qtyText);
                    if (qty > currentQty) {
                        errorMsg = "Not enough stock";
                        valid = false;
                    }
                } catch (NumberFormatException ex) {
                    errorMsg = "Enter valid number";
                    valid = false;
                }
            }

            if (errorMsg != null) {
                qtyError.setText(errorMsg);
                qtyError.setVisible(true);
                quantityField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else {
                qtyError.setVisible(false);
                quantityField.setStyle(fieldStyle);
            }

            if (valid && !qtyText.isEmpty()) {
                okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
                okButton.setDisable(false);
            } else {
                okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
                okButton.setDisable(true);
            }
        };

        quantityField.textProperty().addListener(qtyValidator);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    return Double.parseDouble(quantityField.getText());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(qty -> {
            if (qty != null && qty > 0 && qty <= ingredient.getQuantity()) {
                double newQty = ingredient.getQuantity() - qty;
                ingredientDAO.updateQuantity(ingredient.getId(), newQty);
                loadInventory();
            }
        });
    }

    private void showEditThresholdDialog(Ingredient ingredient) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Edit Threshold");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        String labelStyle = "-fx-text-fill: #a09070; -fx-font-size: 12px;";
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(8);
        javafx.scene.control.Label minLabel = new javafx.scene.control.Label("Min Threshold:");
        minLabel.setStyle(labelStyle);
        TextField minField = new TextField(String.valueOf(ingredient.getMinThreshold()));
        minField.setStyle(fieldStyle);
        javafx.scene.control.Label minError = new javafx.scene.control.Label();
        minError.setStyle("-fx-text-fill: #e07070; -fx-font-size: 11px;");
        minError.setVisible(false);

        javafx.scene.control.Label maxLabel = new javafx.scene.control.Label("Max Stock:");
        maxLabel.setStyle(labelStyle);
        TextField maxField = new TextField(String.valueOf(ingredient.getMaxStock()));
        maxField.setStyle(fieldStyle);
        javafx.scene.control.Label maxError = new javafx.scene.control.Label();
        maxError.setStyle("-fx-text-fill: #e07070; -fx-font-size: 11px;");
        maxError.setVisible(false);

        vbox.getChildren().addAll(minLabel, minField, minError, maxLabel, maxField, maxError);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        okButton.setDisable(true);

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        double currentQty = ingredient.getQuantity();

        javafx.beans.InvalidationListener minValidator = obs -> {
            String minText = minField.getText().trim();
            boolean valid = true;
            String errorMsg = null;

            if (minText.isEmpty()) {
                valid = false;
            } else {
                try {
                    double min = Double.parseDouble(minText);
                    if (min < 0) {
                        errorMsg = "Must be at least 1";
                        valid = false;
                    }
                } catch (NumberFormatException ex) {
                    errorMsg = "Enter valid number";
                    valid = false;
                }
            }

            if (errorMsg != null) {
                minError.setText(errorMsg);
                minError.setVisible(true);
                minField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else {
                minError.setVisible(false);
                minField.setStyle(fieldStyle);
            }

            updateOkButtonState(okButton, minField, maxField, fieldStyle, currentQty);
        };

        javafx.beans.InvalidationListener maxValidator = obs -> {
            String maxText = maxField.getText().trim();
            boolean valid = true;
            String errorMsg = null;

            if (maxText.isEmpty()) {
                valid = false;
            } else {
                try {
                    double max = Double.parseDouble(maxText);
                    if (max < currentQty) {
                        errorMsg = "Must be higher than current stock";
                        valid = false;
                    }
                } catch (NumberFormatException ex) {
                    errorMsg = "Enter valid number";
                    valid = false;
                }
            }

            if (errorMsg != null) {
                maxError.setText(errorMsg);
                maxError.setVisible(true);
                maxField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else {
                maxError.setVisible(false);
                maxField.setStyle(fieldStyle);
            }

            updateOkButtonState(okButton, minField, maxField, fieldStyle, currentQty);
        };

        minField.textProperty().addListener(minValidator);
        maxField.textProperty().addListener(maxValidator);

        updateOkButtonState(okButton, minField, maxField, fieldStyle, currentQty);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                try {
                    return Double.parseDouble(minField.getText());
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                double newMin = Double.parseDouble(minField.getText());
                double newMax = Double.parseDouble(maxField.getText());
                if (newMin > 0 && newMax > 0) {
                    ingredientDAO.updateThreshold(ingredient.getId(), newMin);
                    ingredientDAO.updateMaxStock(ingredient.getId(), newMax);
                    loadInventory();
                }
            } catch (NumberFormatException ex) {
                System.err.println("Invalid input: " + ex.getMessage());
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
            ingredientDAO.updateAvailabilityStatus(ingredient.getId(), "Unavailable");
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
            ingredientDAO.updateAvailabilityStatus(ingredient.getId(), "Available");
            loadInventory();
        });

        javafx.scene.control.Button cancelButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancelType);
        cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        alert.showAndWait();
    }

@FXML
    private void onAddIngredient() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Add New Ingredient");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        String labelStyle = "-fx-text-fill: #a09070; -fx-font-size: 12px;";
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";
        String errorStyle = "-fx-text-fill: #e07070; -fx-font-size: 11px;";

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        TextField nameField = new TextField();
        nameField.setStyle(fieldStyle);
        nameField.setPromptText("Ingredient name");

        TextField unitField = new TextField();
        unitField.setStyle(fieldStyle);
        unitField.setPromptText("e.g., pcs, g, L");

        TextField quantityField = new TextField();
        quantityField.setStyle(fieldStyle);
        quantityField.setPromptText("Initial quantity");

        TextField minThresholdField = new TextField();
        minThresholdField.setStyle(fieldStyle);
        minThresholdField.setPromptText("Low stock alert threshold");

        TextField maxStockField = new TextField();
        maxStockField.setStyle(fieldStyle);
        maxStockField.setPromptText("Maximum stock capacity");

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle(labelStyle);
        Label unitLabel = new Label("Unit:");
        unitLabel.setStyle(labelStyle);
        Label qtyLabel = new Label("Quantity:");
        qtyLabel.setStyle(labelStyle);
        Label minLabel = new Label("Min Threshold:");
        minLabel.setStyle(labelStyle);
        Label maxLabel = new Label("Max Stock:");
        maxLabel.setStyle(labelStyle);

        Label nameError = new Label("");
        nameError.setStyle(errorStyle);
        nameError.setVisible(false);
        Label unitError = new Label("");
        unitError.setStyle(errorStyle);
        unitError.setVisible(false);
        Label qtyError = new Label("");
        qtyError.setStyle(errorStyle);
        qtyError.setVisible(false);
        Label minError = new Label("");
        minError.setStyle(errorStyle);
        minError.setVisible(false);
        Label maxError = new Label("");
        maxError.setStyle(errorStyle);
        maxError.setVisible(false);

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(nameError, 1, 1);
        grid.add(unitLabel, 0, 2);
        grid.add(unitField, 1, 2);
        grid.add(unitError, 1, 3);
        grid.add(qtyLabel, 0, 4);
        grid.add(quantityField, 1, 4);
        grid.add(qtyError, 1, 5);
        grid.add(minLabel, 0, 6);
        grid.add(minThresholdField, 1, 6);
        grid.add(minError, 1, 7);
        grid.add(maxLabel, 0, 8);
        grid.add(maxStockField, 1, 8);
        grid.add(maxError, 1, 9);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
        okButton.setDisable(true);

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        javafx.beans.InvalidationListener nameValidator = obs -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty() && !name.matches("^[a-zA-Z\\s]+$")) {
                nameError.setText("Only letters and spaces allowed");
                nameError.setVisible(true);
                nameField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else if (name.isEmpty()) {
                nameError.setText("Required");
                nameError.setVisible(true);
                nameField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else if (ingredientDAO.existsByName(name)) {
                nameError.setText("Already exists");
                nameError.setVisible(true);
                nameField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else {
                nameError.setVisible(false);
                nameField.setStyle(fieldStyle);
            }
            checkAllValid(okButton, nameField, unitField, quantityField, minThresholdField, maxStockField, fieldStyle);
        };

        javafx.beans.InvalidationListener unitValidator = obs -> {
            String unit = unitField.getText().trim();
            if (!unit.isEmpty() && !unit.matches("^[a-zA-Z]+$")) {
                unitError.setText("Only letters allowed");
                unitError.setVisible(true);
                unitField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else if (unit.isEmpty()) {
                unitError.setText("Required");
                unitError.setVisible(true);
                unitField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            } else {
                unitError.setVisible(false);
                unitField.setStyle(fieldStyle);
            }
            checkAllValid(okButton, nameField, unitField, quantityField, minThresholdField, maxStockField, fieldStyle);
        };

        javafx.beans.InvalidationListener qtyValidator = obs -> {
            try {
                double qty = Double.parseDouble(quantityField.getText().trim());
                qtyError.setVisible(false);
                quantityField.setStyle(fieldStyle);
            } catch (NumberFormatException ex) {
                qtyError.setText("Enter valid number");
                qtyError.setVisible(true);
                quantityField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            }
            checkAllValid(okButton, nameField, unitField, quantityField, minThresholdField, maxStockField, fieldStyle);
        };

        javafx.beans.InvalidationListener minValidator = obs -> {
            try {
                double min = Double.parseDouble(minThresholdField.getText().trim());
                double qty = Double.parseDouble(quantityField.getText().trim());
                if (min > qty) {
                    minError.setText("Must be lower than Quantity");
                    minError.setVisible(true);
                    minThresholdField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                } else {
                    minError.setVisible(false);
                    minThresholdField.setStyle(fieldStyle);
                }
            } catch (NumberFormatException ex) {
                minError.setText("Enter valid number");
                minError.setVisible(true);
                minThresholdField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            }
            checkAllValid(okButton, nameField, unitField, quantityField, minThresholdField, maxStockField, fieldStyle);
        };

        javafx.beans.InvalidationListener maxValidator = obs -> {
            try {
                double max = Double.parseDouble(maxStockField.getText().trim());
                double qty = Double.parseDouble(quantityField.getText().trim());
                if (max < qty) {
                    maxError.setText("Must be higher than Quantity");
                    maxError.setVisible(true);
                    maxStockField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                } else {
                    maxError.setVisible(false);
                    maxStockField.setStyle(fieldStyle);
                }
            } catch (NumberFormatException ex) {
                maxError.setText("Enter valid number");
                maxError.setVisible(true);
                maxStockField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
            }
            checkAllValid(okButton, nameField, unitField, quantityField, minThresholdField, maxStockField, fieldStyle);
        };

        nameField.textProperty().addListener(nameValidator);
        unitField.textProperty().addListener(unitValidator);
        quantityField.textProperty().addListener(qtyValidator);
        minThresholdField.textProperty().addListener(minValidator);
        maxStockField.textProperty().addListener(maxValidator);

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return true;
            }
            return false;
        });

        dialog.showAndWait().filter(result -> result).ifPresent(result -> {
            try {
                String name = nameField.getText().trim();
                String unit = unitField.getText().trim();
                double quantity = Double.parseDouble(quantityField.getText().trim());
                double minThreshold = Double.parseDouble(minThresholdField.getText().trim());
                double maxStock = Double.parseDouble(maxStockField.getText().trim());

                if (!name.isEmpty() && !unit.isEmpty()) {
                    ingredientDAO.insert(name, unit, quantity, minThreshold, maxStock);
                    loadInventory();
                }
            } catch (NumberFormatException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("Invalid input");
                err.setContentText("Please enter valid numbers for quantity, threshold, and max stock.");
                err.showAndWait();
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Sales Reports");
        alert.setHeaderText(null);
        alert.setContentText("Coming soon!");
        alert.showAndWait();
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