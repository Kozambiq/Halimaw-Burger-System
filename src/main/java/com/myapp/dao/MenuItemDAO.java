package com.myapp.dao;

import com.myapp.model.MenuItemModel;
import com.myapp.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {

    private static final String STATUS_OK = "Available";
    private static final String STATUS_LOW = "Low Stock";
    private static final String STATUS_OUT = "Out of Stock";

    public List<MenuItemModel> findAllWithIngredientStatus() {
        List<MenuItemModel> menuItems = new ArrayList<>();
        String sql = "SELECT mi.id, mi.name, mi.category, mi.price " +
                     "FROM menu_items mi ORDER BY mi.name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int menuItemId = rs.getInt("id");
                String availability = calculateAvailability(conn, menuItemId);

                MenuItemModel item = new MenuItemModel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    availability
                );
                menuItems.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching menu items: " + e.getMessage());
        }
        return menuItems;
    }

    private String calculateAvailability(Connection conn, int menuItemId) {
        String sql = "SELECT i.quantity, i.min_threshold, i.max_stock " +
                     "FROM menu_item_ingredients mmi " +
                     "JOIN ingredients i ON mmi.ingredient_id = i.id " +
                     "WHERE mmi.menu_item_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, menuItemId);
            ResultSet rs = stmt.executeQuery();

            String worstStatus = STATUS_OK;
            boolean hasIngredients = false;

            while (rs.next()) {
                hasIngredients = true;
                double quantity = rs.getDouble("quantity");
                double minThreshold = rs.getDouble("min_threshold");

                double criticalThreshold = minThreshold * 0.5;

                if (quantity <= 0 || quantity <= criticalThreshold) {
                    return STATUS_OUT;
                } else if (quantity <= minThreshold) {
                    worstStatus = STATUS_LOW;
                }
            }

            if (!hasIngredients) {
                return STATUS_OK;
            }

            return worstStatus;
        } catch (SQLException e) {
            System.err.println("Error calculating availability: " + e.getMessage());
            return STATUS_OK;
        }
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM menu_items WHERE availability != 'Hidden'";
        return countQuery(sql);
    }

    public int getAvailableCount() {
        String sql = "SELECT COUNT(*) FROM menu_items WHERE availability = 'Available'";
        return countQuery(sql);
    }

    public int getLowStockCount() {
        String sql = "SELECT COUNT(*) FROM menu_items WHERE availability = 'Low Stock'";
        return countQuery(sql);
    }

    public int getOutOfStockCount() {
        String sql = "SELECT COUNT(*) FROM menu_items WHERE availability = 'Out of Stock'";
        return countQuery(sql);
    }

    private int countQuery(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting: " + e.getMessage());
        }
        return 0;
    }
}