package com.myapp.halimawburgersystem;

import com.myapp.dao.ComboDAO;
import com.myapp.dao.MenuItemDAO;
import com.myapp.dao.OrderDAO;
import com.myapp.dao.IngredientDAO;
import com.myapp.model.Combo;
import com.myapp.model.MenuItemModel;
import com.myapp.model.Order;
import com.myapp.model.OrderItem;
import com.myapp.model.Staff;
import com.myapp.model.Ingredient;
import com.myapp.service.CashierService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.myapp.util.OrderNotificationService;

public class CashierController {

    @FXML private GridPane menuGrid;
    @FXML private VBox orderItemsList;
    @FXML private TextField txtSearch;
    @FXML private TextField txtOrderNotes;
    @FXML private Label lblOrderNum;
    @FXML private Label lblSubtotal;
    @FXML private Label lblDiscount;
    @FXML private Label lblTotal;
    @FXML private Button btnDineIn;
    @FXML private Button btnTakeout;
    @FXML private Button btnLogout;
    @FXML private Label lblStaffName;
    @FXML private Label lblStaffInitials;
    @FXML private Button btnCatAll;
    @FXML private Button btnCatBurgers;
    @FXML private Button btnCatSides;
    @FXML private Button btnCatDrinks;
    @FXML private Button btnCatOthers;

    private CashierService cashierService = new CashierService();
    private Map<Integer, Integer> orderItems = new HashMap<>();
    private Map<Integer, Double> itemPrices = new HashMap<>();
    private Map<Integer, Double> comboPrices = new HashMap<>();
    private List<MenuItemModel> allMenuItems;
    private List<Combo> allCombos;
    private String currentOrderType = "Dine-in";
    private String currentCategory = "All";
    private int orderNumber = 1;
    private double subtotal = 0.0;
    private static final int CARD_WIDTH = 160;
    private static final int CARD_HGAP = 10;
    private static final int RIGHT_PANEL_WIDTH = 360;
    private static final int GRID_PADDING = 32;

