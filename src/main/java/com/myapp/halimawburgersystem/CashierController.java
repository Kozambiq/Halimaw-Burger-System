package com.myapp.halimawburgersystem;

import com.myapp.dao.ComboDAO;
import com.myapp.dao.MenuItemDAO;
import com.myapp.model.Combo;
import com.myapp.model.MenuItemModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CashierController {

    @FXML private GridPane menuGrid;
    @FXML private VBox orderItemsList;
    @FXML private TextField txtOrderNotes;
    @FXML private Label lblOrderNum;
    @FXML private Label lblSubtotal;
    @FXML private Label lblDiscount;
    @FXML private Label lblTotal;
    @FXML private Button btnDineIn;
    @FXML private Button btnTakeout;

    private MenuItemDAO menuItemDAO = new MenuItemDAO();
    private ComboDAO comboDAO = new ComboDAO();
    private Map<Integer, Integer> orderItems = new HashMap<>();
    private Map<Integer, Double> itemPrices = new HashMap<>();
    private Map<Integer, Double> comboPrices = new HashMap<>();
    private List<MenuItemModel> allMenuItems;
    private List<Combo> allCombos;
    private String currentOrderType = "Dine-in";
    private int orderNumber = 1;
    private double subtotal = 0.0;
    private static final int CARD_WIDTH = 150;
    private static final int CARD_HGAP = 10;
    private static final int RIGHT_PANEL_WIDTH = 360;
    private static final int GRID_PADDING = 32;

    @FXML
    public void initialize() {
        updateOrderNumber();
        updateTotals();
        setActiveToggle(btnDineIn);

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

    private int calculateColumns() {
        double availableWidth = 0;
        if (menuGrid.getParent() != null) {
            availableWidth = menuGrid.getParent().getLayoutBounds().getWidth() - GRID_PADDING;
        }
        if (availableWidth <= 0) return 4;
        int columns = (int) (availableWidth / (CARD_WIDTH + CARD_HGAP));
        return Math.max(columns, 1);
    }

    private void reloadMenu() {
        try {
            menuGrid.getChildren().clear();
            loadMenuItems();
            loadCombos();
        } catch (Exception e) {
            System.err.println("Error reloading menu: " + e.getMessage());
        }
    }

    private void loadMenuItems() {
        try {
            allMenuItems = menuItemDAO.findAllWithIngredientStatus();
            menuGrid.getChildren().clear();
            menuGrid.getColumnConstraints().clear();

            int columns = calculateColumns();
            for (int i = 0; i < columns; i++) {
                ColumnConstraints col = new ColumnConstraints();
                col.setPrefWidth(CARD_WIDTH);
                col.setMinWidth(CARD_WIDTH);
                col.setMaxWidth(CARD_WIDTH);
                menuGrid.getColumnConstraints().add(col);
            }

            Map<String, List<MenuItemModel>> byCategory = new HashMap<>();
            for (MenuItemModel item : allMenuItems) {
                byCategory.computeIfAbsent(item.getCategory(), k -> new ArrayList<>()).add(item);
            }

            int row = 0;
            for (String category : byCategory.keySet()) {
                row = addCategorySection(category, byCategory.get(category), columns, row);
            }
        } catch (Exception e) {
            System.err.println("Error loading menu items: " + e.getMessage());
        }
    }

    private int addCategorySection(String category, List<MenuItemModel> items, int columns, int startRow) {
        int row = startRow;

        Label dividerLabel = new Label(category.toUpperCase() + "  ─────────────────");
        dividerLabel.getStyleClass().add("menu-divider-label");
        GridPane.setConstraints(dividerLabel, 0, row, columns, 1);
        dividerLabel.setMaxWidth(Double.MAX_VALUE);
        GridPane.setMargin(dividerLabel, new Insets(16, 0, 8, 0));
        menuGrid.getChildren().add(dividerLabel);
        row++;

        int col = 0;
        for (MenuItemModel item : items) {
            itemPrices.put(-item.getId(), item.getPrice());
            StackPane card = createMenuCard(item, false);
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

    private void loadCombos() {
        try {
            allCombos = comboDAO.findAll();

            if (allCombos.isEmpty()) return;

            int columns = calculateColumns();
            int existingItems = menuGrid.getChildren().size();
            int startRow = (existingItems + columns - 1) / columns;

            Label dividerLabel = new Label("PROMOS & COMBOS  ─────────────────");
            dividerLabel.getStyleClass().add("menu-divider-label");
            GridPane.setConstraints(dividerLabel, 0, startRow, columns, 1);
            dividerLabel.setMaxWidth(Double.MAX_VALUE);
            GridPane.setMargin(dividerLabel, new Insets(16, 0, 8, 0));
            menuGrid.getChildren().add(dividerLabel);
            startRow++;

            int row = startRow;
            int col = 0;

            for (Combo combo : allCombos) {
                if (!"Active".equals(combo.getStatus())) continue;
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
        } catch (Exception e) {
            System.err.println("Error loading combos: " + e.getMessage());
        }
    }

    private StackPane createMenuCard(MenuItemModel item, boolean isCombo) {
        StackPane card = new StackPane();
        card.setPrefSize(150, 110);
        card.setMinSize(150, 110);
        card.setMaxSize(150, 110);
        card.getStyleClass().add("menu-card");

        VBox content = new VBox(4);
        content.setPadding(new Insets(12));

        Label nameLabel = new Label(item.getName());
        nameLabel.getStyleClass().add("menu-card-name");

        Label priceLabel = new Label("₱" + String.format("%.2f", item.getPrice()));
        priceLabel.getStyleClass().add("menu-card-price");

        Label catLabel = new Label(item.getCategory());
        catLabel.getStyleClass().add("menu-card-cat");

        content.getChildren().addAll(nameLabel, priceLabel, catLabel);

        boolean isOutOfStock = "Out of Stock".equals(item.getAvailability());
        if (isOutOfStock) {
            card.getStyleClass().add("menu-card-disabled");

            Label outLabel = new Label("OUT OF STOCK");
            outLabel.getStyleClass().add("menu-out-label");
            StackPane.setAlignment(outLabel, javafx.geometry.Pos.CENTER);
            card.getChildren().addAll(content, outLabel);
        } else {
            card.getChildren().add(content);
        }

        final MenuItemModel clickedItem = item;
        card.setOnMouseClicked((MouseEvent event) -> {
            if (!isOutOfStock) {
                addMenuItemToOrder(clickedItem);
            }
        });

        return card;
    }

    private StackPane createComboCard(Combo combo) {
        StackPane card = new StackPane();
        card.setPrefSize(150, 110);
        card.setMinSize(150, 110);
        card.setMaxSize(150, 110);
        card.getStyleClass().add("menu-card");
        card.getStyleClass().add("menu-card-promo");

        VBox content = new VBox(2);
        content.setPadding(new Insets(8, 12, 8, 12));
        content.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label promoBadge = new Label("PROMO");
        promoBadge.getStyleClass().add("menu-promo-badge");

        Label nameLabel = new Label(combo.getName());
        nameLabel.getStyleClass().add("menu-card-name");

        Label priceLabel = new Label("₱" + String.format("%.2f", combo.getPromoPrice()));
        priceLabel.getStyleClass().add("menu-card-price");

        Label includesLabel = new Label(combo.getIncludes());
        includesLabel.getStyleClass().add("menu-card-cat");
        includesLabel.setWrapText(true);

        content.getChildren().addAll(promoBadge, nameLabel, priceLabel, includesLabel);
        card.getChildren().add(content);

        final Combo clickedCombo = combo;
        card.setOnMouseClicked((MouseEvent event) -> {
            addComboToOrder(clickedCombo);
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
        row.setPadding(new Insets(8, 0, 8, 0));

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("order-item-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

        HBox qtyCtrl = new HBox(6);
        qtyCtrl.getStyleClass().add("qty-ctrl");

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().add("qty-btn");
        final int finalQty = qty;
        final String finalName = name;
        final double finalPrice = price;
        minusBtn.setOnAction(e -> decrementItem(finalName, finalPrice));

        Label qtyLabel = new Label(String.valueOf(qty));
        qtyLabel.getStyleClass().add("qty-val");

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");
        plusBtn.setOnAction(e -> incrementItem(finalName, finalPrice));

        qtyCtrl.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        double itemTotal = price * qty;
        Label totalLabel = new Label("₱" + String.format("%.2f", itemTotal));
        totalLabel.getStyleClass().add("order-item-total");

        row.getChildren().addAll(nameLabel, qtyCtrl, totalLabel);
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

    @FXML
    private void onFilterCategory() {
    }

    @FXML
    private void onClearOrder() {
        orderItems.clear();
        subtotal = 0.0;
        updateOrderDisplay();
        updateTotals();
        orderNumber++;
        updateOrderNumber();
    }

    @FXML
    private void onPayCash() {
    }

    @FXML
    private void onPayCard() {
    }

    public void setActiveNav(String navName) {
    }

    @FXML
    private void onLogout() {
        try {
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}