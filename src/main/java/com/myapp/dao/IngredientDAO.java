package com.myapp.dao;

import com.myapp.model.Ingredient;
import com.myapp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IngredientDAO {

    public List<Ingredient> findAll() {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT id, name, unit, quantity, min_threshold, max_stock, status, reserved FROM ingredients ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Ingredient ing = new Ingredient(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("unit"),
                    rs.getDouble("quantity"),
                    rs.getDouble("min_threshold"),
                    rs.getDouble("max_stock"),
                    rs.getString("status")
                );
                ing.setReserved(rs.getDouble("reserved"));
                ingredients.add(ing);
            }
        } catch (SQLException e) {
            System.err.println("Error loading ingredients: " + e.getMessage());
        }

        return ingredients;
    }

    public boolean updateAvailabilityStatus(int id, String status) {
        String sql = "UPDATE ingredients SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating availability status: " + e.getMessage());
        }
        return false;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM ingredients";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting ingredients: " + e.getMessage());
        }
        return 0;
    }

    public int getLowStockCount() {
        String sql = "SELECT COUNT(*) FROM ingredients WHERE quantity > 0 AND quantity <= min_threshold";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting low stock: " + e.getMessage());
        }
        return 0;
    }

    public int getOutOfStockCount() {
        String sql = "SELECT COUNT(*) FROM ingredients WHERE quantity <= 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting out of stock: " + e.getMessage());
        }
        return 0;
    }

    public boolean updateQuantity(int id, double quantity) {
        String sql = "UPDATE ingredients SET quantity = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, quantity);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating quantity: " + e.getMessage());
        }
        return false;
    }

    public boolean addRestock(int ingredientId, double quantityAdded, int staffId) {
        String updateSql = "UPDATE ingredients SET quantity = quantity + ? WHERE id = ?";
        String logSql = "INSERT INTO restock_logs (ingredient_id, quantity_added, restocked_by) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                 PreparedStatement logStmt = conn.prepareStatement(logSql)) {

                updateStmt.setDouble(1, quantityAdded);
                updateStmt.setInt(2, ingredientId);
                updateStmt.executeUpdate();

                logStmt.setInt(1, ingredientId);
                logStmt.setDouble(2, quantityAdded);
                logStmt.setInt(3, staffId);
                logStmt.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error in restock: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error with connection: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM ingredients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting ingredient: " + e.getMessage());
        }
        return false;
    }

    public boolean updateThreshold(int id, double minThreshold) {
        String sql = "UPDATE ingredients SET min_threshold = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, minThreshold);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating threshold: " + e.getMessage());
        }
        return false;
    }

    public boolean updateMaxStock(int id, double maxStock) {
        String sql = "UPDATE ingredients SET max_stock = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, maxStock);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating max_stock: " + e.getMessage());
        }
        return false;
    }

    public boolean existsByName(String name) {
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

    public List<String> searchByName(String query) {
        List<String> items = new ArrayList<>();
        String sql = "SELECT name FROM ingredients WHERE LOWER(name) LIKE LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching ingredients: " + e.getMessage());
        }
        return items;
    }

    public List<Ingredient> findByName(String name) {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT id, name, unit, quantity, min_threshold, max_stock, status FROM ingredients WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ingredients.add(new Ingredient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getDouble("quantity"),
                        rs.getDouble("min_threshold"),
                        rs.getDouble("max_stock"),
                        rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding ingredient by name: " + e.getMessage());
        }
        return ingredients;
    }

    public boolean insert(String name, String unit, double quantity, double minThreshold, double maxStock) {
        String sql = "INSERT INTO ingredients (name, unit, quantity, min_threshold, max_stock) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, unit);
            stmt.setDouble(3, quantity);
            stmt.setDouble(4, minThreshold);
            stmt.setDouble(5, maxStock);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting ingredient: " + e.getMessage());
        }
        return false;
    }

    public boolean canDeduct(int ingredientId, double quantityNeeded) {
        String sql = "SELECT quantity, COALESCE(reserved, 0) as reserved FROM ingredients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ingredientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double currentQty = rs.getDouble("quantity");
                    double reserved = rs.getDouble("reserved");
                    return (currentQty - reserved - quantityNeeded) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking ingredient stock: " + e.getMessage());
        }
        return false;
    }

    public boolean wouldGoBelowThreshold(int ingredientId, double quantityNeeded) {
        String sql = "SELECT quantity, min_threshold FROM ingredients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ingredientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double currentQty = rs.getDouble("quantity");
                    double minThreshold = rs.getDouble("min_threshold");
                    return (currentQty - quantityNeeded) < minThreshold;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking threshold: " + e.getMessage());
        }
        return false;
    }

    public boolean deduct(int ingredientId, double quantityToDeduct) {
        String sql = "UPDATE ingredients SET quantity = quantity - ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, quantityToDeduct);
            stmt.setInt(2, ingredientId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deducting ingredient: " + e.getMessage());
        }
        return false;
    }

    public boolean reserve(int ingredientId, double quantityToReserve) {
        String sql = "UPDATE ingredients SET reserved = reserved + ? WHERE id = ? AND (quantity - reserved) >= ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, quantityToReserve);
            stmt.setInt(2, ingredientId);
            stmt.setDouble(3, quantityToReserve);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error reserving ingredient: " + e.getMessage());
        }
        return false;
    }

    public boolean releaseReservation(int ingredientId, double quantityToRelease) {
        String sql = "UPDATE ingredients SET reserved = GREATEST(0, reserved - ?) WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, quantityToRelease);
            stmt.setInt(2, ingredientId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error releasing reservation: " + e.getMessage());
        }
        return false;
    }

    public double getAvailableStock(int ingredientId) {
        String sql = "SELECT quantity - COALESCE(reserved, 0) AS available FROM ingredients WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ingredientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("available");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting available stock: " + e.getMessage());
        }
        return 0;
    }

    public int findIdByName(String name) {
        String sql = "SELECT id FROM ingredients WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding ingredient by name: " + e.getMessage());
        }
        return -1;
    }

    public List<Ingredient> findLowStock() {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT id, name, unit, quantity, min_threshold, max_stock, status FROM ingredients WHERE quantity > 0 AND quantity <= min_threshold ORDER BY (quantity / min_threshold) ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ingredients.add(new Ingredient(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("unit"),
                    rs.getDouble("quantity"),
                    rs.getDouble("min_threshold"),
                    rs.getDouble("max_stock"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading low stock ingredients: " + e.getMessage());
        }
        return ingredients;
    }

    public List<Ingredient> findCriticalStock() {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT id, name, unit, quantity, min_threshold, max_stock, status FROM ingredients WHERE quantity <= 0 ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ingredients.add(new Ingredient(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("unit"),
                    rs.getDouble("quantity"),
                    rs.getDouble("min_threshold"),
                    rs.getDouble("max_stock"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading critical stock ingredients: " + e.getMessage());
        }
        return ingredients;
    }
}