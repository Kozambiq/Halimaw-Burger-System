package com.myapp.service;

import com.myapp.dao.MenuItemDAO;
import com.myapp.dao.MenuItemDAO.MenuItemIngredient;
import com.myapp.dao.MenuItemDAO.TopSellingItem;
import com.myapp.model.Ingredient;
import com.myapp.model.MenuItemModel;
import java.util.List;

public class MenuItemService {

    private MenuItemDAO menuItemDAO = new MenuItemDAO();

    public List<MenuItemModel> findAllWithIngredientStatus() {
        return menuItemDAO.findAllWithIngredientStatus();
    }

    public void syncAvailabilityToDatabase() {
        menuItemDAO.syncAvailabilityToDatabase();
    }

    public int getTotalCount() {
        return menuItemDAO.getTotalCount();
    }

    public int getAvailableCount() {
        return menuItemDAO.getAvailableCount();
    }

    public int getLowStockCount() {
        return menuItemDAO.getLowStockCount();
    }

    public int getOutOfStockCount() {
        return menuItemDAO.getOutOfStockCount();
    }

    public MenuItemModel findById(int id) {
        return menuItemDAO.findById(id);
    }

    public List<MenuItemIngredient> getIngredientsForMenuItem(int menuItemId) {
        return menuItemDAO.getIngredientsForMenuItem(menuItemId);
    }

    public List<MenuItemIngredient> getIngredientsForMenuItemByName(String menuItemName) {
        return menuItemDAO.getIngredientsForMenuItemByName(menuItemName);
    }

    public List<String> getAllCategories() {
        return menuItemDAO.getAllCategories();
    }

    public List<Ingredient> searchIngredients(String query) {
        return menuItemDAO.searchIngredients(query);
    }

    public boolean ingredientExistsByName(String name) {
        return menuItemDAO.ingredientExistsByName(name);
    }

    public String getIngredientUnit(String name) {
        return menuItemDAO.getIngredientUnit(name);
    }

    public boolean updateMenuItem(int id, String name, String category, double price) {
        return menuItemDAO.updateMenuItem(id, name, category, price);
    }

    public boolean insert(String name, String category, double price) {
        return menuItemDAO.insert(name, category, price);
    }

    public boolean existsByName(String name) {
        return menuItemDAO.existsByName(name);
    }

    public int insertAndGetId(String name, String category, double price) {
        return menuItemDAO.insertAndGetId(name, category, price);
    }

    public boolean updateMenuItemIngredients(int menuItemId, List<MenuItemIngredient> ingredients) {
        return menuItemDAO.updateMenuItemIngredients(menuItemId, ingredients);
    }

    public boolean updateAvailability(int id, String availability) {
        return menuItemDAO.updateAvailability(id, availability);
    }

    public List<String> searchByName(String query) {
        return menuItemDAO.searchByName(query);
    }

    public List<MenuItemModel> findByName(String name) {
        return menuItemDAO.findByName(name);
    }

    public List<TopSellingItem> getTopSellingItems(int limit) {
        return menuItemDAO.getTopSellingItems(limit);
    }
}