package com.myapp.util;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SalesReportService {

    public static class SalesSummary {
        public double revenueToday;
        public double revenueYesterday;
        public int ordersToday;
        public int ordersYesterday;
        public List<String[]> topItems;       // [name, qty, revenue]
        public List<String[]> stockLevels;    // [name, unit, qty, min, max]
        public List<String[]> hourlyRevenue;  // [hour, revenue]
        public List<String[]> categoryBreakdown; // [category, qty, revenue]
    }

    public static SalesSummary fetchSummary() throws SQLException {
        SalesSummary s = new SalesSummary();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        try (Connection conn = DatabaseConnection.getConnection()) {

            // --- Revenue & orders today ---
            String revenueSQL = "SELECT COALESCE(SUM(total),0), COUNT(*) FROM orders "
                + "WHERE DATE(created_at) = ? AND status != 'Cancelled'";
            try (PreparedStatement ps = conn.prepareStatement(revenueSQL)) {
                ps.setDate(1, Date.valueOf(today));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    s.revenueToday = rs.getDouble(1);
                    s.ordersToday  = rs.getInt(2);
                }
            }

            // --- Revenue & orders yesterday ---
            try (PreparedStatement ps = conn.prepareStatement(revenueSQL)) {
                ps.setDate(1, Date.valueOf(yesterday));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    s.revenueYesterday = rs.getDouble(1);
                    s.ordersYesterday  = rs.getInt(2);
                }
            }

            // --- Top selling items today ---
            String topSQL = "SELECT oi.item_name, SUM(oi.quantity) AS qty, "
                + "SUM(oi.total_price) AS revenue "
                + "FROM order_items oi "
                + "JOIN orders o ON oi.order_id = o.id "
                + "WHERE DATE(o.created_at) = ? AND o.status != 'Cancelled' "
                + "GROUP BY oi.item_name ORDER BY qty DESC LIMIT 8";
            s.topItems = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(topSQL)) {
                ps.setDate(1, Date.valueOf(today));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    s.topItems.add(new String[]{
                        rs.getString("item_name"),
                        String.valueOf(rs.getInt("qty")),
                        String.format("%.2f", rs.getDouble("revenue"))
                    });
                }
            }

            // --- Stock levels ---
            String stockSQL = "SELECT name, unit, quantity, min_threshold, max_stock FROM ingredients ORDER BY name";
            s.stockLevels = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(stockSQL)) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    s.stockLevels.add(new String[]{
                        rs.getString("name"),
                        rs.getString("unit"),
                        String.format("%.2f", rs.getDouble("quantity")),
                        String.format("%.2f", rs.getDouble("min_threshold")),
                        String.format("%.2f", rs.getDouble("max_stock"))
                    });
                }
            }

            // --- Hourly revenue today ---
            String hourlySQL = "SELECT HOUR(created_at) AS hr, COALESCE(SUM(total),0) AS rev "
                + "FROM orders WHERE DATE(created_at) = ? AND status != 'Cancelled' "
                + "GROUP BY hr ORDER BY hr";
            s.hourlyRevenue = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(hourlySQL)) {
                ps.setDate(1, Date.valueOf(today));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int hr = rs.getInt("hr");
                    String label = (hr == 0 ? "12AM" : hr < 12 ? hr + "AM" : hr == 12 ? "12PM" : (hr - 12) + "PM");
                    s.hourlyRevenue.add(new String[]{ label, String.format("%.2f", rs.getDouble("rev")) });
                }
            }

            // --- Category breakdown today ---
            String catSQL = "SELECT mi.category, SUM(oi.quantity) AS qty, SUM(oi.total_price) AS revenue "
                + "FROM order_items oi "
                + "JOIN orders o ON oi.order_id = o.id "
                + "LEFT JOIN menu_items mi ON oi.item_id = mi.id AND oi.item_type = 'MenuItem' "
                + "WHERE DATE(o.created_at) = ? AND o.status != 'Cancelled' "
                + "GROUP BY mi.category ORDER BY revenue DESC";
            s.categoryBreakdown = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(catSQL)) {
                ps.setDate(1, Date.valueOf(today));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String cat = rs.getString("category");
                    s.categoryBreakdown.add(new String[]{
                        cat != null ? cat : "Combo",
                        String.valueOf(rs.getInt("qty")),
                        String.format("%.2f", rs.getDouble("revenue"))
                    });
                }
            }
        }

        return s;
    }

    // Build a readable summary string to send to Gemini
    public static String buildAnalyticsPrompt(SalesSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a sales analyst for Halimaw Burger, a burger restaurant.\n");
        sb.append("Analyze the following sales data for today and give a short, clear report.\n");
        sb.append("Use bullet points. Be concise. Focus on what matters for a restaurant manager.\n\n");

        sb.append("=== TODAY'S SALES SUMMARY ===\n");
        sb.append("Revenue Today: PHP ").append(String.format("%.2f", s.revenueToday)).append("\n");
        sb.append("Revenue Yesterday: PHP ").append(String.format("%.2f", s.revenueYesterday)).append("\n");
        sb.append("Orders Today: ").append(s.ordersToday).append("\n");
        sb.append("Orders Yesterday: ").append(s.ordersYesterday).append("\n\n");

        sb.append("=== TOP SELLING ITEMS TODAY ===\n");
        for (String[] item : s.topItems) {
            sb.append("- ").append(item[0]).append(": ").append(item[1]).append(" sold, PHP ").append(item[2]).append("\n");
        }

        sb.append("\n=== SALES BY CATEGORY ===\n");
        for (String[] cat : s.categoryBreakdown) {
            sb.append("- ").append(cat[0]).append(": ").append(cat[1]).append(" items, PHP ").append(cat[2]).append("\n");
        }

        sb.append("\nProvide: 1) Key insights, 2) What's performing well, 3) Any concerns.");
        return sb.toString();
    }

    public static String buildRecommendationPrompt(SalesSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an inventory manager for Halimaw Burger, a burger restaurant.\n");
        sb.append("Based on the current stock levels and today's sales, give specific restock recommendations.\n");
        sb.append("Use bullet points. Be direct. Tell the manager exactly what to buy and how much.\n\n");

        sb.append("=== CURRENT INVENTORY ===\n");
        sb.append("Format: Ingredient | Current Stock | Min Threshold | Max Stock | Unit\n");
        for (String[] ing : s.stockLevels) {
            sb.append("- ").append(ing[0])
              .append(" | ").append(ing[2]).append(" ").append(ing[1])
              .append(" | min: ").append(ing[3])
              .append(" | max: ").append(ing[4]).append("\n");
        }

        sb.append("\n=== TODAY'S TOP SELLERS (ingredients most consumed) ===\n");
        for (String[] item : s.topItems) {
            sb.append("- ").append(item[0]).append(": ").append(item[1]).append(" sold\n");
        }

        sb.append("\nProvide: 1) What needs restocking urgently, 2) What to buy proactively based on sales trends, ");
        sb.append("3) Estimated quantities to order to reach max stock.");
        return sb.toString();
    }
}
