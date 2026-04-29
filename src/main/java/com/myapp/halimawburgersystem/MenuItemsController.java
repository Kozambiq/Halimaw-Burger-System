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
    @FXML private Button catChicken;
    @FXML private Button catSides;
    @FXML private Button catDrinks;

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
    private void onCategorySelect(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        selectedCategory = source.getText().trim();
        
        // Update button highlights
        catAll.getStyleClass().remove("cat-nav-item-active");
        catBurgers.getStyleClass().remove("cat-nav-item-active");
        catChicken.getStyleClass().remove("cat-nav-item-active");
        catSides.getStyleClass().remove("cat-nav-item-active");
        catDrinks.getStyleClass().remove("cat-nav-item-active");
        
        source.getStyleClass().add("cat-nav-item-active");
        
        applyFilters();
    }

    private void setupSearchAutocomplete() {
        javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.getStyleClass().add("suggestion-list");
        suggestionList.setFixedCellSize(36);
        suggestionList.setMaxHeight(200);
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
                suggestionList.setPrefHeight(rowCount * 36 + 2);
                javafx.geometry.Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal());
                suggestionPopup.show(searchField, bounds.getMinX(), bounds.getMaxY());
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
        HBox mainLayout = new HBox(0);
        mainLayout.setPadding(new Insets(20, 20, 20, 20));

        // LEFT COLUMN: Identity
        VBox colLeft = new VBox(24);
        colLeft.getStyleClass().addAll("dialog-col-left", "dialog-section-card");
        colLeft.setPrefWidth(320);

        // Name Field
        VBox nameBox = new VBox(8);
        Label nameEyebrow = new Label("ITEM NAME");
        nameEyebrow.getStyleClass().add("dialog-eyebrow");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Halimaw Signature Burger");
        nameField.getStyleClass().add("premium-field");
        nameBox.getChildren().addAll(nameEyebrow, nameField);

        // Category Field
        VBox catBox = new VBox(8);
        Label catEyebrow = new Label("CATEGORY");
        catEyebrow.getStyleClass().add("dialog-eyebrow");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getStyleClass().add("premium-combo");
        categoryCombo.setPrefWidth(300);
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
        priceBox.getChildren().addAll(priceEyebrow, priceField);

        colLeft.getChildren().addAll(nameBox, catBox, priceBox);

        Region colSpacer = new Region();
        HBox.setHgrow(colSpacer, javafx.scene.layout.Priority.ALWAYS);

        // RIGHT COLUMN: Recipe Builder
        VBox colRight = new VBox(24);
        colRight.getStyleClass().addAll("dialog-col-right", "dialog-section-card");
        colRight.setPrefWidth(380);

        Label recipeEyebrow = new Label("RECIPE CONSTRUCTION");
        recipeEyebrow.getStyleClass().add("dialog-eyebrow");

        // Ingredient Search & Add
        VBox searchArea = new VBox(12);
        HBox searchInputs = new HBox(10);
        searchInputs.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TextField ingSearch = new TextField();
        ingSearch.getStyleClass().add("premium-field");
        ingSearch.setPromptText("Search Ingredient...");
        HBox.setHgrow(ingSearch, javafx.scene.layout.Priority.ALWAYS);

        TextField qtyField = new TextField();
        qtyField.getStyleClass().add("premium-field");
        qtyField.setPrefWidth(60);
        qtyField.setPromptText("Qty");

        Button addBtn = new Button("ADD");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setStyle("-fx-padding: 8 16 8 16; -fx-font-size: 11px;");

        searchInputs.getChildren().addAll(ingSearch, qtyField, addBtn);
        searchArea.getChildren().addAll(recipeEyebrow, searchInputs);

        // Current Recipe List (Chips)
        javafx.scene.control.ScrollPane recipeScroll = new javafx.scene.control.ScrollPane();
        recipeScroll.setFitToWidth(true);
        recipeScroll.setPrefHeight(200);
        recipeScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-width: 0;");
        
        VBox ingredientChipContainer = new VBox(10);
        ingredientChipContainer.setPadding(new Insets(4, 0, 0, 0));
        recipeScroll.setContent(ingredientChipContainer);

        colRight.getChildren().addAll(searchArea, recipeScroll);

        mainLayout.getChildren().addAll(colLeft, colSpacer, colRight);

        // --- Logic & Events ---
        List<MenuItemIngredient> ingredientDataList = new ArrayList<>();
        List<String> allIngredientNames = menuItemDAO.searchIngredients("").stream().map(Ingredient::getName).collect(Collectors.toList());

        // Ingredient Autocomplete (Reusing logic but with new UI)
        javafx.scene.control.ListView<String> ingSuggestionList = new javafx.scene.control.ListView<>();
        ingSuggestionList.getStyleClass().add("suggestion-list");
        ingSuggestionList.setFixedCellSize(36);
        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(ingSuggestionList);

        ingSearch.textProperty().addListener(obs -> {
            String q = ingSearch.getText().trim().toLowerCase();
            if (q.isEmpty()) { suggestionPopup.hide(); return; }
            List<String> matches = allIngredientNames.stream().filter(n -> n.toLowerCase().contains(q)).collect(Collectors.toList());
            if (!matches.isEmpty()) {
                ingSuggestionList.setItems(FXCollections.observableArrayList(matches));
                ingSuggestionList.setPrefHeight(Math.min(matches.size(), 5) * 36 + 2);
                javafx.geometry.Bounds bounds = ingSearch.localToScreen(ingSearch.getBoundsInLocal());
                suggestionPopup.show(ingSearch, bounds.getMinX(), bounds.getMaxY());
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

        Button okButton = (Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("SAVE ITEM");
        okButton.getStyleClass().add("btn-primary");
        
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
        dialog.setTitle("Edit Menu Item");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("dialog-pane");
        dialog.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        String labelStyle = "-fx-text-fill: #a09070; -fx-font-size: 12px;";
        String fieldStyle = "-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;";
        String errorStyle = "-fx-text-fill: #e07070; -fx-font-size: 11px;";

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setMaxWidth(500);

        MenuItemModel fullItem = menuItemDAO.findById(menuItem.getId());
        List<MenuItemIngredient> currentIngredients = menuItemDAO.getIngredientsForMenuItem(menuItem.getId());
        List<String> categories = menuItemDAO.getAllCategories();

        Label nameLabel = new Label("Name:");
        nameLabel.setStyle(labelStyle);
        TextField nameField = new TextField(fullItem.getName());
        nameField.setStyle(fieldStyle);
        nameField.setPrefWidth(300);
        Label nameError = new Label("");
        nameError.setStyle(errorStyle);
        nameError.setVisible(false);
        nameError.setManaged(false);

        Label categoryLabel = new Label("Category:");
        categoryLabel.setStyle(labelStyle);
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setStyle("-fx-background-color: #221a0e; -fx-text-fill: white; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;");
        categoryCombo.setPrefWidth(300);
        categoryCombo.setCellFactory(list -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #221a0e; -fx-text-fill: #f5ede0; -fx-font-size: 13px;");
                }
            }
        });
        categoryCombo.setButtonCell(new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-background-color: #221a0e; -fx-text-fill: white; -fx-font-size: 13px;");
                }
            }
        });
        categoryCombo.getItems().addAll(categories);
        categoryCombo.getSelectionModel().select(fullItem.getCategory());
        categoryCombo.setOnShown(e -> {
            categoryCombo.getStyleClass().add("category-combo-visible");
        });

        Label priceLabel = new Label("Price:");
        priceLabel.setStyle(labelStyle);
        TextField priceField = new TextField(String.format("%.2f", fullItem.getPrice()));
        priceField.setStyle(fieldStyle);
        priceField.setPrefWidth(300);
        Label priceError = new Label("");
        priceError.setStyle(errorStyle);
        priceError.setVisible(false);
        priceError.setManaged(false);

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(nameError, 1, 1);
        grid.add(categoryLabel, 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(priceLabel, 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(priceError, 1, 4);

        VBox ingredientsSection = new VBox(10);
        Label ingredientsLabel = new Label("Ingredients:");
        ingredientsLabel.setStyle(labelStyle);

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setStyle(fieldStyle);
        searchField.setPrefWidth(200);
        searchField.setPromptText("Search ingredient...");

javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.getStyleClass().add("suggestion-list");
        suggestionList.setFixedCellSize(32);
        suggestionList.setMaxHeight(160);
        suggestionList.setPrefWidth(220);
        suggestionList.setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        suggestionList.setCellFactory(list -> new javafx.scene.control.ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else { setText(item); setStyle("-fx-background-color: transparent; -fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16; -fx-wrap-text: true;"); }
            }
        });

        javafx.stage.Popup suggestionPopup = new javafx.stage.Popup();
        suggestionPopup.setAutoHide(true);
        suggestionPopup.getContent().add(suggestionList);

        suggestionList.setOnMousePressed(e -> {
            if (!suggestionList.getSelectionModel().isEmpty()) {
                searchField.setText(suggestionList.getSelectionModel().getSelectedItem());
                suggestionPopup.hide();
            }
        });

        TextField qtyField = new TextField();
        qtyField.setStyle(fieldStyle);
        qtyField.setPrefWidth(80);
        qtyField.setPromptText("Qty");

        javafx.scene.control.Button addBtn = new javafx.scene.control.Button("Send");
        addBtn.setLayoutY(0);
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 12px; -fx-cursor: hand;");
        addBtn.setOnMouseEntered(e -> addBtn.setStyle("-fx-background-color: #66BB6A; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 12px; -fx-cursor: hand;"));
        addBtn.setOnMouseExited(e -> addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 12px; -fx-cursor: hand;"));

        javafx.scene.control.Label qtyError = new javafx.scene.control.Label("");
        qtyError.setStyle(errorStyle);
        qtyError.setVisible(false);
        qtyError.setManaged(false);

        searchBox.getChildren().addAll(searchField, qtyField, addBtn, qtyError);

        VBox ingredientList = new VBox(5);
        final List<MenuItemIngredient> ingredientDataList = new ArrayList<>(currentIngredients);
        for (MenuItemIngredient ing : currentIngredients) {
            addIngredientRow(ingredientList, ing, ingredientDataList, fieldStyle);
        }

        List<String> allIngredientNames = menuItemDAO.searchIngredients("").stream()
                .map(Ingredient::getName)
                .collect(Collectors.toList());

        searchField.textProperty().addListener(obs -> {
            String query = searchField.getText().trim().toLowerCase();
            if (query.isEmpty()) {
                searchField.setStyle(fieldStyle);
                qtyError.setVisible(false);
                suggestionPopup.hide();
                return;
            }

 List<String> matches = allIngredientNames.stream()
                    .filter(name -> name.toLowerCase().contains(query))
                    .collect(Collectors.toList());
            if (!matches.isEmpty()) {
                int rowCount = Math.min(matches.size(), 5);
                suggestionList.setItems(FXCollections.observableArrayList(matches));
                suggestionList.setPrefHeight(rowCount * 32 + 4);
                javafx.geometry.Bounds bounds = searchField.localToScreen(searchField.getBoundsInLocal());
                suggestionPopup.show(searchField, bounds.getMinX(), bounds.getMaxY());
            } else {
                suggestionPopup.hide();
            }

            boolean exists = menuItemDAO.ingredientExistsByName(query);
            if (exists) {
                searchField.setStyle(fieldStyle + "-fx-border-color: #4CAF50;");
            } else {
                searchField.setStyle(fieldStyle + "-fx-border-color: #e07070;-fx-background-color: #221a0e;");
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

        addBtn.setOnAction(e -> {
            String searchText = searchField.getText().trim();
            String qtyText = qtyField.getText().trim();

            if (searchText.isEmpty() || qtyText.isEmpty()) {
                return;
            }

            boolean exists = menuItemDAO.ingredientExistsByName(searchText);
            if (!exists) {
                return;
            }

            boolean alreadyExists = false;
            for (MenuItemIngredient existing : ingredientDataList) {
                if (existing.getIngredientName().equalsIgnoreCase(searchText)) {
                    alreadyExists = true;
                    break;
                }
            }

            if (alreadyExists) {
                qtyError.setText(searchText + " already in list");
                qtyError.setVisible(true);
                return;
            }

            String unit = menuItemDAO.getIngredientUnit(searchText);
            boolean isPcs = "pcs".equalsIgnoreCase(unit);

            try {
                double qty = Double.parseDouble(qtyText);

                if (isPcs) {
                    if (qty != Math.floor(qty)) {
                        qtyError.setText("Whole numbers only for pcs");
                        qtyError.setVisible(true);
                        return;
                    }
                }

                List<Ingredient> results = menuItemDAO.searchIngredients(searchText);
                if (!results.isEmpty()) {
                    Ingredient ing = results.get(0);
                    MenuItemIngredient newIng = new MenuItemIngredient(ing.getId(), ing.getName(), ing.getUnit(), qty);
                    ingredientDataList.add(newIng);
                    addIngredientRow(ingredientList, newIng, ingredientDataList, fieldStyle);
                    searchField.clear();
                    qtyField.clear();
                    searchField.setStyle(fieldStyle);
                    qtyError.setVisible(false);
                }
            } catch (NumberFormatException ex) {
                qtyError.setText("Enter valid number");
                qtyError.setVisible(true);
            }
        });

        ingredientsSection.getChildren().addAll(ingredientsLabel, searchBox, ingredientList);

        VBox content = new VBox(10);
        content.getChildren().addAll(grid, ingredientsSection);
        content.setPadding(new Insets(10, 0, 0, 0));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(550);
        dialog.getDialogPane().setMinHeight(400);
        dialog.getDialogPane().getButtonTypes().addAll(
            javafx.scene.control.ButtonType.CANCEL,
            javafx.scene.control.ButtonType.OK
        );

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setText("Save");
        okButton.setDisable(true);
        okButton.setStyle("-fx-background-color: #555555; -fx-text-fill: #aaaaaa; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return true;
            }
            return false;
        });

        Runnable validateForm = () -> {
            String name = nameField.getText().trim();
            String priceText = priceField.getText().trim();
            boolean valid = !name.isEmpty() && !priceText.isEmpty() && categoryCombo.getValue() != null;
            if (valid) {
                try {
                    Double.parseDouble(priceText);
                } catch (Exception ex) {
                    valid = false;
                }
            }
            okButton.setDisable(!valid);
            okButton.setStyle(valid 
                ? "-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;"
                : "-fx-background-color: #555555; -fx-text-fill: #aaaaaa; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
        };
        validateForm.run();

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            String name = newVal.trim();
            nameError.setVisible(false);
            nameError.setManaged(false);
            nameField.setStyle(fieldStyle);
            validateForm.run();
        });

        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            String priceText = newVal.trim();
            priceError.setVisible(false);
            priceError.setManaged(false);
            priceField.setStyle(fieldStyle);
            if (!priceText.isEmpty()) {
                try {
                    double p = Double.parseDouble(priceText);
                    if (p <= 0) {
                        priceError.setText("Must be greater than 0");
                        priceError.setVisible(true);
                        priceError.setManaged(true);
                        priceField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                    }
                } catch (NumberFormatException ex) {
                    priceError.setText("Invalid price");
                    priceError.setVisible(true);
                    priceError.setManaged(true);
                    priceField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                }
            }
            validateForm.run();
        });

        okButton.setOnAction(e -> {
            boolean valid = true;
            String name = nameField.getText().trim();
            String category = categoryCombo.getValue();
            String priceText = priceField.getText().trim();

            nameField.setStyle(fieldStyle);
            priceField.setStyle(fieldStyle);
            nameError.setVisible(false);
            nameError.setManaged(false);
            priceError.setVisible(false);
            priceError.setManaged(false);

            if (name.isEmpty()) {
                nameError.setText("Required");
                nameError.setVisible(true);
                nameError.setManaged(true);
                nameField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else {
                nameError.setVisible(false);
                nameError.setManaged(false);
                nameField.setStyle(fieldStyle);
            }

            double price = 0;
            try {
                price = Double.parseDouble(priceText);
                if (price <= 0) {
                    priceError.setText("Must be greater than 0");
                    priceError.setVisible(true);
                    priceError.setManaged(true);
                    priceField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                    valid = false;
                } else {
                    priceError.setVisible(false);
                    priceError.setManaged(false);
                    priceField.setStyle(fieldStyle);
                }
            } catch (NumberFormatException ex) {
                priceError.setText("Invalid price");
                priceError.setVisible(true);
                priceError.setManaged(true);
                priceField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            }

            if (valid && category != null) {
                boolean updated = menuItemDAO.updateMenuItem(menuItem.getId(), name, category, price);
                if (updated) {
                    menuItemDAO.updateMenuItemIngredients(menuItem.getId(), ingredientDataList);
                    loadMenuItems();
                }
                dialog.close();
            } else {
                e.consume();
            }
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