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

    private MenuItemDAO menuItemDAO = new MenuItemDAO();
    private boolean alreadyLoaded = false;

    @FXML
    public void initialize() {
        if (alreadyLoaded) return;
        alreadyLoaded = true;

        setActiveNav("Menu Items");
        setupTableColumns();
        loadMenuItems();
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
        colName.setCellValueFactory(new PropertyValueFactory<MenuItemModel, String>("name"));

        colCategory.setCellValueFactory(new PropertyValueFactory<MenuItemModel, String>("category"));

        colPrice.setCellValueFactory(new PropertyValueFactory<MenuItemModel, String>("formattedPrice"));

        menuItemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colAvailability.setCellValueFactory(new PropertyValueFactory<MenuItemModel, String>("availability"));
        colAvailability.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
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

                if ("Available".equals(status)) {
                    pill.getStyleClass().add("pill-ok");
                } else if ("Low Stock".equals(status)) {
                    pill.getStyleClass().add("pill-low");
                } else if ("Out of Stock".equals(status)) {
                    pill.getStyleClass().add("pill-out");
                }

                setGraphic(pill);
                setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<MenuItemModel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                MenuItemModel menuItem = getTableView().getItems().get(getIndex());
                if (menuItem == null) {
                    setGraphic(null);
                    return;
                }

                MenuButton menuBtn = new MenuButton("...");
                menuBtn.getStyleClass().add("menu-btn-dots");

                MenuItem edit = new MenuItem("Edit");
                edit.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");
                edit.setOnAction(e -> showEditPopup(menuItem));

                MenuItem availability = new MenuItem("Availability");
                availability.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");

                MenuItem delete = new MenuItem("Delete");
                delete.setStyle("-fx-text-fill: #e07070; -fx-font-size: 13px; -fx-padding: 8 16 8 16;");

                menuBtn.getItems().addAll(edit, availability, delete);

                setGraphic(menuBtn);
                setText(null);
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

        Label categoryLabel = new Label("Category:");
        categoryLabel.setStyle(labelStyle);
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setStyle("-fx-background-color: #221a0e; -fx-text-fill: white; -fx-prompt-text-fill: #8a7055; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 13px;");
        categoryCombo.setPrefWidth(300);
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

        ComboBox<String> searchCombo = new ComboBox<>();
        searchCombo.setEditable(true);
        searchCombo.setStyle(fieldStyle);
        searchCombo.setPrefWidth(200);
        searchCombo.setPromptText("Search ingredient...");
        TextField searchField = searchCombo.getEditor();

        javafx.scene.control.ListView<String> suggestionList = new javafx.scene.control.ListView<>();
        suggestionList.setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");
        suggestionList.setPrefHeight(100);
        suggestionList.setVisible(false);

        StackPane searchContainer = new StackPane();
        searchContainer.getChildren().addAll(searchCombo, suggestionList);
        searchContainer.setPrefWidth(200);

        TextField qtyField = new TextField();
        qtyField.setStyle(fieldStyle);
        qtyField.setPrefWidth(80);
        qtyField.setPromptText("Qty");

        javafx.scene.control.Button addBtn = new javafx.scene.control.Button("Send");
        addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 12px; -fx-cursor: hand;");
        addBtn.setOnMouseEntered(e -> addBtn.setStyle("-fx-background-color: #66BB6A; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 12px; -fx-cursor: hand;"));
        addBtn.setOnMouseExited(e -> addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-font-size: 12px; -fx-cursor: hand;"));

        javafx.scene.control.Label qtyError = new javafx.scene.control.Label("");
        qtyError.setStyle(errorStyle);
        qtyError.setVisible(false);

        searchBox.getChildren().addAll(searchContainer, qtyField, addBtn, qtyError);

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
                suggestionList.setVisible(false);
                return;
            }

            List<String> matches = allIngredientNames.stream()
                    .filter(name -> name.toLowerCase().contains(query))
                    .limit(10)
                    .collect(Collectors.toList());

            if (!matches.isEmpty()) {
                suggestionList.setItems(FXCollections.observableArrayList(matches));
                suggestionList.setVisible(true);
            } else {
                suggestionList.setVisible(false);
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
                suggestionList.setVisible(false);
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
                Alert warn = new Alert(Alert.AlertType.WARNING);
                warn.setTitle("Warning");
                warn.setHeaderText("Ingredient already in list");
                warn.setContentText(searchText + " is already added to this menu item.");
                warn.showAndWait();
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
        okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");

        dialog.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL).setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #a09070; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px;");

        dialog.setResultConverter(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                return true;
            }
            return false;
        });

        Runnable updateOkButtonState = () -> {
            boolean hasErrors = nameError.isVisible() || priceError.isVisible() || qtyError.isVisible();
            if (hasErrors) {
                okButton.setDisable(true);
                okButton.setStyle("-fx-background-color: #5c4828; -fx-text-fill: #8a7055; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
            } else {
                okButton.setDisable(false);
                okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
        };

        nameField.textProperty().addListener(obs -> updateOkButtonState.run());
        priceField.textProperty().addListener(obs -> updateOkButtonState.run());

        updateOkButtonState.run();

        dialog.showAndWait().filter(result -> result).ifPresent(result -> {
            boolean valid = true;
            String name = nameField.getText().trim();
            String category = categoryCombo.getValue();
            String priceText = priceField.getText().trim();

            if (name.isEmpty()) {
                nameError.setText("Required");
                nameError.setVisible(true);
                nameField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            } else {
                nameError.setVisible(false);
                nameField.setStyle(fieldStyle);
            }

            double price = 0;
            try {
                price = Double.parseDouble(priceText);
                priceError.setVisible(false);
                priceField.setStyle(fieldStyle);
            } catch (NumberFormatException ex) {
                priceError.setText("Invalid price");
                priceError.setVisible(true);
                priceField.setStyle(fieldStyle + "-fx-border-color: #e07070;");
                valid = false;
            }

            if (valid && category != null) {
                boolean updated = menuItemDAO.updateMenuItem(menuItem.getId(), name, category, price);
                if (updated) {
                    menuItemDAO.updateMenuItemIngredients(menuItem.getId(), ingredientDataList);
                    loadMenuItems();
                }
            }
        });
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

    private void loadMenuItems() {
        try {
            int total = menuItemDAO.getTotalCount();
            int available = menuItemDAO.getAvailableCount();
            int low = menuItemDAO.getLowStockCount();
            int out = menuItemDAO.getOutOfStockCount();
            List<MenuItemModel> items = menuItemDAO.findAllWithIngredientStatus();

            Platform.runLater(() -> {
                lblTotal.setText(String.valueOf(total));
                lblAvailable.setText(String.valueOf(available));
                lblLowStock.setText(String.valueOf(low));
                lblOutOfStock.setText(String.valueOf(out));
                menuItemsTable.setItems(FXCollections.observableArrayList(items));
            });
        } catch (Exception e) {
            System.err.println("Error loading menu items: " + e.getMessage());
        }
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
        System.out.println("Combos & Promos - Coming soon");
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