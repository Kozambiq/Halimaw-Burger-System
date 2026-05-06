package com.myapp.service;

import com.myapp.dao.IngredientDAO;
import com.myapp.model.Ingredient;
import com.myapp.model.RestockLog;
import java.util.List;

public class InventoryService {

    private IngredientDAO ingredientDAO = new IngredientDAO();

    public List<Ingredient> findAll() {
        return ingredientDAO.findAll();
    }

    public List<RestockLog> findAllRestockLogs() {
        return ingredientDAO.findAllRestockLogs();
    }

    public List<Ingredient> findByName(String name) {
        return ingredientDAO.findByName(name);
    }

    public List<String> searchByName(String query) {
        return ingredientDAO.searchByName(query);
    }

    public boolean updateAvailabilityStatus(int id, String status) {
        return ingredientDAO.updateAvailabilityStatus(id, status);
    }

    public int getTotalCount() {
        return ingredientDAO.getTotalCount();
    }

    public int getLowStockCount() {
        return ingredientDAO.getLowStockCount();
    }

    public int getOutOfStockCount() {
        return ingredientDAO.getOutOfStockCount();
    }

    public boolean updateQuantity(int id, double quantity) {
        return ingredientDAO.updateQuantity(id, quantity);
    }

    public boolean updateThreshold(int id, double minThreshold) {
        return ingredientDAO.updateThreshold(id, minThreshold);
    }

    public boolean updateMaxStock(int id, double maxStock) {
        return ingredientDAO.updateMaxStock(id, maxStock);
    }

    public boolean existsByName(String name) {
        return ingredientDAO.existsByName(name);
    }

    public boolean insert(String name, String unit, double quantity, double minThreshold, double maxStock) {
        return ingredientDAO.insert(name, unit, quantity, minThreshold, maxStock);
    }

    public boolean logTransaction(int ingredientId, double quantityChange, int staffId, String notes) {
        return ingredientDAO.logTransaction(ingredientId, quantityChange, staffId, notes);
    }

    public boolean addRestock(int ingredientId, double quantityAdded, int staffId) {
        return ingredientDAO.addRestock(ingredientId, quantityAdded, staffId);
    }

    public boolean delete(int id) {
        return ingredientDAO.delete(id);
    }

    public double getAvailableStock(int ingredientId) {
        return ingredientDAO.getAvailableStock(ingredientId);
    }

    public List<Ingredient> findLowStock() {
        return ingredientDAO.findLowStock();
    }

    public List<Ingredient> findCriticalStock() {
        return ingredientDAO.findCriticalStock();
    }

    public boolean canDeduct(int ingredientId, double quantityNeeded) {
        return ingredientDAO.canDeduct(ingredientId, quantityNeeded);
    }

    public boolean deduct(int ingredientId, double quantityToDeduct) {
        return ingredientDAO.deduct(ingredientId, quantityToDeduct);
    }

    public int findIdByName(String name) {
        return ingredientDAO.findIdByName(name);
    }
}