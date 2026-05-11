package com.myapp.dao;

import com.myapp.model.Order;
import com.myapp.model.OrderItem;
import com.myapp.util.DatabaseConnection;
import com.myapp.model.MenuItemIngredient;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Order management.
 * Handles critical transactions including order placement,
 * real-time ingredient reservations, and complex analytical aggregations.
 */
public class OrderDAO {

    /**
     * Atomically inserts an order and its constituent items.
     * TRANSACTION STRATEGY:
     * 1. Start Transaction (Disable Auto-Commit).
     * 2. Insert Order Header.
     * 3. Batch Insert Order Items.
     * 4. Perform Ingredient Reservation (Fails if stock ran out during the process).
     * 5. Commit if all steps succeed, otherwise Rollback.
     */
    public int insert(Order order, List<OrderItem> items) {
        String orderSql = "INSERT INTO orders (order_number, staff_id, order_type, subtotal, discount, total, payment_type, reference_number, status, notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_id, item_type, item_id, item_name, quantity, unit_price, total_price) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId = -1;
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
                    orderStmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
                    orderStmt.executeUpdate();

                    try (ResultSet rs = orderStmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            orderId = rs.getInt(1);
                        }
                    }
                }

                if (orderId > 0) {
                    try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                        for (OrderItem item : items) {
                            itemStmt.setInt(1, orderId);
                            itemStmt.setString(2, item.getItemType());
                            itemStmt.setInt(3, item.getItemId());
                            itemStmt.setString(4, item.getItemName());
                            itemStmt.setInt(5, item.getQuantity());
                            itemStmt.setDouble(6, item.getUnitPrice());
                            itemStmt.setDouble(7, item.getTotalPrice());
                            itemStmt.addBatch();
                        }
                        itemStmt.executeBatch();
                    }

                    // ATOMIC RESERVATION: Reserves ingredients within the same transaction
                    String reserveError = reserveIngredientsInTransaction(conn, orderId, items);
                    if (reserveError != null) {
                        conn.rollback();
                        // Special code -2 indicates a concurrency/stock failure
                        return -2; 
                    }

                    conn.commit();
                    return orderId;
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error in transaction: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error with database connection: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Logic for reserving ingredients to prevent overselling.
     * Checks availability against (Physical Quantity - Existing Reservations).
     */
    private String reserveIngredientsInTransaction(Connection conn, int orderId, List<OrderItem> items) throws SQLException {
        // AGGREGATION: Maps ingredient IDs to total quantity needed for this entire order
        java.util.Map<Integer, Double> needed = new java.util.HashMap<>();
        
        // 1. Resolve MenuItem and Combo ingredients into raw needs
        String menuItemSql = "SELECT mmi.ingredient_id, mmi.quantity_used FROM menu_item_ingredients mmi WHERE mmi.menu_item_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(menuItemSql)) {
            for (OrderItem item : items) {
                if ("MenuItem".equals(item.getItemType())) {
                    stmt.setInt(1, item.getItemId());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            int ingId = rs.getInt(1);
                            double qty = rs.getDouble(2);
                            needed.merge(ingId, qty * item.getQuantity(), Double::sum);
                        }
                    }
                } else if ("Combo".equals(item.getItemType())) {
                    // For combos, parse inclusions and resolve their individual ingredient requirements
                    String comboSql = "SELECT includes FROM combos WHERE id = ?";
                    try (PreparedStatement comboStmt = conn.prepareStatement(comboSql)) {
                        comboStmt.setInt(1, item.getItemId());
                        try (ResultSet rsCombo = comboStmt.executeQuery()) {
                            if (rsCombo.next()) {
                                String[] itemNames = rsCombo.getString("includes").split(",");
                                for (String name : itemNames) {
                                    name = name.trim();
                                    String ingForNameSql = "SELECT mmi.ingredient_id, mmi.quantity_used FROM menu_item_ingredients mmi JOIN menu_items mi ON mmi.menu_item_id = mi.id WHERE LOWER(mi.name) = LOWER(?)";
                                    try (PreparedStatement ingStmt = conn.prepareStatement(ingForNameSql)) {
                                        ingStmt.setString(1, name);
                                        try (ResultSet rsIng = ingStmt.executeQuery()) {
                                            while (rsIng.next()) {
                                                needed.merge(rsIng.getInt(1), rsIng.getDouble(2) * item.getQuantity(), Double::sum);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CHECK & UPDATE: Verify availability and increment the 'reserved' column
        String checkSql = "SELECT name, quantity, COALESCE(reserved, 0) as reserved FROM ingredients WHERE id = ?";
        String updateSql = "UPDATE ingredients SET reserved = COALESCE(reserved, 0) + ? WHERE id = ?";
        
        for (java.util.Map.Entry<Integer, Double> entry : needed.entrySet()) {
            int ingId = entry.getKey();
            double qtyNeeded = entry.getValue();
            
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, ingId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        double available = rs.getDouble("quantity") - rs.getDouble("reserved");
                        if (available < qtyNeeded) {
                            return "Not enough stock for: " + rs.getString("name");
                        }
                    }
                }
            }
            
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, qtyNeeded);
                updateStmt.setInt(2, ingId);
                updateStmt.executeUpdate();
            }
        }

        // 3. FLAG SUCCESS: Mark order as having valid reservations
        String markSql = "UPDATE orders SET ingredients_reserved = 1 WHERE id = ?";
        try (PreparedStatement markStmt = conn.prepareStatement(markSql)) {
            markStmt.setInt(1, orderId);
            markStmt.executeUpdate();
        }

        return null;
    }

    /**
     * Fetches orders within a date range. 
     * Uses a subquery to generate a human-readable summary of items for display.
     */
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

    /**
     * Fetches active orders for the Kitchen queue.
     * Includes an optimization to pre-fetch items for each order to avoid the N+1 problem.
     */
    public List<Order> findActiveOrders() {
        List<Order> orders = new java.util.ArrayList<>();
        String sql = "SELECT o.* FROM orders o WHERE status IN ('New', 'Preparing', 'Done') " +
                     "OR (status = 'Cancelled' AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)) " +
                     "ORDER BY created_at ASC";
        
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
                
                // SOLVE N+1: Pre-fetches items using the existing database connection
                order.setItems(findItemsByOrderId(conn, order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active orders: " + e.getMessage());
        }
        return orders;
    }

    public List<OrderItem> findItemsByOrderId(Connection conn, int orderId) throws SQLException {
        List<OrderItem> items = new java.util.ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        }
        return items;
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

    /**
     * Public wrapper for ingredient reservation. 
     * Used when an order is created at the POS.
     */
    public String reserveIngredientsForOrder(int orderId) {
        String checkSql = "SELECT ingredients_reserved FROM orders WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, orderId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt("ingredients_reserved") == 1) {
                    return null; // Already reserved
                }
            }
        } catch (SQLException e) {
            return "Error checking order: " + e.getMessage();
        }

        List<OrderItem> items = findItemsByOrderId(orderId);
        IngredientDAO ingredientDAO = new IngredientDAO();
        MenuItemDAO menuItemDAO = new MenuItemDAO();

        for (OrderItem item : items) {
            if ("MenuItem".equals(item.getItemType())) {
                int menuItemId = item.getItemId();
                int orderQty = item.getQuantity();
                List<MenuItemIngredient> menuItemIngredients = menuItemDAO.getIngredientsForMenuItem(menuItemId);
                for (MenuItemIngredient mi : menuItemIngredients) {
                    double totalNeeded = mi.getQuantity() * orderQty;
                    int ingId = ingredientDAO.findIdByName(mi.getIngredientName());
                    if (ingId > 0 && !ingredientDAO.reserve(ingId, totalNeeded)) {
                        return "Not enough available stock for: " + mi.getIngredientName();
                    }
                }
            } else if ("Combo".equals(item.getItemType())) {
                String includes = item.getItemName();
                String[] itemNames = includes.split(",");
                int orderQty = item.getQuantity();
                for (String itemName : itemNames) {
                    itemName = itemName.trim();
                    List<MenuItemIngredient> menuItemIngredients = menuItemDAO.getIngredientsForMenuItemByName(itemName);
                    for (MenuItemIngredient mi : menuItemIngredients) {
                        double totalNeeded = mi.getQuantity() * orderQty;
                        int ingId = ingredientDAO.findIdByName(mi.getIngredientName());
                        if (ingId > 0 && !ingredientDAO.reserve(ingId, totalNeeded)) {
                            return "Not enough available stock for: " + mi.getIngredientName();
                        }
                    }
                }
            }
        }

        String updateSql = "UPDATE orders SET ingredients_reserved = 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, orderId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking ingredients as reserved: " + e.getMessage());
        }

        return null;
    }

    /**
     * Frees previously reserved ingredients back into the available pool.
     * Used during order cancellation.
     */
    public void releaseReservationsForOrder(int orderId) {
        List<OrderItem> items = findItemsByOrderId(orderId);
        IngredientDAO ingredientDAO = new IngredientDAO();
        MenuItemDAO menuItemDAO = new MenuItemDAO();

        for (OrderItem item : items) {
            if ("MenuItem".equals(item.getItemType())) {
                int menuItemId = item.getItemId();
                int orderQty = item.getQuantity();
                List<MenuItemIngredient> menuItemIngredients = menuItemDAO.getIngredientsForMenuItem(menuItemId);
                for (MenuItemIngredient mi : menuItemIngredients) {
                    double totalNeeded = mi.getQuantity() * orderQty;
                    int ingId = ingredientDAO.findIdByName(mi.getIngredientName());
                    if (ingId > 0) {
                        ingredientDAO.releaseReservation(ingId, totalNeeded);
                    }
                }
            } else if ("Combo".equals(item.getItemType())) {
                String includes = item.getItemName();
                String[] itemNames = includes.split(",");
                int orderQty = item.getQuantity();
                for (String itemName : itemNames) {
                    itemName = itemName.trim();
                    List<MenuItemIngredient> menuItemIngredients = menuItemDAO.getIngredientsForMenuItemByName(itemName);
                    for (MenuItemIngredient mi : menuItemIngredients) {
                        double totalNeeded = mi.getQuantity() * orderQty;
                        int ingId = ingredientDAO.findIdByName(mi.getIngredientName());
                        if (ingId > 0) {
                            ingredientDAO.releaseReservation(ingId, totalNeeded);
                        }
                    }
                }
            }
        }
    }

    /**
     * Physically deducts ingredients from stock and clears reservations.
     * TRANSACTIONAL: Ensures atomic update for all affected ingredients.
     */
    public String deductIngredientsForOrder(int orderId) {
        String checkSql = "SELECT ingredients_deducted, ingredients_reserved FROM orders WHERE id = ?";
        boolean isReserved = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, orderId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getInt("ingredients_deducted") == 1) return null;
                    isReserved = rs.getInt("ingredients_reserved") == 1;
                }
            }
        } catch (SQLException e) {
            return "Error checking order: " + e.getMessage();
        }

        List<OrderItem> items = findItemsByOrderId(orderId);
        java.util.Map<Integer, Double> needed = new java.util.HashMap<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Aggregate all needed ingredients
                for (OrderItem item : items) {
                    if ("MenuItem".equals(item.getItemType())) {
                        String ingSql = "SELECT ingredient_id, quantity_used FROM menu_item_ingredients WHERE menu_item_id = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(ingSql)) {
                            stmt.setInt(1, item.getItemId());
                            try (ResultSet rs = stmt.executeQuery()) {
                                while (rs.next()) {
                                    needed.merge(rs.getInt(1), rs.getDouble(2) * item.getQuantity(), Double::sum);
                                }
                            }
                        }
                    } else if ("Combo".equals(item.getItemType())) {
                        String comboSql = "SELECT includes FROM combos WHERE id = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(comboSql)) {
                            stmt.setInt(1, item.getItemId());
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    String[] names = rs.getString("includes").split(",");
                                    for (String name : names) {
                                        String ingForNameSql = "SELECT mmi.ingredient_id, mmi.quantity_used FROM menu_item_ingredients mmi JOIN menu_items mi ON mmi.menu_item_id = mi.id WHERE LOWER(mi.name) = LOWER(?)";
                                        try (PreparedStatement ingStmt = conn.prepareStatement(ingForNameSql)) {
                                            ingStmt.setString(1, name.trim());
                                            try (ResultSet rsIng = ingStmt.executeQuery()) {
                                                while (rsIng.next()) {
                                                    needed.merge(rsIng.getInt(1), rsIng.getDouble(2) * item.getQuantity(), Double::sum);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // DEDUCTION LOGIC: Reduces physical quantity and releases reservation simultaneously
                String deductSql = "UPDATE ingredients SET quantity = quantity - ? WHERE id = ?";
                String releaseSql = "UPDATE ingredients SET reserved = CASE WHEN COALESCE(reserved, 0) - ? < 0 THEN 0 ELSE COALESCE(reserved, 0) - ? END WHERE id = ?";
                
                try (PreparedStatement deductStmt = conn.prepareStatement(deductSql);
                     PreparedStatement releaseStmt = conn.prepareStatement(releaseSql)) {
                    for (java.util.Map.Entry<Integer, Double> entry : needed.entrySet()) {
                        int ingId = entry.getKey();
                        double qty = entry.getValue();

                        deductStmt.setDouble(1, qty);
                        deductStmt.setInt(2, ingId);
                        deductStmt.addBatch();

                        if (isReserved) {
                            releaseStmt.setDouble(1, qty);
                            releaseStmt.setDouble(2, qty);
                            releaseStmt.setInt(3, ingId);
                            releaseStmt.addBatch();
                        }
                    }
                    deductStmt.executeBatch();
                    if (isReserved) releaseStmt.executeBatch();
                }

                String updateOrderSql = "UPDATE orders SET ingredients_deducted = 1 WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateOrderSql)) {
                    stmt.setInt(1, orderId);
                    stmt.executeUpdate();
                }

                conn.commit();
                return null;
            } catch (SQLException e) {
                conn.rollback();
                return "Error deducting ingredients: " + e.getMessage();
            }
        } catch (SQLException e) {
            return "Connection error: " + e.getMessage();
        }
    }

    public String checkThresholdWarnings(int orderId) {
        List<OrderItem> items = findItemsByOrderId(orderId);
        IngredientDAO ingredientDAO = new IngredientDAO();
        MenuItemDAO menuItemDAO = new MenuItemDAO();
        List<String> warnings = new ArrayList<>();

        for (OrderItem item : items) {
            if ("MenuItem".equals(item.getItemType())) {
                int menuItemId = item.getItemId();
                int orderQty = item.getQuantity();
                List<MenuItemIngredient> menuItemIngredients = menuItemDAO.getIngredientsForMenuItem(menuItemId);
                for (MenuItemIngredient mi : menuItemIngredients) {
                    double totalNeeded = mi.getQuantity() * orderQty;
                    int ingId = ingredientDAO.findIdByName(mi.getIngredientName());
                    if (ingId > 0 && ingredientDAO.wouldGoBelowThreshold(ingId, totalNeeded)) {
                        warnings.add(mi.getIngredientName() + " will be low");
                    }
                }
            } else if ("Combo".equals(item.getItemType())) {
                String includes = item.getItemName();
                String[] itemNames = includes.split(" \\+ ");
                int orderQty = item.getQuantity();
                for (String itemName : itemNames) {
                    itemName = itemName.trim();
                    List<MenuItemIngredient> menuItemIngredients = menuItemDAO.getIngredientsForMenuItemByName(itemName);
                    for (MenuItemIngredient mi : menuItemIngredients) {
                        double totalNeeded = mi.getQuantity() * orderQty;
                        int ingId = ingredientDAO.findIdByName(mi.getIngredientName());
                        if (ingId > 0 && ingredientDAO.wouldGoBelowThreshold(ingId, totalNeeded)) {
                            warnings.add(mi.getIngredientName() + " will be low");
                        }
                    }
                }
            }
        }

        return warnings.isEmpty() ? null : String.join(", ", warnings);
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

    public double getTodayRevenue() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE DATE(created_at) = CURDATE() AND status != 'Cancelled'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Error calculating today's revenue: " + e.getMessage());
        }
        return 0;
    }

    public double getYesterdayRevenue() {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM orders WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND status != 'Cancelled'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Error calculating yesterday's revenue: " + e.getMessage());
        }
        return 0;
    }

    public int getTodayOrderCount() {
        String sql = "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE() AND status != 'Cancelled'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting today's orders: " + e.getMessage());
        }
        return 0;
    }

    public int getYesterdayOrderCount() {
        String sql = "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND status != 'Cancelled'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error counting yesterday's orders: " + e.getMessage());
        }
        return 0;
    }
}