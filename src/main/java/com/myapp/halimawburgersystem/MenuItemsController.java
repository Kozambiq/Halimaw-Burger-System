package com.myapp.halimawburgersystem;

import com.myapp.dao.MenuItemDAO;
import com.myapp.dao.MenuItemDAO.MenuItemIngredient;
import com.myapp.model.Ingredient;
import com.myapp.model.MenuItemModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MenuItemsController {

    @FXML private Label lblTotal;
    @FXML private Label lblAvailable;
    @FXML private Label lblLowStock;
    @FXML private Label lblOutOfStock;
    @FXML private TableView<MenuItemModel> menuItemsTable;
    @FXML private TableColumn<MenuItemModel, String> colName;
    @FXML private TableColumn<MenuItemModel, String> colCategory;
    @FXML private TableColumn<MenuItemModel, String> colPrice;
    @FXML private TableColumn<MenuItemModel, String> colAvailability;
    @FXML private TableColumn<MenuItemModel, String> colActions;

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
    @FXML private Button btnAddItem;

    // Category Sidebar Buttons
    @FXML private Button catAll;
    @FXML private Button catBurgers;
    @FXML private Button catSides;
    @FXML private Button catDrinks;
    @FXML private Button catOthers;

    private MenuItemDAO menuItemDAO = new MenuItemDAO();
    private boolean alreadyLoaded = false;
    private List<MenuItemModel> allMenuItems;
    private String selectedCategory = "All Items";

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        setActiveNav("Menu Items");
        setupTableColumns();
        setupSearchAutocomplete();
        loadMenuItems();
    }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase();
        
        if (allMenuItems == null) return;
        
        List<MenuItemModel> filtered = allMenuItems.stream()
            .filter(item -> {
                boolean matchesSearch = query.isEmpty() || item.getName().toLowerCase().contains(query);
                boolean matchesCat = selectedCategory.equals("All Items") || item.getCategory().equalsIgnoreCase(selectedCategory);
                return matchesSearch && matchesCat;
            })
            .collect(Collectors.toList());
            
        menuItemsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private HBox searchBarContainer;

    @FXML
    private void onCategorySelect(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        selectedCategory = source.getText().trim();
        
        // Update button highlights
        catAll.getStyleClass().remove("cat-nav-item-active");
        catBurgers.getStyleClass().remove("cat-nav-item-active");
        catSides.getStyleClass().remove("cat-nav-item-active");
        catDrinks.getStyleClass().remove("cat-nav-item-active");
        catOthers.getStyleClass().remove("cat-nav-item-active");
        
        source.getStyleClass().add("cat-nav-item-active");
        
        applyFilters();
    }

    private void setupSearchAutocomplete() {
        javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.getStyleClass().add("suggestion-list");
        suggestionList.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        suggestionList.setFixedCellSize(40);
        suggestionList.setMaxHeight(240);
        suggestionList.setPrefWidth(280);

        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionList);

        List<String> allMenuItemNames = menuItemDAO.searchByName("");

        searchField.textProperty().addListener(obs -> {
            String query = searchField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                suggestionPopup.hide();
                applyFilters();
                return;
            }

            List<String> matches = allMenuItemNames.stream()
                    .filter(name -> name.toLowerCase().contains(query))
                    .collect(Collectors.toList());

            if (!matches.isEmpty()) {
                int rowCount = Math.min(matches.size(), 6);
                suggestionList.setItems(FXCollections.observableArrayList(matches));
                suggestionList.setPrefHeight(rowCount * 40 + 8);
                suggestionList.setPrefWidth(searchBarContainer.getWidth());
                javafx.geometry.Bounds bounds = searchBarContainer.localToScreen(searchBarContainer.getBoundsInLocal());
                suggestionPopup.show(searchBarContainer, bounds.getMinX(), bounds.getMaxY() + 4);
            } else {
                suggestionPopup.hide();
            }
            applyFilters();
        });

        suggestionList.setOnMouseClicked(e -> {
            if (!suggestionList.getSelectionModel().isEmpty()) {
                String selected = suggestionList.getSelectionModel().getSelectedItem();
                searchField.setText(selected);
                suggestionPopup.hide();
                applyFilters();
            }
        });

        searchField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                suggestionPopup.hide();
            }
        });
    }

    @FXML
    private void onCloseDetails() {
    }

    @FXML
    private void onSearchMenuItem() {
        applyFilters();
    }

    @FXML
    private void onAddItem() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("CREATE NEW ITEM");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        // Load Stylesheets
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

        // --- Header Setup ---
        Label headerLabel = new Label("Create New Item");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        // --- Main Layout (Two Columns) ---
        HBox mainLayout = new HBox(32); // Use fixed spacing between cards
        mainLayout.setPadding(new Insets(30, 40, 30, 40)); // Increased overall padding
        mainLayout.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        // LEFT COLUMN: Identity
        VBox colLeft = new VBox(28); // Increased internal spacing
        colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
        colLeft.setPrefWidth(340);
        colLeft.setFillWidth(true); // Ensure children fill width

        // Name Field
        VBox nameBox = new VBox(8);
        Label nameEyebrow = new Label("ITEM NAME");
        nameEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Halimaw Signature Burger");
        nameField.getStyleClass().add("premium-field");
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameBox.getChildren().addAll(nameEyebrow, nameField);

        // Category Field
        VBox catBox = new VBox(8);
        Label catEyebrow = new Label("CATEGORY");
        catEyebrow.getStyleClass().add("dialog-eyebrow");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getStyleClass().add("premium-combo");
        categoryCombo.setMaxWidth(Double.MAX_VALUE); // Fill width
        categoryCombo.setPromptText("Select Category");
        categoryCombo.getItems().addAll(menuItemDAO.getAllCategories());
        catBox.getChildren().addAll(catEyebrow, categoryCombo);

        // Price Field
        VBox priceBox = new VBox(8);
        Label priceEyebrow = new Label("BASE PRICE (PHP)");
        priceEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField priceField = new TextField();
        priceField.setPromptText("0.00");
        priceField.getStyleClass().add("premium-field");
        priceField.setMaxWidth(Double.MAX_VALUE);
        priceBox.getChildren().addAll(priceEyebrow, priceField);

        colLeft.getChildren().addAll(nameBox, catBox, priceBox);

        // RIGHT COLUMN: Recipe Builder
        VBox colRight = new VBox(24);
        colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
        colRight.setPrefWidth(400);
        colRight.setFillWidth(true);

        Label recipeEyebrow = new Label("RECIPE CONSTRUCTION");
        recipeEyebrow.getStyleClass().add("dialog-eyebrow");

        // Ingredient Search & Add
        VBox searchArea = new VBox(12);
        
        // Wrapped Search Bar
        HBox searchInputs = new HBox(10);
        searchInputs.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox ingSearchWrapper = new HBox(10);
        ingSearchWrapper.getStyleClass().add("search-bar-group");
        ingSearchWrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(ingSearchWrapper, javafx.scene.layout.Priority.ALWAYS);

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.getStyleClass().add("search-icon-svg");
        searchIcon.setScaleX(0.7);
        searchIcon.setScaleY(0.7);

        TextField ingSearch = new TextField();
        ingSearch.getStyleClass().add("premium-field");
        ingSearch.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 8 0 8 0;");
        ingSearch.setPromptText("Search Ingredient...");
        HBox.setHgrow(ingSearch, javafx.scene.layout.Priority.ALWAYS);
        
        ingSearchWrapper.getChildren().addAll(searchIcon, ingSearch);

        TextField qtyField = new TextField();
        qtyField.getStyleClass().add("premium-field");
        qtyField.setPrefWidth(70);
        qtyField.setPromptText("Qty");

        Button addBtn = new Button("ADD");
        addBtn.getStyleClass().add("btn-recipe-add");

        searchInputs.getChildren().addAll(ingSearchWrapper, qtyField, addBtn);
        searchArea.getChildren().addAll(recipeEyebrow, searchInputs);

        // Current Recipe List (Chips)
        javafx.scene.control.ScrollPane recipeScroll = new javafx.scene.control.ScrollPane();
        recipeScroll.setFitToWidth(true);
        recipeScroll.setPrefHeight(220);
        recipeScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        
        VBox ingredientChipContainer = new VBox(10);
        ingredientChipContainer.setPadding(new Insets(4, 0, 0, 0));
        recipeScroll.setContent(ingredientChipContainer);

        colRight.getChildren().addAll(searchArea, recipeScroll);

        mainLayout.getChildren().addAll(colLeft, colRight);

        // --- Logic & Events ---
        List<MenuItemIngredient> ingredientDataList = new ArrayList<>();
        List<String> allIngredientNames = menuItemDAO.searchIngredients("").stream().map(Ingredient::getName).collect(Collectors.toList());

        // Ingredient Autocomplete (Reusing logic but with new UI)
        javafx.scene.control.ListView<String> ingSuggestionList = new javafx.scene.control.ListView<>();
        ingSuggestionList.getStyleClass().add("suggestion-list");
        ingSuggestionList.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        ingSuggestionList.setFixedCellSize(40);
        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(ingSuggestionList);

        ingSearch.textProperty().addListener(obs -> {
            String q = ingSearch.getText().trim().toLowerCase();
            if (q.isEmpty()) { suggestionPopup.hide(); return; }
            List<String> matches = allIngredientNames.stream().filter(n -> n.toLowerCase().contains(q)).collect(Collectors.toList());
            if (!matches.isEmpty()) {
                ingSuggestionList.setItems(FXCollections.observableArrayList(matches));
                ingSuggestionList.setPrefHeight(Math.min(matches.size(), 5) * 40 + 8);
                ingSuggestionList.setPrefWidth(ingSearchWrapper.getWidth());
                javafx.geometry.Bounds bounds = ingSearchWrapper.localToScreen(ingSearchWrapper.getBoundsInLocal());
                suggestionPopup.show(ingSearchWrapper, bounds.getMinX(), bounds.getMaxY() + 4);
            } else { suggestionPopup.hide(); }
        });

        ingSuggestionList.setOnMouseClicked(e -> {
            if (!ingSuggestionList.getSelectionModel().isEmpty()) {
                ingSearch.setText(ingSuggestionList.getSelectionModel().getSelectedItem());
                suggestionPopup.hide();
            }
        });

        addBtn.setOnAction(e -> {
            String txt = ingSearch.getText().trim();
            String qtxt = qtyField.getText().trim();
            if (txt.isEmpty() || qtxt.isEmpty()) return;
            
            try {
                double q = Double.parseDouble(qtxt);
                List<Ingredient> res = menuItemDAO.searchIngredients(txt);
                if (!res.isEmpty()) {
                    Ingredient i = res.get(0);
                    MenuItemIngredient mi = new MenuItemIngredient(i.getId(), i.getName(), i.getUnit(), q);
                    ingredientDataList.add(mi);
                    
                    // Add Chip UI
                    HBox chip = createIngredientChip(mi, ingredientDataList, ingredientChipContainer);
                    ingredientChipContainer.getChildren().add(chip);
                    
                    ingSearch.clear(); qtyField.clear();
                }
            } catch (Exception ex) {}
        });

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);

        // Styling Dialog Buttons
        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("SAVE ITEM");
        okButton.getStyleClass().add("dialog-button-save");

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-cancel");
        
        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK);

        okButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String cat = categoryCombo.getValue();
            String ptxt = priceField.getText().trim();
            if (name.isEmpty() || cat == null || ptxt.isEmpty() || ingredientDataList.isEmpty()) {
                e.consume();
                return;
            }
            try {
                double p = Double.parseDouble(ptxt);
                int nid = menuItemDAO.insertAndGetId(name, cat, p);
                if (nid > 0) {
                    menuItemDAO.updateMenuItemIngredients(nid, ingredientDataList);
                    loadMenuItems();
                    dialog.close();
                }
            } catch (Exception ex) { e.consume(); }
        });

        dialog.showAndWait();
    }

    private HBox createIngredientChip(MenuItemIngredient mi, List<MenuItemIngredient> dataList, VBox container) {
        HBox chip = new HBox(12);
        chip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        chip.getStyleClass().add("ingredient-chip");

        Label name = new Label(mi.getIngredientName().toUpperCase());
        name.getStyleClass().add("chip-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        StackPane qtyPill = new StackPane();
        qtyPill.getStyleClass().add("chip-qty-pill");
        Label qtyText = new Label(mi.getQuantity() + " " + mi.getUnit());
        qtyText.getStyleClass().add("chip-qty-text");
        qtyPill.getChildren().add(qtyText);

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("btn-chip-remove");
        removeBtn.setOnAction(e -> {
            dataList.remove(mi);
            container.getChildren().remove(chip);
        });

        chip.getChildren().addAll(name, spacer, qtyPill, removeBtn);
        return chip;
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
        colName.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-name"); }
            }
        });

        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colCategory.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item.toUpperCase()); getStyleClass().add("cell-cat"); }
            }
        });

        colPrice.setCellValueFactory(new PropertyValueFactory<>("formattedPrice"));
        colPrice.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else { setText(item); getStyleClass().add("cell-price"); }
            }
        });

        menuItemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colAvailability.setCellValueFactory(new PropertyValueFactory<>("availability"));
        colAvailability.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null); setText(null); return;
                }

                Label pill = new Label(status.toUpperCase());
                pill.getStyleClass().add("status-pill");

                if ("Available".equals(status)) pill.getStyleClass().add("pill-available");
                else if ("Low Stock".equals(status)) pill.getStyleClass().add("pill-low");
                else pill.getStyleClass().add("pill-out");

                setGraphic(pill); setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                MenuItemModel menuItem = getTableView().getItems().get(getIndex());
                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem edit = new MenuItem("Edit Details");
                edit.setOnAction(e -> showEditPopup(menuItem));

                if ("Unavailable".equals(menuItem.getAvailability())) {
                    MenuItem enable = new MenuItem("Enable Item");
                    enable.setOnAction(e -> {
                        menuItemDAO.updateAvailability(menuItem.getId(), "Available");
                        loadMenuItems();
                    });
                    menuBtn.getItems().addAll(edit, enable);
                } else {
                    MenuItem disable = new MenuItem("Disable Item");
                    disable.setOnAction(e -> {
                        menuItemDAO.updateAvailability(menuItem.getId(), "Unavailable");
                        loadMenuItems();
                    });
                    menuBtn.getItems().addAll(edit, disable);
                }
                setGraphic(menuBtn);
            }
        });
    }

    private void showEditPopup(MenuItemModel menuItem) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("EDIT MENU ITEM");
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        
        // Load Stylesheets
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());

        // Header
        Label headerLabel = new Label("Edit Menu Item");
        headerLabel.getStyleClass().add("dialog-header-text");
        dialog.getDialogPane().setHeader(headerLabel);

        // Main Layout
        HBox mainLayout = new HBox(32);
        mainLayout.setPadding(new Insets(30, 40, 30, 40));
        mainLayout.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        MenuItemModel fullItem = menuItemDAO.findById(menuItem.getId());
        List<MenuItemIngredient> currentIngredients = menuItemDAO.getIngredientsForMenuItem(menuItem.getId());
        List<String> categories = menuItemDAO.getAllCategories();

        // LEFT COLUMN: Identity
        VBox colLeft = new VBox(28);
        colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
        colLeft.setPrefWidth(340);

        VBox nameBox = new VBox(8);
        Label nameEyebrow = new Label("ITEM NAME");
        nameEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField nameField = new TextField(fullItem.getName());
        nameField.getStyleClass().add("premium-field");
        nameField.setMaxWidth(Double.MAX_VALUE);
        Label nameError = new Label("");
        nameError.getStyleClass().add("dialog-error");
        nameError.setVisible(false);
        nameError.setManaged(false);
        nameBox.getChildren().addAll(nameEyebrow, nameField, nameError);

        VBox catBox = new VBox(8);
        Label catEyebrow = new Label("CATEGORY");
        catEyebrow.getStyleClass().add("dialog-eyebrow");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getStyleClass().add("premium-combo");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.getItems().addAll(categories);
        categoryCombo.getSelectionModel().select(fullItem.getCategory());
        catBox.getChildren().addAll(catEyebrow, categoryCombo);

        VBox priceBox = new VBox(8);
        Label priceEyebrow = new Label("BASE PRICE (PHP)");
        priceEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField priceField = new TextField(String.format("%.2f", fullItem.getPrice()));
        priceField.getStyleClass().add("premium-field");
        priceField.setMaxWidth(Double.MAX_VALUE);
        Label priceError = new Label("");
        priceError.getStyleClass().add("dialog-error");
        priceError.setVisible(false);
        priceError.setManaged(false);
        priceBox.getChildren().addAll(priceEyebrow, priceField, priceError);

        colLeft.getChildren().addAll(nameBox, catBox, priceBox);

        // RIGHT COLUMN: Recipe Builder
        VBox colRight = new VBox(24);
        colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
        colRight.setPrefWidth(400);

        Label recipeEyebrow = new Label("RECIPE CONSTRUCTION");
        recipeEyebrow.getStyleClass().add("dialog-eyebrow");

        VBox searchArea = new VBox(12);
        HBox searchInputs = new HBox(10);
        searchInputs.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox ingSearchWrapper = new HBox(10);
        ingSearchWrapper.getStyleClass().add("search-bar-group");
        ingSearchWrapper.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(ingSearchWrapper, javafx.scene.layout.Priority.ALWAYS);

        SVGPath searchIcon = new SVGPath();
        searchIcon.setContent("M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z");
        searchIcon.getStyleClass().add("search-icon-svg");
        searchIcon.setScaleX(0.7); searchIcon.setScaleY(0.7);

        TextField searchField = new TextField();
        searchField.getStyleClass().add("premium-field");
        searchField.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-padding: 8 0 8 0;");
        searchField.setPromptText("Search Ingredient...");
        HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS);
        ingSearchWrapper.getChildren().addAll(searchIcon, searchField);

        TextField qtyField = new TextField();
        qtyField.getStyleClass().add("premium-field");
        qtyField.setPrefWidth(70);
        qtyField.setPromptText("Qty");

        Button addBtn = new Button("ADD");
        addBtn.getStyleClass().add("btn-recipe-add");

        searchInputs.getChildren().addAll(ingSearchWrapper, qtyField, addBtn);
        searchArea.getChildren().addAll(recipeEyebrow, searchInputs);

        javafx.scene.control.ScrollPane recipeScroll = new javafx.scene.control.ScrollPane();
        recipeScroll.setFitToWidth(true);
        recipeScroll.setPrefHeight(220);
        recipeScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        
        VBox ingredientList = new VBox(10);
        ingredientList.setPadding(new Insets(4, 0, 0, 0));
        recipeScroll.setContent(ingredientList);

        colRight.getChildren().addAll(searchArea, recipeScroll);
        mainLayout.getChildren().addAll(colLeft, colRight);

        final List<MenuItemIngredient> ingredientDataList = new ArrayList<>(currentIngredients);
        for (MenuItemIngredient ing : currentIngredients) {
            ingredientList.getChildren().add(createIngredientChip(ing, ingredientDataList, ingredientList));
        }

        // Ingredient Autocomplete
        List<String> allIngredientNames = menuItemDAO.searchIngredients("").stream().map(Ingredient::getName).collect(Collectors.toList());
        javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.getStyleClass().add("suggestion-list");
        suggestionList.getStylesheets().add(getClass().getResource("/css/common.css").toExternalForm());
        suggestionList.setFixedCellSize(40);
        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionList);

        searchField.textProperty().addListener(obs -> {
            String q = searchField.getText().trim().toLowerCase();
            if (q.isEmpty()) { suggestionPopup.hide(); return; }
            List<String> matches = allIngredientNames.stream().filter(n -> n.toLowerCase().contains(q)).collect(Collectors.toList());
            if (!matches.isEmpty()) {
                suggestionList.setItems(FXCollections.observableArrayList(matches));
                suggestionList.setPrefHeight(Math.min(matches.size(), 5) * 40 + 8);
                suggestionList.setPrefWidth(ingSearchWrapper.getWidth());
                javafx.geometry.Bounds bounds = ingSearchWrapper.localToScreen(ingSearchWrapper.getBoundsInLocal());
                suggestionPopup.show(ingSearchWrapper, bounds.getMinX(), bounds.getMaxY() + 4);
            } else { suggestionPopup.hide(); }
        });

        suggestionList.setOnMouseClicked(e -> {
            if (!suggestionList.getSelectionModel().isEmpty()) {
                searchField.setText(suggestionList.getSelectionModel().getSelectedItem());
                suggestionPopup.hide();
            }
        });

        addBtn.setOnAction(e -> {
            String txt = searchField.getText().trim();
            String qtxt = qtyField.getText().trim();
            if (txt.isEmpty() || qtxt.isEmpty()) return;
            try {
                double q = Double.parseDouble(qtxt);
                List<Ingredient> res = menuItemDAO.searchIngredients(txt);
                if (!res.isEmpty()) {
                    Ingredient i = res.get(0);
                    MenuItemIngredient mi = new MenuItemIngredient(i.getId(), i.getName(), i.getUnit(), q);
                    ingredientDataList.add(mi);
                    ingredientList.getChildren().add(createIngredientChip(mi, ingredientDataList, ingredientList));
                    searchField.clear(); qtyField.clear();
                }
            } catch (Exception ex) {}
        });

        dialog.getDialogPane().setContent(mainLayout);
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.CANCEL, javafx.scene.control.ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("UPDATE ITEM");
        okButton.getStyleClass().add("dialog-button-save");

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-button-cancel");

        dialog.setResultConverter(btn -> btn == javafx.scene.control.ButtonType.OK);

        okButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String cat = categoryCombo.getValue();
            String ptxt = priceField.getText().trim();
            if (name.isEmpty() || cat == null || ptxt.isEmpty() || ingredientDataList.isEmpty()) {
                e.consume();
                return;
            }
            try {
                double p = Double.parseDouble(ptxt);
                if (menuItemDAO.updateMenuItem(menuItem.getId(), name, cat, p)) {
                    menuItemDAO.updateMenuItemIngredients(menuItem.getId(), ingredientDataList);
                    loadMenuItems();
                    dialog.close();
                }
            } catch (Exception ex) { e.consume(); }
        });

        dialog.showAndWait();
    }

    private void addIngredientRow(VBox ingredientList, MenuItemIngredient ing, List<MenuItemIngredient> dataList, String fieldStyle) {
        HBox row = new HBox(10);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label(ing.getIngredientName() + " (" + ing.getQuantity() + " " + ing.getUnit() + ")");
        nameLabel.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px;");

        javafx.scene.control.Button removeBtn = new javafx.scene.control.Button("X");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e07070; -fx-font-size: 12px; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> {
            dataList.remove(ing);
            ingredientList.getChildren().remove(row);
        });

        row.getChildren().addAll(nameLabel, removeBtn);
        ingredientList.getChildren().add(row);
    }

    private void showDisableConfirmation(MenuItemModel menuItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Disable Menu Item");
        alert.setHeaderText("Disable " + menuItem.getName() + "?");
        alert.setContentText("This menu item will no longer be available for orders.");
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
            menuItemDAO.updateAvailability(menuItem.getId(), "Unavailable");
            loadMenuItems();
        });

        javafx.scene.control.Button cancelButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancelType);
        cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        alert.showAndWait();
    }

    private void showEnableConfirmation(MenuItemModel menuItem) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Enable Menu Item");
        alert.setHeaderText("Enable " + menuItem.getName() + "?");
        alert.setContentText("This menu item will be available for orders.");
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
            menuItemDAO.updateAvailability(menuItem.getId(), "Available");
            loadMenuItems();
        });

        javafx.scene.control.Button cancelButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(cancelType);
        cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        alert.showAndWait();
    }

    private void loadMenuItems() {
        try {
            menuItemDAO.syncAvailabilityToDatabase();

            int total = menuItemDAO.getTotalCount();
            int available = menuItemDAO.getAvailableCount();
            int low = menuItemDAO.getLowStockCount();
            int out = menuItemDAO.getOutOfStockCount();
            allMenuItems = menuItemDAO.findAllWithIngredientStatus();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblAvailable.setText(String.valueOf(available));
                lblLowStock.setText(String.valueOf(low));
                lblOutOfStock.setText(String.valueOf(out));
                menuItemsTable.setItems(FXCollections.observableArrayList(allMenuItems));
            });
        } catch (Exception e) {
            System.err.println("Error loading menu items: " + e.getMessage());
        }
    }

    private String hexToRgb(String hex) {
        hex = hex.replace("#", "");
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return r + "," + g + "," + b;
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