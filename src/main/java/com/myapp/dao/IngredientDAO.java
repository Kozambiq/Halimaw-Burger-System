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
        String sql = "SELECT id, name, unit, quantity, min_threshold FROM ingredients ORDER BY name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ingredients.add(new Ingredient(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("unit"),
                    rs.getDouble("quantity"),
                    rs.getDouble("min_threshold")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading ingredients: " + e.getMessage());
        }

        return ingredients;
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
}