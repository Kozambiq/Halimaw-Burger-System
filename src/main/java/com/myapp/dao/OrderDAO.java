package com.myapp.dao;

import com.myapp.model.Order;
import com.myapp.model.OrderItem;
import com.myapp.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public int insert(Order order, List<OrderItem> items) {
        String orderSql = "INSERT INTO orders (order_number, staff_id, order_type, subtotal, discount, total, payment_type, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_id, item_type, item_id, item_name, quantity, unit_price, total_price) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setInt(1, order.getOrderNumber());
                orderStmt.setInt(2, order.getStaffId());
                orderStmt.setString(3, order.getOrderType());
                orderStmt.setDouble(4, order.getSubtotal());
                orderStmt.setDouble(5, order.getDiscount());
                orderStmt.setDouble(6, order.getTotal());
                orderStmt.setString(7, order.getPaymentType());
                orderStmt.setString(8, order.getNotes());

                orderStmt.executeUpdate();

                try (ResultSet rs = orderStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int orderId = rs.getInt(1);

                        try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                            for (OrderItem item : items) {
                                itemStmt.setInt(1, orderId);
                                itemStmt.setString(2, item.getItemType());
                                itemStmt.setInt(3, item.getItemId());
                                itemStmt.setString(4, item.getItemName());
                                itemStmt.setInt(5, item.getQuantity());
                                itemStmt.setDouble(6, item.getUnitPrice());
                                itemStmt.setDouble(7, item.getTotalPrice());
                                itemStmt.executeUpdate();
                            }
                        }

                        conn.commit();
                        return orderId;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error inserting order: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error with database connection: " + e.getMessage());
        }
        return -1;
    }

    public List<Order> findByDateRange(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE created_at BETWEEN ? AND ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(new Order(
                        rs.getInt("order_number"),
                        rs.getInt("staff_id"),
                        rs.getString("order_type"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("discount"),
                        rs.getDouble("total"),
                        rs.getString("payment_type"),
                        rs.getString("notes")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching orders: " + e.getMessage());
        }
        return orders;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM orders";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting orders: " + e.getMessage());
        }
        return 0;
    }

    public double getTotalSales() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Error calculating total sales: " + e.getMessage());
        }
        return 0;
    }
}