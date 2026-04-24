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
        String orderSql = "INSERT INTO orders (order_number, staff_id, order_type, subtotal, discount, total, payment_type, reference_number, status, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                orderStmt.setString(8, order.getReferenceNumber());
                orderStmt.setString(9, order.getStatus());
                orderStmt.setString(10, order.getNotes());

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
        String sql = "SELECT o.*, COALESCE((SELECT GROUP_CONCAT(CONCAT(quantity, 'x ', item_name) SEPARATOR ', ') FROM order_items WHERE order_id = o.id), 'No items') as summary FROM orders o WHERE o.created_at BETWEEN ? AND ? ORDER BY o.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order(
                        rs.getInt("order_number"),
                        rs.getInt("staff_id"),
                        rs.getString("order_type"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("discount"),
                        rs.getDouble("total"),
                        rs.getString("payment_type"),
                        rs.getString("reference_number"),
                        rs.getString("status"),
                        rs.getString("notes")
                    );
                    order.setId(rs.getInt("id"));
                    order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    order.setItemsSummary(rs.getString("summary"));
                    orders.add(order);
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

    public int getNextOrderNumber() {
        String sql = "SELECT COALESCE(MAX(order_number), 0) + 1 FROM orders";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error getting next order number: " + e.getMessage());
        }
        return 1;
    }

    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, COALESCE((SELECT GROUP_CONCAT(CONCAT(quantity, 'x ', item_name) SEPARATOR ', ') FROM order_items WHERE order_id = o.id), 'No items') as summary FROM orders o ORDER BY o.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("order_number"),
                    rs.getInt("staff_id"),
                    rs.getString("order_type"),
                    rs.getDouble("subtotal"),
                    rs.getDouble("discount"),
                    rs.getDouble("total"),
                    rs.getString("payment_type"),
                    rs.getString("reference_number"),
                    rs.getString("status"),
                    rs.getString("notes")
                );
                order.setId(rs.getInt("id"));
                order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                Timestamp cancelledTs = rs.getTimestamp("cancelled_at");
                if (cancelledTs != null) {
                    order.setCancelledAt(cancelledTs.toLocalDateTime());
                }
                order.setStatus(rs.getString("status"));
                order.setItemsSummary(rs.getString("summary"));
                orders.add(order);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching orders: " + e.getMessage());
        }
        return orders;
    }

    public List<Order> findByFilters(String orderType, LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT o.*, COALESCE((SELECT GROUP_CONCAT(CONCAT(quantity, 'x ', item_name) SEPARATOR ', ') FROM order_items WHERE order_id = o.id), 'No items') as summary FROM orders o WHERE 1=1");

        if (orderType != null && !orderType.isEmpty() && !orderType.equals("All Types")) {
            sql.append(" AND o.order_type = ?");
        }
        if (startDate != null && endDate != null) {
            sql.append(" AND DATE(o.created_at) BETWEEN DATE(?) AND DATE(?)");
        }
        sql.append(" ORDER BY o.created_at DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            if (orderType != null && !orderType.isEmpty() && !orderType.equals("All Types")) {
                stmt.setString(paramIndex++, orderType);
            }
            if (startDate != null && endDate != null) {
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(startDate));
                stmt.setTimestamp(paramIndex++, Timestamp.valueOf(endDate));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = new Order(
                        rs.getInt("order_number"),
                        rs.getInt("staff_id"),
                        rs.getString("order_type"),
                        rs.getDouble("subtotal"),
                        rs.getDouble("discount"),
                        rs.getDouble("total"),
                        rs.getString("payment_type"),
                        rs.getString("reference_number"),
                        rs.getString("status"),
                        rs.getString("notes")
                    );
                    order.setId(rs.getInt("id"));
                    order.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    order.setStatus(rs.getString("status"));
                    order.setItemsSummary(rs.getString("summary"));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching filtered orders: " + e.getMessage());
        }
        return orders;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE orders SET status = ?";
        if ("Cancelled".equals(status)) {
            sql += ", cancelled_at = NOW()";
        }
        sql += " WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating order status: " + e.getMessage());
        }
        return false;
    }

    public int getCountByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting orders by status: " + e.getMessage());
        }
        return 0;
    }

    public List<OrderItem> findItemsByOrderId(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem(
                        rs.getString("item_type"),
                        rs.getInt("item_id"),
                        rs.getString("item_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("unit_price")
                    );
                    item.setId(rs.getInt("id"));
                    item.setOrderId(rs.getInt("order_id"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching order items: " + e.getMessage());
        }
        return items;
    }
}