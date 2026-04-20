package com.myapp.dao;

import com.myapp.model.Ingredient;
import com.myapp.model.MenuItemModel;
import com.myapp.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuItemDAO {

    private static final String STATUS_OK = "Available";
    private static final String STATUS_LOW = "Low Stock";
    private static final String STATUS_OUT = "Out of Stock";

    public static class MenuItemIngredient {
        private int ingredientId;
        private String ingredientName;
        private String unit;
        private double quantity;

        public MenuItemIngredient(int ingredientId, String ingredientName, String unit, double quantity) {
            this.ingredientId = ingredientId;
            this.ingredientName = ingredientName;
            this.unit = unit;
            this.quantity = quantity;
        }

        public int getIngredientId() { return ingredientId; }
        public String getIngredientName() { return ingredientName; }
        public String getUnit() { return unit; }
        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }
    }

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

    public MenuItemModel findById(int id) {
        String sql = "SELECT id, name, category, price FROM menu_items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new MenuItemModel(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getDouble("price"),
                        "Available"
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding menu item: " + e.getMessage());
        }
        return null;
    }

    public List<MenuItemIngredient> getIngredientsForMenuItem(int menuItemId) {
        List<MenuItemIngredient> ingredients = new ArrayList<>();
        String sql = "SELECT i.id, i.name, i.unit, mmi.quantity_used " +
                     "FROM menu_item_ingredients mmi " +
                     "JOIN ingredients i ON mmi.ingredient_id = i.id " +
                     "WHERE mmi.menu_item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, menuItemId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new MenuItemIngredient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getDouble("quantity_used")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting ingredients: " + e.getMessage());
        }
        return ingredients;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM menu_items ORDER BY category";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting categories: " + e.getMessage());
        }
        return categories;
    }

    public List<Ingredient> searchIngredients(String query) {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT id, name, unit FROM ingredients WHERE LOWER(name) LIKE LOWER(?) ORDER BY name LIMIT 10";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new Ingredient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("unit"),
                        0, 0, 0
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching ingredients: " + e.getMessage());
        }
        return ingredients;
    }

    public boolean ingredientExistsByName(String name) {
        String sql = "SELECT COUNT(*) FROM ingredients WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking ingredient: " + e.getMessage());
        }
        return false;
    }

    public String getIngredientUnit(String name) {
        String sql = "SELECT unit FROM ingredients WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("unit");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting ingredient unit: " + e.getMessage());
        }
        return null;
    }

    public boolean updateMenuItem(int id, String name, String category, double price) {
        String sql = "UPDATE menu_items SET name = ?, category = ?, price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setDouble(3, price);
            stmt.setInt(4, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating menu item: " + e.getMessage());
        }
        return false;
    }

    public boolean updateMenuItemIngredients(int menuItemId, List<MenuItemIngredient> ingredients) {
        String deleteSql = "DELETE FROM menu_item_ingredients WHERE menu_item_id = ?";
        String insertSql = "INSERT INTO menu_item_ingredients (menu_item_id, ingredient_id, quantity_used) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, menuItemId);
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (MenuItemIngredient ing : ingredients) {
                    insertStmt.setInt(1, menuItemId);
                    insertStmt.setInt(2, ing.getIngredientId());
                    insertStmt.setDouble(3, ing.getQuantity());
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating menu item ingredients: " + e.getMessage());
        }
        return false;
    }
}