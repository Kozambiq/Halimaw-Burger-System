package com.myapp.service;

import com.myapp.dao.ComboDAO;
import com.myapp.dao.IngredientDAO;
import com.myapp.dao.MenuItemDAO;
import com.myapp.dao.OrderDAO;
import com.myapp.model.Combo;
import com.myapp.model.Ingredient;
import com.myapp.model.MenuItemModel;
import com.myapp.model.Order;
import com.myapp.model.OrderItem;
import java.util.List;

public class CashierService {

    private MenuItemDAO menuItemDAO = new MenuItemDAO();
    private ComboDAO comboDAO = new ComboDAO();
    private OrderDAO orderDAO = new OrderDAO();
    private IngredientDAO ingredientDAO = new IngredientDAO();

    public void syncAvailabilityToDatabase() {
        menuItemDAO.syncAvailabilityToDatabase();
    }

    public List<MenuItemModel> findAllMenuItemsWithIngredientStatus() {
        return menuItemDAO.findAllWithIngredientStatus();
    }

    public List<MenuItemModel> findAllWithIngredientStatus() {
        return menuItemDAO.findAllWithIngredientStatus();
    }

    public List<Combo> findAll() {
        return comboDAO.findAll();
    }

    public int getNextOrderNumber() {
        return orderDAO.getNextOrderNumber();
    }

    public int insert(Order order, List<OrderItem> items) {
        return orderDAO.insert(order, items);
    }

    public String reserveIngredientsForOrder(int orderId) {
        return orderDAO.reserveIngredientsForOrder(orderId);
    }

    public void releaseReservationsForOrder(int orderId) {
        orderDAO.releaseReservationsForOrder(orderId);
    }

    public boolean updateStatus(int orderId, String status) {
        return orderDAO.updateStatus(orderId, status);
    }

    public List<MenuItemDAO.MenuItemIngredient> getIngredientsForMenuItem(int itemId) {
        return menuItemDAO.getIngredientsForMenuItem(itemId);
    }

    public int findIdByName(String name) {
        return ingredientDAO.findIdByName(name);
    }

    public double getAvailableStock(int ingredientId) {
        return ingredientDAO.getAvailableStock(ingredientId);
    }

    public List<MenuItemDAO.MenuItemIngredient> getIngredientsForMenuItemByName(String itemName) {
        return menuItemDAO.getIngredientsForMenuItemByName(itemName);
    }
}