package com.myapp.dao;

import com.myapp.model.Combo;
import com.myapp.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComboDAO {

    public List<Combo> findAll() {
        List<Combo> combos = new ArrayList<>();
        String sql = "SELECT id, name, includes, promo_price, original_price, valid_until, status FROM combos ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                combos.add(new Combo(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("includes"),
                    rs.getDouble("promo_price"),
                    rs.getDouble("original_price"),
                    rs.getDate("valid_until") != null ? rs.getDate("valid_until").toLocalDate() : null,
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading combos: " + e.getMessage());
        }

        return combos;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM combos";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting combos: " + e.getMessage());
        }
        return 0;
    }

    public int getActiveCount() {
        String sql = "SELECT COUNT(*) FROM combos WHERE status = 'Active'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting active combos: " + e.getMessage());
        }
        return 0;
    }

    public boolean insert(String name, String includes, double promoPrice, double originalPrice, Date validUntil) {
        String sql = "INSERT INTO combos (name, includes, promo_price, original_price, valid_until) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, includes);
            stmt.setDouble(3, promoPrice);
            stmt.setDouble(4, originalPrice);
            stmt.setDate(5, validUntil);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting combo: " + e.getMessage());
        }
        return false;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE combos SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating combo status: " + e.getMessage());
        }
        return false;
    }

    public boolean update(int id, String name, String includes, double promoPrice, double originalPrice, Date validUntil) {
        String sql = "UPDATE combos SET name = ?, includes = ?, promo_price = ?, original_price = ?, valid_until = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, includes);
            stmt.setDouble(3, promoPrice);
            stmt.setDouble(4, originalPrice);
            stmt.setDate(5, validUntil);
            stmt.setInt(6, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating combo: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM combos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting combo: " + e.getMessage());
        }
        return false;
    }

    public List<String> searchMenuItems(String query) {
        List<String> items = new ArrayList<>();
        String sql = "SELECT name FROM menu_items WHERE LOWER(name) LIKE LOWER(?) AND availability != 'Unavailable' ORDER BY name LIMIT 10";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching menu items: " + e.getMessage());
        }
        return items;
    }

    public double getMenuItemPrice(String name) {
        String sql = "SELECT price FROM menu_items WHERE name = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("price");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting menu item price: " + e.getMessage());
        }
        return 0.0;
    }
}