    @FXML
    public void initialize() {
        orderNumber = cashierService.getNextOrderNumber();
        updateOrderNumber();
        updateTotals();
        setActiveToggle(btnDineIn);
        setActiveCategory(btnCatAll);
        updateStaffInfo();

        menuGrid.parentProperty().addListener((obs, oldParent, newParent) -> {
            if (newParent != null) {
                newParent.layoutBoundsProperty().addListener((o, oldBounds, newBounds) -> {
                    if (newBounds.getWidth() > 0 && menuGrid.getChildren().isEmpty()) {
                        reloadMenu();
                    }
                });
            }
        });

        menuGrid.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.widthProperty().addListener((o, ov, nv) ->
                    Platform.runLater(() -> reloadMenu()));
            }
        });
    }

    private void updateStaffInfo() {
        Staff staff = Main.getCurrentStaff();
        if (staff != null) {
            lblStaffName.setText(staff.getName());
            lblStaffInitials.setText(staff.getInitials());
        }
    }

    private int calculateColumns() {
        double availableWidth = 0;
        if (menuGrid.getParent() != null) {
            availableWidth = menuGrid.getParent().getLayoutBounds().getWidth() - GRID_PADDING;
        }
        if (availableWidth <= 0) return 4;
        int columns = (int) (availableWidth / (CARD_WIDTH + CARD_HGAP));
        return Math.max(columns, 1);
    }

    @FXML
    private void onSearchKeyReleased() {
        reloadMenu();
    }

    private void reloadMenu() {
        try {
            String searchText = txtSearch.getText().toLowerCase().trim();
            menuGrid.getChildren().clear();
            
            // 1. Load Promos & Combos First
            int currentRow = loadFilteredCombos(0, searchText);
            
            // 2. Load Menu Items Categories in requested order: Burgers, Drinks, Sides, Others
            loadFilteredMenuItems(currentRow, searchText);
        } catch (Exception e) {
            System.err.println("Error reloading menu: " + e.getMessage());
        }
    }

    private int loadFilteredMenuItems(int startRow, String searchText) {
        try {
            if (allMenuItems == null) {
                cashierService.syncAvailabilityToDatabase();
                allMenuItems = cashierService.findAllWithIngredientStatus();
            }
            
            menuGrid.getColumnConstraints().clear();

            int columns = calculateColumns();
            for (int i = 0; i < columns; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPrefWidth(CARD_WIDTH);
                col.setMinWidth(CARD_WIDTH);
                col.setMaxWidth(CARD_WIDTH);
                menuGrid.getColumnConstraints().add(col);
            }

            Map<String, List<MenuItemModel>> byCategory = new java.util.LinkedHashMap<>();
            // Updated category order: Burgers, Drinks, Sides, Others
            String[] categoryOrder = {"Burgers", "Drinks", "Sides", "Others"};
            for (String cat : categoryOrder) {
                if ("All".equals(currentCategory) || cat.equals(currentCategory)) {
                    byCategory.put(cat, new ArrayList<>());
                }
            }
            
            for (MenuItemModel item : allMenuItems) {
                if (!"All".equals(currentCategory) && !item.getCategory().equals(currentCategory)) continue;
                if (!searchText.isEmpty() && !item.getName().toLowerCase().contains(searchText)) continue;
                
                byCategory.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
            }

            int row = startRow;
            for (String category : byCategory.keySet()) {
                List<MenuItemModel> items = byCategory.get(category);
                if (items == null || items.isEmpty()) continue;
                row = addCategorySection(category, items, columns, row);
            }
            return row;
        } catch (Exception e) {
            System.err.println("Error loading menu items: " + e.getMessage());
            return startRow;
        }
    }

    private int addCategorySection(String category, List<MenuItemModel> items, int columns, int startRow) {
        int row = startRow;

        Label dividerLabel = new Label(category.toUpperCase());
        dividerLabel.getStyleClass().add("menu-divider-label");
        GridPane.setConstraints(dividerLabel, 0, row, columns, 1);
        dividerLabel.setMaxWidth(Double.MAX_VALUE);
        GridPane.setMargin(dividerLabel, new Insets(0, 16, 0, 16)); // Standardized side margins
        menuGrid.getChildren().add(dividerLabel);
        row++;

        int col = 0;
        for (MenuItemModel item : items) {
            itemPrices.put(-item.getId(), item.getPrice());
            StackPane card = createMenuCard(item);
            GridPane.setConstraints(card, col, row);
            menuGrid.getChildren().add(card);

            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
        if (col > 0) row++;

        return row;
    }

    private int loadFilteredCombos(int startRow, String searchText) {
        try {
            if (allCombos == null) {
                allCombos = cashierService.findAll();
            }

            if (allCombos.isEmpty()) return startRow;
            if (!"All".equals(currentCategory)) return startRow;

            List<Combo> filteredCombos = new ArrayList<>();
            for (Combo combo : allCombos) {
                if (!"Active".equals(combo.getStatus())) continue;
                if (!searchText.isEmpty() && !combo.getName().toLowerCase().contains(searchText)) continue;
                filteredCombos.add(combo);
            }

            if (filteredCombos.isEmpty()) return startRow;

            int columns = calculateColumns();

            Label dividerLabel = new Label("PROMOS & COMBOS");
            dividerLabel.getStyleClass().add("menu-divider-label");
            GridPane.setConstraints(dividerLabel, 0, startRow, columns, 1);
            dividerLabel.setMaxWidth(Double.MAX_VALUE);
            GridPane.setMargin(dividerLabel, new Insets(0, 16, 0, 16));
            menuGrid.getChildren().add(dividerLabel);
            int row = startRow + 1;

            int col = 0;
            for (Combo combo : filteredCombos) {
                comboPrices.put(combo.getId(), combo.getPromoPrice());
                StackPane card = createComboCard(combo);
                GridPane.setConstraints(card, col, row);
                menuGrid.getChildren().add(card);

                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
            if (col > 0) row++;
            return row;
        } catch (Exception e) {
            System.err.println("Error loading combos: " + e.getMessage());
            return startRow;
        }
    }

    private StackPane createMenuCard(MenuItemModel item) {
        StackPane card = new StackPane();
        card.setPrefSize(160, 100);
        card.setMinSize(160, 100);
        card.setMaxSize(160, 100);
        card.getStyleClass().add("menu-card");

        VBox content = new VBox(4);
        content.getStyleClass().add("menu-card-content");

        Label catLabel = new Label(item.getCategory());
        catLabel.getStyleClass().add("menu-card-cat");

        Label nameLabel = new Label(item.getName());
        nameLabel.getStyleClass().add("menu-card-name");
        VBox.setVgrow(nameLabel, Priority.ALWAYS);

        Label priceLabel = new Label("₱" + String.format("%.2f", item.getPrice()));
        priceLabel.getStyleClass().add("menu-card-price");

        content.getChildren().addAll(catLabel, nameLabel, priceLabel);

        String availability = item.getAvailability();
        if ("Out of Stock".equals(availability)) {
            card.getStyleClass().add("menu-card-disabled");
            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("menu-card-overlay");
            Label outLabel = new Label("OUT OF STOCK");
            outLabel.getStyleClass().add("menu-out-label");
            overlay.getChildren().add(outLabel);
            card.getChildren().addAll(content, overlay);
        } else if ("Low Stock".equals(availability)) {
            card.getStyleClass().add("menu-card-low");
            Label lowLabel = new Label("LOW STOCK");
            lowLabel.getStyleClass().add("menu-low-label");
            StackPane.setAlignment(lowLabel, javafx.geometry.Pos.TOP_RIGHT);
            StackPane.setMargin(lowLabel, new Insets(8));
            card.getChildren().addAll(content, lowLabel);
        } else {
            card.getChildren().add(content);
        }

        card.setOnMouseClicked((MouseEvent event) -> {
            if (!"Out of Stock".equals(availability)) {
                addMenuItemToOrder(item);
            }
        });

        return card;
    }

    private StackPane createComboCard(Combo combo) {
        StackPane card = new StackPane();
        card.setPrefSize(160, 100);
        card.setMinSize(160, 100);
        card.setMaxSize(160, 100);
        card.getStyleClass().add("menu-card");
        card.getStyleClass().add("menu-card-promo");

        VBox content = new VBox(4);
        content.getStyleClass().add("menu-card-content");

        HBox badgeBox = new HBox();
        Label promoBadge = new Label("PROMO");
        promoBadge.getStyleClass().add("menu-promo-badge");
        badgeBox.getChildren().add(promoBadge);

        Label nameLabel = new Label(combo.getName());
        nameLabel.getStyleClass().add("menu-card-name");
        VBox.setVgrow(nameLabel, Priority.ALWAYS);

        Label priceLabel = new Label("₱" + String.format("%.2f", combo.getPromoPrice()));
        priceLabel.getStyleClass().add("menu-card-price");

        content.getChildren().addAll(badgeBox, nameLabel, priceLabel);
        card.getChildren().add(content);

        card.setOnMouseClicked((MouseEvent event) -> {
            addComboToOrder(combo);
        });

        return card;
    }

    private void addMenuItemToOrder(MenuItemModel item) {
        int itemId = -item.getId();
        int quantity = orderItems.getOrDefault(itemId, 0) + 1;
        orderItems.put(itemId, quantity);

        subtotal += item.getPrice();
        updateOrderDisplay();
        updateTotals();
    }

    private void addComboToOrder(Combo combo) {
        int comboId = combo.getId();
        int quantity = orderItems.getOrDefault(comboId, 0) + 1;
        orderItems.put(comboId, quantity);

        subtotal += combo.getPromoPrice();
        updateOrderDisplay();
        updateTotals();
    }

    private void updateOrderDisplay() {
        orderItemsList.getChildren().clear();

        for (Map.Entry<Integer, Integer> entry : orderItems.entrySet()) {
            int itemId = entry.getKey();
            int qty = entry.getValue();

            if (itemId > 0) {
                Combo combo = findComboById(itemId);
                if (combo == null || qty <= 0) continue;
                addOrderRow(combo.getName(), qty, combo.getPromoPrice());
            } else {
                MenuItemModel item = findMenuItemById(-itemId);
                if (item == null || qty <= 0) continue;
                addOrderRow(item.getName(), qty, item.getPrice());
            }
        }
    }

    private void addOrderRow(String name, int qty, double price) {
        HBox row = new HBox(8);
        row.getStyleClass().add("order-item-row");
        row.setPadding(new Insets(10, 0, 10, 0));
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox info = new VBox(2);
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("order-item-name");
        Label unitPriceLabel = new Label("₱" + String.format("%.2f", price));
        unitPriceLabel.getStyleClass().add("order-item-cat"); // Use existing small text style
        unitPriceLabel.setStyle("-fx-text-fill: #5c4828; -fx-font-size: 10px;");
        info.getChildren().addAll(nameLabel, unitPriceLabel);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        HBox qtyCtrl = new HBox(4);
        qtyCtrl.getStyleClass().add("qty-ctrl");
        qtyCtrl.setAlignment(javafx.geometry.Pos.CENTER);

        Button minusBtn = new Button();
        minusBtn.getStyleClass().add("qty-btn");
        SVGPath minusIcon = new SVGPath();
        minusIcon.setContent("M19 13H5v-2h14v2z");
        minusIcon.setScaleX(0.7);
        minusIcon.setScaleY(0.7);
        minusIcon.getStyleClass().add("qty-btn-icon");
        minusBtn.setGraphic(minusIcon);
        
        final int finalQty = qty;
        final String finalName = name;
        final double finalPrice = price;
        minusBtn.setOnAction(e -> decrementItem(finalName, finalPrice));

        Label qtyLabel = new Label(String.valueOf(qty));
        qtyLabel.getStyleClass().add("qty-val");
        qtyLabel.setMinWidth(24);

        Button plusBtn = new Button();
        plusBtn.getStyleClass().add("qty-btn");
        SVGPath plusIcon = new SVGPath();
        plusIcon.setContent("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
        plusIcon.setScaleX(0.7);
        plusIcon.setScaleY(0.7);
        plusIcon.getStyleClass().add("qty-btn-icon");
        plusBtn.setGraphic(plusIcon);
        plusBtn.setOnAction(e -> incrementItem(finalName, finalPrice));

        qtyCtrl.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        double itemTotal = price * qty;
        Label totalLabel = new Label("₱" + String.format("%.2f", itemTotal));
        totalLabel.getStyleClass().add("order-item-total");
        totalLabel.setMinWidth(70);

        row.getChildren().addAll(info, qtyCtrl, totalLabel);
        orderItemsList.getChildren().add(row);
    }

    private void decrementItem(String name, double price) {
        for (Map.Entry<Integer, Integer> entry : orderItems.entrySet()) {
            int id = entry.getKey();
            if (id > 0) {
                Combo combo = findComboById(id);
                if (combo != null && combo.getName().equals(name)) {
                    int qty = entry.getValue() - 1;
                    if (qty <= 0) {
                        orderItems.remove(id);
                        subtotal -= combo.getPromoPrice() * entry.getValue();
                    } else {
                        entry.setValue(qty);
                        subtotal -= combo.getPromoPrice();
                    }
                    break;
                }
            } else {
                MenuItemModel item = findMenuItemById(-id);
                if (item != null && item.getName().equals(name)) {
                    int qty = entry.getValue() - 1;
                    if (qty <= 0) {
                        orderItems.remove(id);
                        subtotal -= item.getPrice() * entry.getValue();
                    } else {
                        entry.setValue(qty);
                        subtotal -= item.getPrice();
                    }
                    break;
                }
            }
        }
        updateOrderDisplay();
        updateTotals();
    }

    private void incrementItem(String name, double price) {
        for (Map.Entry<Integer, Integer> entry : orderItems.entrySet()) {
            int id = entry.getKey();
            if (id > 0) {
                Combo combo = findComboById(id);
                if (combo != null && combo.getName().equals(name)) {
                    entry.setValue(entry.getValue() + 1);
                    subtotal += combo.getPromoPrice();
                    break;
                }
            } else {
                MenuItemModel item = findMenuItemById(-id);
                if (item != null && item.getName().equals(name)) {
                    entry.setValue(entry.getValue() + 1);
                    subtotal += item.getPrice();
                    break;
                }
            }
        }
        updateOrderDisplay();
        updateTotals();
    }

    private MenuItemModel findMenuItemById(int id) {
        for (MenuItemModel item : allMenuItems) {
            if (item.getId() == id) return item;
        }
        return null;
    }

    private Combo findComboById(int id) {
        for (Combo combo : allCombos) {
            if (combo.getId() == id) return combo;
        }
        return null;
    }

    private void updateTotals() {
        lblSubtotal.setText("₱" + String.format("%.2f", subtotal));
        lblDiscount.setText("₱0.00");
        lblTotal.setText("₱" + String.format("%.2f", subtotal));
    }

    private void updateOrderNumber() {
        lblOrderNum.setText(String.format("%04d", orderNumber));
    }

    @FXML
    private void onToggleDineIn() {
        currentOrderType = "Dine-in";
        setActiveToggle(btnDineIn);
        btnTakeout.getStyleClass().remove("toggle-btn-active");
    }

    @FXML
    private void onToggleTakeout() {
        currentOrderType = "Takeout";
        setActiveToggle(btnTakeout);
        btnDineIn.getStyleClass().remove("toggle-btn-active");
    }

    private void setActiveToggle(Button btn) {
        btn.getStyleClass().add("toggle-btn-active");
    }

    private void setActiveCategory(Button btn) {
        btn.getStyleClass().add("cat-btn-active");
    }

    @FXML
    private void onFilterCategory(ActionEvent event) {
        Button clickedBtn = (Button) event.getSource();
        currentCategory = clickedBtn.getText();
        
        btnCatAll.getStyleClass().remove("cat-btn-active");
        btnCatBurgers.getStyleClass().remove("cat-btn-active");
        btnCatSides.getStyleClass().remove("cat-btn-active");
        btnCatDrinks.getStyleClass().remove("cat-btn-active");
        btnCatOthers.getStyleClass().remove("cat-btn-active");
        clickedBtn.getStyleClass().add("cat-btn-active");
        
        reloadMenu();
    }

    @FXML
    private void onPayCash() {
        processOrder("Cash");
    }

    @FXML
    private void onPayCard() {
        processOrder("GCash");
    }

    @FXML
    private void onClearOrder() {
        orderItems.clear();
        subtotal = 0.0;
        updateOrderDisplay();
        updateTotals();
    }

    private void processOrder(String paymentType) {
        if (orderItems.isEmpty()) {
            showErrorAlert("No items ordered", "Please add items to the order before proceeding with payment.");
            return;
        }

        List<String> outOfStockItems = checkOutOfStockIngredients();
        if (!outOfStockItems.isEmpty()) {
            showOutOfStockWarning(outOfStockItems);
            return;
        }

        String referenceNumber = null;
        if ("GCash".equals(paymentType)) {
            referenceNumber = showGCashDialog();
            if (referenceNumber == null) {
                return;
            }
        }

        Staff staff = Main.getCurrentStaff();
        int staffId = staff != null ? staff.getId() : 1;

        double discount = 0.0;
        String notes = txtOrderNotes.getText();

        Order order = new Order(orderNumber, staffId, currentOrderType, subtotal, discount, subtotal, paymentType, referenceNumber, "New", notes);

        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : orderItems.entrySet()) {
            int id = entry.getKey();
            int qty = entry.getValue();

            if (id > 0) {
                Combo combo = findComboById(id);
                if (combo != null) {
                    items.add(new OrderItem("Combo", id, combo.getName(), qty, combo.getPromoPrice()));
                }
            } else {
                MenuItemModel item = findMenuItemById(-id);
                if (item != null) {
                    items.add(new OrderItem("MenuItem", -id, item.getName(), qty, item.getPrice()));
                }
            }
        }

        int orderId = cashierService.insert(order, items);
        if (orderId > 0) {
            String reserveError = cashierService.reserveIngredientsForOrder(orderId);
            if (reserveError != null) {
                showErrorAlert("Insufficient Stock", reserveError);
                cashierService.releaseReservationsForOrder(orderId);
                cashierService.updateStatus(orderId, "Cancelled");
                return;
            }
orderNumber = cashierService.getNextOrderNumber();
orderItems.clear();
subtotal = 0.0;
txtOrderNotes.clear();
updateOrderNumber();
updateOrderDisplay();
updateTotals();

// Notify other modules (like Cook Panel and Inventory) instantly
OrderNotificationService.broadcastUpdate();
            updateOrderNumber();
            reloadMenu();
        } else {
            showErrorAlert("Order Failed", "Failed to save the order. Please try again.");
        }
    }

    private void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-background-color: #2e2410; -fx-border-color: #4a3820; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");

        javafx.scene.control.Label headerText = (javafx.scene.control.Label) alert.getDialogPane().lookup(".header");
        if (headerText != null) {
            headerText.setStyle("-fx-text-fill: #e07070; -fx-font-size: 14px; -fx-font-weight: bold;");
        }

        javafx.scene.Node contentNode = alert.getDialogPane().lookup(".content");
        if (contentNode != null) {
            contentNode.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px;");
        }

        alert.getDialogPane().getButtonTypes().clear();
        alert.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.OK);

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle("-fx-background-color: #c8500a; -fx-text-fill: #f5ede0; -fx-border-radius: 6; -fx-padding: 8 16 8 16; -fx-font-size: 12px; -fx-font-weight: bold;");

        alert.showAndWait();
    }

    private List<String> checkOutOfStockIngredients() {
        List<String> outOfStockItems = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : orderItems.entrySet()) {
            int id = entry.getKey();
            int orderQty = entry.getValue();

            if (id > 0) {
                Combo combo = findComboById(id);
                if (combo != null) {
                    List<String> comboOutItems = checkComboIngredients(combo.getIncludes(), orderQty);
                    for (String item : comboOutItems) {
                        if (!outOfStockItems.contains(item)) {
                            outOfStockItems.add(item);
                        }
                    }
                }
            } else {
                MenuItemModel item = findMenuItemById(-id);
                if (item != null) {
                    List<MenuItemDAO.MenuItemIngredient> ingredients = cashierService.getIngredientsForMenuItem(item.getId());
                    for (MenuItemDAO.MenuItemIngredient ing : ingredients) {
                        int ingId = cashierService.findIdByName(ing.getIngredientName());
                        if (ingId > 0) {
                            double availableStock = cashierService.getAvailableStock(ingId);
                            double needed = ing.getQuantity() * orderQty;
                            if (availableStock < needed) {
                                String display = item.getName() + " - " + ing.getIngredientName() +
                                    " (need " + String.format("%.1f", needed) + ", available " + String.format("%.1f", availableStock) + ")";
                                if (!outOfStockItems.contains(display)) {
                                    outOfStockItems.add(display);
                                }
                            }
                        }
                    }
                }
            }
        }

        return outOfStockItems;
    }

    private List<String> checkComboIngredients(String includes, int orderQty) {
        List<String> outOfStockItems = new ArrayList<>();
        if (includes == null || includes.isEmpty()) return outOfStockItems;

        String[] items = includes.split(",");
        for (String itemName : items) {
            itemName = itemName.trim();
            List<MenuItemDAO.MenuItemIngredient> ingredients = cashierService.getIngredientsForMenuItemByName(itemName);
            for (MenuItemDAO.MenuItemIngredient ing : ingredients) {
                int ingId = cashierService.findIdByName(ing.getIngredientName());
                if (ingId > 0) {
                    double availableStock = cashierService.getAvailableStock(ingId);
                    double needed = ing.getQuantity() * orderQty;
                    if (availableStock < needed) {
                        String display = itemName + " - " + ing.getIngredientName() +
                            " (need " + String.format("%.1f", needed) + ", available " + String.format("%.1f", availableStock) + ")";
                        if (!outOfStockItems.contains(display)) {
                            outOfStockItems.add(display);
                        }
                    }
                }
            }
        }

        return outOfStockItems;
    }

    private void showOutOfStockWarning(List<String> outOfStockItems) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Out of Stock Warning");
        alert.setHeaderText("Some ingredients are out of stock");

        StringBuilder content = new StringBuilder("The following items cannot be prepared:\n\n");
        for (String item : outOfStockItems) {
            content.append("• ").append(item).append("\n");
        }
        content.append("\nPlease remove these items from the order or notify the kitchen.");
        alert.setContentText(content.toString());

        alert.getDialogPane().setStyle(
            "-fx-background-color: #2e2410; " +
            "-fx-border-color: #e07070; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 12; " +
            "-fx-background-radius: 12;"
        );

        javafx.scene.control.Label headerText = (javafx.scene.control.Label) alert.getDialogPane().lookup(".header");
        if (headerText != null) {
            headerText.setStyle("-fx-text-fill: #e07070; -fx-font-size: 16px; -fx-font-weight: bold;");
        }

        javafx.scene.Node contentNode = alert.getDialogPane().lookup(".content");
        if (contentNode != null) {
            contentNode.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px;");
        }

        alert.getDialogPane().getButtonTypes().clear();
        alert.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.OK);

        javafx.scene.control.Button okButton = (javafx.scene.control.Button) alert.getDialogPane().lookupButton(javafx.scene.control.ButtonType.OK);
        okButton.setStyle(
            "-fx-background-color: #c8500a; " +
            "-fx-text-fill: #f5ede0; " +
            "-fx-border-radius: 6; " +
            "-fx-padding: 8 24 8 24; " +
            "-fx-font-size: 13px; " +
            "-fx-font-weight: bold;"
        );

        alert.showAndWait();
    }

    private String showGCashDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gcash-dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("GCash Payment");
            dialogStage.initOwner(menuGrid.getScene().getWindow());
            dialogStage.setResizable(false);

            Scene scene = new Scene(root, 400, 300);
            dialogStage.setScene(scene);

            GCashDialogController controller = loader.getController();
            dialogStage.showAndWait();

            if (controller.isConfirmed()) {
                return controller.getReferenceNumber();
            }
        } catch (Exception e) {
            System.err.println("Error showing GCash dialog: " + e.getMessage());
        }
        return null;
    }

    public void setActiveNav(String navName) {
    }

    @FXML
    private void onLogout() {
        try {
            Main.clearSession();
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}