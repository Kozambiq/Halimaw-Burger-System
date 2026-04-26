package com.myapp.util;

import com.myapp.util.DatabaseConnection;
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
        public List<String[]> topItems;
        public List<String[]> stockLevels;
        public List<String[]> hourlyRevenue;
        public List<String[]> dailyRevenue;
        public List<String[]> categoryBreakdown;
    }

    public static SalesSummary fetchSummary() throws SQLException {
        SalesSummary s = new SalesSummary();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        try (Connection conn = DatabaseConnection.getConnection()) {

            String revenueSQL = "SELECT COALESCE(SUM(total),0), COUNT(*) FROM orders "
                    + "WHERE DATE(created_at) = ? AND status = 'Completed'";
            try (PreparedStatement ps = conn.prepareStatement(revenueSQL)) {
                ps.setDate(1, Date.valueOf(today));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    s.revenueToday = rs.getDouble(1);
                    s.ordersToday  = rs.getInt(2);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(revenueSQL)) {
                ps.setDate(1, Date.valueOf(yesterday));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    s.revenueYesterday = rs.getDouble(1);
                    s.ordersYesterday  = rs.getInt(2);
                }
            }

            String topSQL = "SELECT oi.item_name, SUM(oi.quantity) AS qty, "
                    + "SUM(oi.total_price) AS revenue "
                    + "FROM order_items oi "
                    + "JOIN orders o ON oi.order_id = o.id "
                    + "WHERE DATE(o.created_at) = ? AND o.status = 'Completed' "
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

            String hourlySQL = "SELECT HOUR(created_at) AS hr, COALESCE(SUM(total),0) AS rev "
                    + "FROM orders WHERE DATE(created_at) = ? AND status = 'Completed' "
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

            String catSQL = "SELECT mi.category, SUM(oi.quantity) AS qty, SUM(oi.total_price) AS revenue "
                    + "FROM order_items oi "
                    + "JOIN orders o ON oi.order_id = o.id "
                    + "LEFT JOIN menu_items mi ON oi.item_id = mi.id AND oi.item_type = 'MenuItem' "
                    + "WHERE DATE(o.created_at) = ? AND o.status = 'Completed' "
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

    public static List<String[]> fetchDailyRevenue(int days) throws SQLException {
        List<String[]> dailyRevenue = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        try (Connection conn = DatabaseConnection.getConnection()) {
            String dailySQL = "SELECT DATE(created_at) AS sale_date, COALESCE(SUM(total),0) AS rev "
                    + "FROM orders WHERE DATE(created_at) BETWEEN ? AND ? AND status = 'Completed' "
                    + "GROUP BY sale_date ORDER BY sale_date";
            try (PreparedStatement ps = conn.prepareStatement(dailySQL)) {
                ps.setDate(1, Date.valueOf(startDate));
                ps.setDate(2, Date.valueOf(endDate));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String dateStr = rs.getDate("sale_date").toString();
                    String label = LocalDate.parse(dateStr).format(java.time.format.DateTimeFormatter.ofPattern("MMM dd"));
                    dailyRevenue.add(new String[]{ label, String.format("%.2f", rs.getDouble("rev")) });
                }
            }
        }

        return dailyRevenue;
    }

    public static String buildAnalyticsPrompt(SalesSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a sales analyst for Halimaw Burger, a burger restaurant in the Philippines.\n");
        sb.append("IMPORTANT: Return raw HTML only. Do NOT wrap in ```html or ``` code blocks.\n");
        sb.append("Analyze the sales data below and respond ONLY in clean HTML.\n");
        sb.append("Use this exact structure:\n");
        sb.append("- <h4> for section headers\n");
        sb.append("- <ul><li> for bullet points\n");
        sb.append("- <b> to highlight key numbers or item names\n");
        sb.append("- <span style='color:#e05a2b'> for positive highlights\n");
        sb.append("- <span style='color:#c0392b'> for warnings or concerns\n");
        sb.append("- Max 3 sections, max 3 bullets each. Be concise.\n");
        sb.append("- No markdown, no plain text, no explanations outside HTML.\n\n");

        sb.append("=== TODAY'S DATA ===\n");
        sb.append("Revenue Today: PHP ").append(String.format("%.2f", s.revenueToday)).append("\n");
        sb.append("Revenue Yesterday: PHP ").append(String.format("%.2f", s.revenueYesterday)).append("\n");
        sb.append("Orders Today: ").append(s.ordersToday).append("\n");
        sb.append("Orders Yesterday: ").append(s.ordersYesterday).append("\n\n");

        sb.append("=== TOP SELLING ITEMS ===\n");
        for (String[] item : s.topItems) {
            sb.append("- ").append(item[0]).append(": ").append(item[1]).append(" sold, PHP ").append(item[2]).append("\n");
        }

        sb.append("\n=== SALES BY CATEGORY ===\n");
        for (String[] cat : s.categoryBreakdown) {
            sb.append("- ").append(cat[0]).append(": ").append(cat[1]).append(" items, PHP ").append(cat[2]).append("\n");
        }

        return sb.toString();
    }

    public static String buildRecommendationPrompt(SalesSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an inventory manager for Halimaw Burger, a burger restaurant in the Philippines.\n");
        sb.append("IMPORTANT: Return raw HTML only. Do NOT wrap in ```html or ``` code blocks.\n");
        sb.append("Review the inventory below and respond ONLY in clean HTML.\n");
        sb.append("Use this exact structure:\n");
        sb.append("- <h4> for section headers\n");
        sb.append("- <ul><li> for each ingredient action\n");
        sb.append("- <b> for ingredient names\n");
        sb.append("- <span style='color:#c0392b'> for urgent restock needed\n");
        sb.append("- <span style='color:#e08c2b'> for low but not critical\n");
        sb.append("- <span style='color:#27ae60'> for items that are fine\n");
        sb.append("- Max 2 sections: Urgent and Proactive. Max 4 bullets each.\n");
        sb.append("- No markdown, no plain text, no explanations outside HTML.\n\n");

        sb.append("=== CURRENT INVENTORY ===\n");
        sb.append("Ingredient | Current | Min | Max | Unit\n");
        for (String[] ing : s.stockLevels) {
            sb.append("- ").append(ing[0])
                    .append(" | ").append(ing[2]).append(" ").append(ing[1])
                    .append(" | min: ").append(ing[3])
                    .append(" | max: ").append(ing[4]).append("\n");
        }

        sb.append("\n=== TODAY'S TOP SELLERS ===\n");
        for (String[] item : s.topItems) {
            sb.append("- ").append(item[0]).append(": ").append(item[1]).append(" sold\n");
        }

        return sb.toString();
    }
}