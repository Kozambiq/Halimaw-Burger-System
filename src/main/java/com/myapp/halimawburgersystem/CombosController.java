package com.myapp.halimawburgersystem;

import com.myapp.dao.ComboDAO;
import com.myapp.model.Combo;
import com.myapp.service.ComboService;
import com.myapp.util.OrderNotificationService;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.sql.Date;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;

public class CombosController extends BaseController {

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
    @FXML private Label topbarDate;

    private ComboService comboService = new ComboService();
    private boolean alreadyLoaded = false;
    private List<Combo> allCombos;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        updateTopbarDate();
        setActiveNav("Combos & Promos");
        setupTableColumns();
        setupSearchAutocomplete();
        loadCombos();

        // Subscribe to instant updates
        OrderNotificationService.subscribe(this::loadCombos);
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

        List<String> allComboNames = comboService.searchByName("");

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

        List<Combo> results = comboService.findByName(searchText);

        Platform.runLater(() -> {
            if (!results.isEmpty()) {
                combosTable.setItems(FXCollections.observableArrayList(results));
                combosTable.getSelectionModel().select(0);
                combosTable.scrollTo(0);
            }
        });
    }

    @Override
    protected Button[] getNavButtons() {
        return new Button[] {btnDashboard, btnOrders, btnKitchen, btnMenuItems, btnCombos, btnInventory, btnSales, btnStaff};
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
                    comboService.updateStatus(combo.getId(), isActive ? "Disabled" : "Active");
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
            int total = comboService.getTotalCount();
            int active = comboService.getActiveCount();
            allCombos = comboService.findAll();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblActive.setText(String.valueOf(active));
                combosTable.setItems(FXCollections.observableArrayList(allCombos));
            });
        } catch (Exception e) {
            System.err.println("Error loading combos: " + e.getMessage());
        }
    }

    private static class InclusionData {
        String name;
        int qty;
        double unitPrice;
        InclusionData(String n, int q, double p) { name = n; qty = q; unitPrice = p; }
    }

    private HBox createItemChipWithQty(InclusionData data, List<InclusionData> dataList, VBox container, TextField originalPriceInput) {
        HBox chip = new HBox(12);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("ingredient-chip");

        Label name = new Label(data.name.toUpperCase());
        name.getStyleClass().add("chip-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        StackPane qtyPill = new StackPane();
        qtyPill.getStyleClass().add("chip-qty-pill");
        Label qtyText = new Label("x" + data.qty);
        qtyText.getStyleClass().add("chip-qty-text");
        qtyPill.getChildren().add(qtyText);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("btn-chip-remove");
        removeBtn.setOnAction(e -> {
            dataList.remove(data);
            container.getChildren().remove(chip);
            double newTotal = 0;
            for (InclusionData id : dataList) { newTotal += (id.unitPrice * id.qty); }
            originalPriceInput.setText(dataList.isEmpty() ? "" : String.format("%.2f", newTotal));
        });

        chip.getChildren().addAll(name, spacer, qtyPill, removeBtn);
        return chip;
    }

    private void onEditPromo(Combo existingCombo) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("EDIT PROMO");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().getStylesheets().addAll(
            getClass().getResource("/css/common.css").toExternalForm(),
            getClass().getResource("/css/dialog.css").toExternalForm()
        );

        Label headerLabel = new Label("Edit Promo Details");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        HBox mainLayout = new HBox(32);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        VBox colLeft = new VBox(28);
        colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
        colLeft.setPrefWidth(340);

        TextField nameField = new TextField(existingCombo.getName());
        nameField.getStyleClass().add("premium-field");
        colLeft.getChildren().add(createFieldGroup("PROMO NAME", nameField));

        TextField promoPriceField = new TextField(String.format("%.2f", existingCombo.getPromoPrice()));
        promoPriceField.getStyleClass().add("premium-field");
        colLeft.getChildren().add(createFieldGroup("PROMO PRICE (PHP)", promoPriceField));

        DatePicker validUntilPicker = new DatePicker(existingCombo.getValidUntil());
        validUntilPicker.setMaxWidth(Double.MAX_VALUE);
        validUntilPicker.getStyleClass().add("premium-date-picker");
        colLeft.getChildren().add(createFieldGroup("VALID UNTIL", validUntilPicker));

        VBox colRight = new VBox(24);
        colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
        colRight.setPrefWidth(400);

        VBox searchArea = new VBox(12);
        HBox searchInputs = new HBox(10);
        searchInputs.setAlignment(Pos.CENTER_LEFT);

        HBox itemSearchWrapper = new HBox(10);
        itemSearchWrapper.getStyleClass().add("search-bar-group");
        itemSearchWrapper.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(itemSearchWrapper, Priority.ALWAYS);

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.getStyleClass().add("search-icon-svg");
        searchIcon.setScaleX(0.7); searchIcon.setScaleY(0.7);

        TextField itemSearchField = new TextField();
        itemSearchField.getStyleClass().add("premium-field");
        itemSearchField.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 8 0 8 0;");
        itemSearchField.setPromptText("Search menu items...");
        HBox.setHgrow(itemSearchField, Priority.ALWAYS);
        itemSearchWrapper.getChildren().addAll(searchIcon, itemSearchField);

        TextField qtyField = new TextField();
        qtyField.getStyleClass().add("premium-field");
        qtyField.setPrefWidth(60);
        qtyField.setPromptText("Qty");

        Button addBtn = new Button("ADD");
        addBtn.getStyleClass().add("btn-recipe-add");
        searchInputs.getChildren().addAll(itemSearchWrapper, qtyField, addBtn);

        Label inclusionErrorLabel = new Label("Quantity must be a whole number > 0");
        inclusionErrorLabel.setStyle("-fx-text-fill: #e07070; -fx-font-size: 11px; -fx-font-weight: bold;");
        inclusionErrorLabel.setVisible(false);
        inclusionErrorLabel.setManaged(false);

        TextField originalPriceField = new TextField(String.format("%.2f", existingCombo.getOriginalPrice()));
        originalPriceField.setEditable(false);
        originalPriceField.getStyleClass().add("premium-field");
        originalPriceField.setStyle("-fx-opacity: 0.7;");
        VBox originalPriceBox = createFieldGroup("TOTAL ORIGINAL PRICE (AUTO)", originalPriceField);

        searchArea.getChildren().addAll(new Label("COMBO INCLUSIONS") {{ getStyleClass().add("dialog-eyebrow"); }}, searchInputs, inclusionErrorLabel, originalPriceBox);

        javafx.scene.control.ScrollPane itemsScroll = new javafx.scene.control.ScrollPane();
        itemsScroll.setFitToWidth(true);
        itemsScroll.setPrefHeight(180);
        itemsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        VBox selectedItemsContainer = new VBox(10);
        selectedItemsContainer.setPadding(new Insets(4, 0, 0, 0));
        itemsScroll.setContent(selectedItemsContainer);

        colRight.getChildren().addAll(searchArea, itemsScroll);
        mainLayout.getChildren().addAll(colLeft, colRight);

        List<InclusionData> inclusionList = new ArrayList<>();
        String[] parts = existingCombo.getIncludes().split(" \\+ ");
        for (String p : parts) {
            try {
                if (p.contains(" x ")) {
                    String[] sub = p.split(" x ");
                    int qty = Integer.parseInt(sub[0]);
                    String name = sub[1];
                    inclusionList.add(new InclusionData(name, qty, comboService.getMenuItemPrice(name)));
                } else {
                    inclusionList.add(new InclusionData(p, 1, comboService.getMenuItemPrice(p)));
                }
            } catch (Exception ex) {}
        }

        for (InclusionData id : inclusionList) {
            selectedItemsContainer.getChildren().add(createItemChipWithQty(id, inclusionList, selectedItemsContainer, originalPriceField));
        }

        qtyField.textProperty().addListener((obs, old, nv) -> {
            validateInclusion(qtyField, inclusionErrorLabel, addBtn);
        });

        List<String> allMenuItemNames = comboService.searchMenuItems("");
        setupAutocomplete(itemSearchField, itemSearchWrapper, allMenuItemNames);

        addBtn.setOnAction(e -> {
            String item = itemSearchField.getText().trim();
            String qtxt = qtyField.getText().trim();
            if (item.isEmpty() || qtxt.isEmpty()) return;
            try {
                int qty = Integer.parseInt(qtxt);
                double unitPrice = comboService.getMenuItemPrice(item);
                if (unitPrice <= 0) return;
                InclusionData existing = inclusionList.stream().filter(i -> i.name.equalsIgnoreCase(item)).findFirst().orElse(null);
                if (existing != null) { existing.qty += qty; }
                else { inclusionList.add(new InclusionData(item, qty, unitPrice)); }
                
                refreshInclusionsUI(inclusionList, selectedItemsContainer, originalPriceField);
                itemSearchField.clear(); qtyField.clear();
            } catch (Exception ex) {}
        });

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);

        setupDialogButtons(dialog, "UPDATE PROMO");

        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK);

        ((Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK)).setOnAction(e -> {
            String name = nameField.getText().trim();
            String pPrice = promoPriceField.getText().trim();
            LocalDate date = validUntilPicker.getValue();
            if (name.isEmpty() || pPrice.isEmpty() || date == null || inclusionList.isEmpty()) { e.consume(); return; }
            try {
                double promoPrice = Double.parseDouble(pPrice);
                double originalPrice = Double.parseDouble(originalPriceField.getText());
                String inclusionStr = inclusionList.stream().map(id -> id.qty + " x " + id.name).collect(Collectors.joining(" + "));
                comboService.update(existingCombo.getId(), name, inclusionStr, promoPrice, originalPrice, java.sql.Date.valueOf(date));
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
        dialog.getDialogPane().getStylesheets().addAll(
            getClass().getResource("/css/common.css").toExternalForm(),
            getClass().getResource("/css/dialog.css").toExternalForm()
        );

        Label headerLabel = new Label("Create New Promo");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        HBox mainLayout = new HBox(32);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setAlignment(Pos.TOP_CENTER);

        VBox colLeft = new VBox(28);
        colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
        colLeft.setPrefWidth(340);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Halimaw Meal Deal");
        nameField.getStyleClass().add("premium-field");
        colLeft.getChildren().add(createFieldGroup("PROMO NAME", nameField));

        TextField promoPriceField = new TextField();
        promoPriceField.setPromptText("0.00");
        promoPriceField.getStyleClass().add("premium-field");
        colLeft.getChildren().add(createFieldGroup("PROMO PRICE (PHP)", promoPriceField));

        DatePicker validUntilPicker = new DatePicker();
        validUntilPicker.setMaxWidth(Double.MAX_VALUE);
        validUntilPicker.getStyleClass().add("premium-date-picker");
        colLeft.getChildren().add(createFieldGroup("VALID UNTIL", validUntilPicker));

        VBox colRight = new VBox(24);
        colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
        colRight.setPrefWidth(400);

        VBox searchArea = new VBox(12);
        HBox searchInputs = new HBox(10);
        searchInputs.setAlignment(Pos.CENTER_LEFT);

        HBox itemSearchWrapper = new HBox(10);
        itemSearchWrapper.getStyleClass().add("search-bar-group");
        itemSearchWrapper.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(itemSearchWrapper, Priority.ALWAYS);

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.getStyleClass().add("search-icon-svg");
        searchIcon.setScaleX(0.7); searchIcon.setScaleY(0.7);

        TextField itemSearchField = new TextField();
        itemSearchField.getStyleClass().add("premium-field");
        itemSearchField.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 8 0 8 0;");
        itemSearchField.setPromptText("Search menu items...");
        HBox.setHgrow(itemSearchField, Priority.ALWAYS);
        itemSearchWrapper.getChildren().addAll(searchIcon, itemSearchField);

        TextField qtyField = new TextField();
        qtyField.getStyleClass().add("premium-field");
        qtyField.setPrefWidth(60);
        qtyField.setPromptText("Qty");

        Button addBtn = new Button("ADD");
        addBtn.getStyleClass().add("btn-recipe-add");
        searchInputs.getChildren().addAll(itemSearchWrapper, qtyField, addBtn);

        Label inclusionErrorLabel = new Label("Quantity must be a whole number > 0");
        inclusionErrorLabel.setStyle("-fx-text-fill: #e07070; -fx-font-size: 11px; -fx-font-weight: bold;");
        inclusionErrorLabel.setVisible(false);
        inclusionErrorLabel.setManaged(false);

        TextField originalPriceField = new TextField();
        originalPriceField.setEditable(false);
        originalPriceField.getStyleClass().add("premium-field");
        originalPriceField.setStyle("-fx-opacity: 0.7;");
        VBox originalPriceBox = createFieldGroup("TOTAL ORIGINAL PRICE (AUTO)", originalPriceField);

        searchArea.getChildren().addAll(new Label("COMBO INCLUSIONS") {{ getStyleClass().add("dialog-eyebrow"); }}, searchInputs, inclusionErrorLabel, originalPriceBox);

        javafx.scene.control.ScrollPane itemsScroll = new javafx.scene.control.ScrollPane();
        itemsScroll.setFitToWidth(true);
        itemsScroll.setPrefHeight(180);
        itemsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        VBox selectedItemsContainer = new VBox(10);
        selectedItemsContainer.setPadding(new Insets(4, 0, 0, 0));
        itemsScroll.setContent(selectedItemsContainer);

        colRight.getChildren().addAll(searchArea, itemsScroll);
        mainLayout.getChildren().addAll(colLeft, colRight);

        List<InclusionData> inclusionList = new ArrayList<>();
        qtyField.textProperty().addListener((obs, old, nv) -> validateInclusion(qtyField, inclusionErrorLabel, addBtn));

        List<String> allMenuItemNames = comboService.searchMenuItems("");
        setupAutocomplete(itemSearchField, itemSearchWrapper, allMenuItemNames);

        addBtn.setOnAction(e -> {
            String item = itemSearchField.getText().trim();
            String qtxt = qtyField.getText().trim();
            if (item.isEmpty() || qtxt.isEmpty()) return;
            try {
                int qty = Integer.parseInt(qtxt);
                double unitPrice = comboService.getMenuItemPrice(item);
                if (unitPrice <= 0) return;
                InclusionData existing = inclusionList.stream().filter(i -> i.name.equalsIgnoreCase(item)).findFirst().orElse(null);
                if (existing != null) { existing.qty += qty; }
                else { inclusionList.add(new InclusionData(item, qty, unitPrice)); }
                
                refreshInclusionsUI(inclusionList, selectedItemsContainer, originalPriceField);
                itemSearchField.clear(); qtyField.clear();
            } catch (Exception ex) {}
        });

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);

        setupDialogButtons(dialog, "SAVE PROMO");

        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK);

        ((Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK)).setOnAction(e -> {
            String name = nameField.getText().trim();
            String pPrice = promoPriceField.getText().trim();
            LocalDate date = validUntilPicker.getValue();
            if (name.isEmpty() || pPrice.isEmpty() || date == null || inclusionList.isEmpty()) { e.consume(); return; }
            try {
                double promoPrice = Double.parseDouble(pPrice);
                double originalPrice = Double.parseDouble(originalPriceField.getText());
                String inclusionStr = inclusionList.stream().map(id -> id.qty + " x " + id.name).collect(Collectors.joining(" + "));
                comboService.insert(name, inclusionStr, promoPrice, originalPrice, java.sql.Date.valueOf(date));
                loadCombos();
                dialog.close();
            } catch (Exception ex) { e.consume(); }
        });

        dialog.showAndWait();
    }

    private VBox createFieldGroup(String label, javafx.scene.Node field) {
        VBox box = new VBox(8);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("dialog-eyebrow");
        box.getChildren().addAll(lbl, field);
        return box;
    }

    private void validateInclusion(TextField qtyField, Label errorLabel, Button addBtn) {
        String qtxt = qtyField.getText().trim();
        if (qtxt.isEmpty()) {
            errorLabel.setVisible(false); errorLabel.setManaged(false); addBtn.setDisable(false); return;
        }
        try {
            double val = Double.parseDouble(qtxt);
            boolean isWhole = val == Math.floor(val);
            if (val <= 0 || !isWhole) {
                errorLabel.setVisible(true); errorLabel.setManaged(true); addBtn.setDisable(true);
            } else {
                errorLabel.setVisible(false); errorLabel.setManaged(false); addBtn.setDisable(false);
            }
        } catch (Exception ex) { addBtn.setDisable(true); }
    }

    private void setupAutocomplete(TextField field, HBox wrapper, List<String> names) {
        javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.getStyleClass().add("suggestion-list");
        suggestionList.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        suggestionList.setFixedCellSize(40);
        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionList);

        field.textProperty().addListener(obs -> {
            String q = field.getText().trim().toLowerCase();
            if (q.isEmpty()) { suggestionPopup.hide(); return; }
            List<String> matches = names.stream().filter(n -> n.toLowerCase().contains(q)).collect(Collectors.toList());
            if (!matches.isEmpty()) {
                suggestionList.setItems(FXCollections.observableArrayList(matches));
                suggestionList.setPrefHeight(Math.min(matches.size(), 5) * 40 + 8);
                suggestionList.setPrefWidth(wrapper.getWidth());
                javafx.geometry.Bounds bounds = wrapper.localToScreen(wrapper.getBoundsInLocal());
                suggestionPopup.show(wrapper, bounds.getMinX(), bounds.getMaxY() + 4);
            } else { suggestionPopup.hide(); }
        });

        suggestionList.setOnMouseClicked(e -> {
            if (!suggestionList.getSelectionModel().isEmpty()) {
                field.setText(suggestionList.getSelectionModel().getSelectedItem());
                suggestionPopup.hide();
            }
        });
    }

    private void refreshInclusionsUI(List<InclusionData> list, VBox container, TextField originalPriceField) {
        container.getChildren().clear();
        double total = 0;
        for (InclusionData id : list) {
            container.getChildren().add(createItemChipWithQty(id, list, container, originalPriceField));
            total += (id.unitPrice * id.qty);
        }
        originalPriceField.setText(String.format("%.2f", total));
    }

    private void setupDialogButtons(Dialog<Boolean> dialog, String saveText) {
        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText(saveText);
        okButton.getStyleClass().add("dialog-button-save");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-cancel");
    }

    private void showDeleteConfirmation(Combo combo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Combo");
        alert.setHeaderText("Delete " + combo.getName() + "?");
        alert.setContentText("This action cannot be undone.");
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        alert.initOwner(combosTable.getScene().getWindow());

        javafx.scene.control.ButtonType okType = new javafx.scene.control.ButtonType("Delete");
        javafx.scene.control.ButtonType cancelType = new javafx.scene.control.ButtonType("Cancel");
        alert.getDialogPane().getButtonTypes().setAll(okType, cancelType);

        ((Button) alert.getDialogPane().lookupButton(okType)).setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16;");
        ((Button) alert.getDialogPane().lookupButton(cancelType)).setStyle("-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6;");

        alert.showAndWait().ifPresent(btn -> {
            if (btn == okType) {
                comboService.delete(combo.getId());
                loadCombos();
            }
        });
    }

    @FXML
    private void onLogout() {
        try { Main.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onNavigateDashboard() { try { Main.showDashboard(); } catch (Exception e) {} }
    @FXML private void onNavigateOrders() { try { Main.showOrders(); } catch (Exception e) {} }
    @FXML private void onNavigateKitchen() { try { Main.showKitchen(); } catch (Exception e) {} }
    @FXML private void onNavigateMenuItems() { try { Main.showMenuItems(); } catch (Exception e) {} }
    @FXML private void onNavigateCombos() { try { Main.showCombos(); } catch (Exception e) {} }
    @FXML private void onNavigateInventory() { try { Main.showInventory(); } catch (Exception e) {} }
    @FXML private void onNavigateSales() { try { Main.showSalesReport(); } catch (Exception e) {} }
    @FXML private void onNavigateStaff() { try { Main.showStaff(); } catch (Exception e) {} }

    private void updateTopbarDate() {
        if (topbarDate != null) {
            String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d yyyy"));
            topbarDate.setText(date);
        }
    }
}