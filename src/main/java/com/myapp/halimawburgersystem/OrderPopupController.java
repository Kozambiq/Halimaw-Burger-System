package com.myapp.halimawburgersystem;

import com.myapp.model.Order;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class OrderPopupController {

    @FXML private Label popupOrderNum;
    @FXML private Label popupDate;
    @FXML private VBox popupItemsContainer;
    @FXML private VBox popupNotesBox;
    @FXML private Label popupNotes;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOrderData(Order order) {
        popupOrderNum.setText("#" + String.format("%04d", order.getOrderNumber()));
        popupDate.setText(order.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy · h:mm a")));
        
        com.myapp.dao.OrderDAO orderDAO = new com.myapp.dao.OrderDAO();
        java.util.List<com.myapp.model.OrderItem> items = orderDAO.findItemsByOrderId(order.getId());
        
        for (com.myapp.model.OrderItem item : items) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(12);
            
            Label qty = new Label(item.getQuantity() + "x");
            qty.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #d4591e; -fx-font-weight: bold; -fx-min-width: 30;");
            
            Label name = new Label(item.getItemName());
            name.setStyle("-fx-text-fill: #f5ede0; -fx-font-size: 13px;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Label price = new Label("₱" + String.format("%.2f", item.getTotalPrice()));
            price.setStyle("-fx-font-family: 'DM Mono', monospace; -fx-text-fill: #c4a882;");
            
            row.getChildren().addAll(qty, name, spacer, price);
            popupItemsContainer.getChildren().add(row);
        }
        
        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            popupNotesBox.setVisible(true);
            popupNotesBox.setManaged(true);
            popupNotes.setText(order.getNotes());
        } else {
            popupNotesBox.setVisible(false);
            popupNotesBox.setManaged(false);
        }
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }
}