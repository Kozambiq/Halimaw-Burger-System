package com.myapp.halimawburgersystem;

import com.myapp.dao.MenuItemDAO;
import com.myapp.model.MenuItemModel;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

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
    private Map<Integer, Integer> orderItems = new HashMap<>();
    private Map<Integer, Double> itemPrices = new HashMap<>();
    private List<MenuItemModel> allMenuItems;
    private String currentOrderType = "Dine-in";
    private int orderNumber = 1;
    private double subtotal = 0.0;

    @FXML
    public void initialize() {
        loadMenuItems();
        updateOrderNumber();
        updateTotals();
        setActiveToggle(btnDineIn);
    }

    private void loadMenuItems() {
        try {
            allMenuItems = menuItemDAO.findAllWithIngredientStatus();
            menuGrid.getChildren().clear();

            for (MenuItemModel item : allMenuItems) {
                itemPrices.put(item.getId(), item.getPrice());
                VBox card = createMenuCard(item);
                menuGrid.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("Error loading menu items: " + e.getMessage());
        }
    }

    private VBox createMenuCard(MenuItemModel item) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(12));
        card.setPrefSize(130, 100);
        card.getStyleClass().add("menu-card");
        card.setBackground(new Background(new BackgroundFill(Color.web("#332615"), CornerRadii.EMPTY, Insets.EMPTY)));

        Label nameLabel = new Label(item.getName());
        nameLabel.getStyleClass().add("menu-card-name");

        Label priceLabel = new Label("₱" + item.getFormattedPrice().replace("₱", ""));
        priceLabel.getStyleClass().add("menu-card-price");

        Label catLabel = new Label(item.getCategory());
        catLabel.getStyleClass().add("menu-card-cat");

        card.getChildren().addAll(nameLabel, priceLabel, catLabel);

        card.setOnMouseClicked((MouseEvent event) -> {
            addToOrder(item);
        });

        return card;
    }

    private void addToOrder(MenuItemModel item) {
        int itemId = item.getId();
        int quantity = orderItems.getOrDefault(itemId, 0) + 1;
        orderItems.put(itemId, quantity);

        subtotal += item.getPrice();
        updateOrderDisplay();
        updateTotals();
    }

    private void updateOrderDisplay() {
        orderItemsList.getChildren().clear();

        for (Map.Entry<Integer, Integer> entry : orderItems.entrySet()) {
            int itemId = entry.getKey();
            int qty = entry.getValue();

            MenuItemModel item = findMenuItemById(itemId);
            if (item == null || qty <= 0) continue;

            HBox row = new HBox(8);
            row.getStyleClass().add("order-item-row");
            row.setPadding(new Insets(8, 0, 8, 0));

            Label nameLabel = new Label(item.getName());
            nameLabel.getStyleClass().add("order-item-name");
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

            HBox qtyCtrl = new HBox(6);
            qtyCtrl.getStyleClass().add("qty-ctrl");

            Button minusBtn = new Button("-");
            minusBtn.getStyleClass().add("qty-btn");
            minusBtn.setOnAction(e -> updateItemQuantity(item, qty - 1));

            Label qtyLabel = new Label(String.valueOf(qty));
            qtyLabel.getStyleClass().add("qty-val");

            Button plusBtn = new Button("+");
            plusBtn.getStyleClass().add("qty-btn");
            plusBtn.setOnAction(e -> updateItemQuantity(item, qty + 1));

            qtyCtrl.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

            double itemTotal = item.getPrice() * qty;
            Label totalLabel = new Label("₱" + String.format("%.2f", itemTotal));
            totalLabel.getStyleClass().add("order-item-total");

            row.getChildren().addAll(nameLabel, qtyCtrl, totalLabel);
            orderItemsList.getChildren().add(row);
        }
    }

    private MenuItemModel findMenuItemById(int id) {
        for (MenuItemModel item : allMenuItems) {
            if (item.getId() == id) return item;
        }
        return null;
    }

    private void updateItemQuantity(MenuItemModel item, int newQty) {
        int itemId = item.getId();
        int oldQty = orderItems.getOrDefault(itemId, 0);

        if (newQty <= 0) {
            orderItems.remove(itemId);
            subtotal -= item.getPrice() * oldQty;
        } else {
            orderItems.put(itemId, newQty);
            subtotal += item.getPrice() * (newQty - oldQty);
        }

        updateOrderDisplay();
        updateTotals();
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