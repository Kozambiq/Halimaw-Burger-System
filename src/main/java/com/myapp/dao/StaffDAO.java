package com.myapp.dao;

import com.myapp.model.Staff;
import com.myapp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class StaffDAO {

    public List<Staff> findAll() {
        List<Staff> staffList = new ArrayList<>();
        String sql = "SELECT id, name, role, shift_start, shift_end, status FROM staff ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                staffList.add(new Staff(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("role"),
                    rs.getTime("shift_start") != null ? rs.getTime("shift_start").toLocalTime() : null,
                    rs.getTime("shift_end") != null ? rs.getTime("shift_end").toLocalTime() : null,
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading staff: " + e.getMessage());
        }

        return staffList;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE staff SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating staff status: " + e.getMessage());
        }
        return false;
    }

    public boolean updateShift(int id, LocalTime shiftStart, LocalTime shiftEnd) {
        String sql = "UPDATE staff SET shift_start = ?, shift_end = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTime(1, java.sql.Time.valueOf(shiftStart));
            stmt.setTime(2, java.sql.Time.valueOf(shiftEnd));
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating shift: " + e.getMessage());
        }
        return false;
    }

    public boolean updateRole(int id, String role) {
        String sql = "UPDATE staff SET role = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, role);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating role: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM staff WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting staff: " + e.getMessage());
        }
        return false;
    }

    public boolean insert(String name, String role, LocalTime shiftStart, LocalTime shiftEnd) {
        String sql = "INSERT INTO staff (name, role, shift_start, shift_end, status) VALUES (?, ?, ?, ?, 'Off Shift')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, role);
            stmt.setTime(3, java.sql.Time.valueOf(shiftStart));
            stmt.setTime(4, java.sql.Time.valueOf(shiftEnd));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting staff: " + e.getMessage());
        }
        return false;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM staff";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting staff: " + e.getMessage());
        }
        return 0;
    }

    public int getActiveCount() {
        String sql = "SELECT COUNT(*) FROM staff WHERE status = 'Active'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting active staff: " + e.getMessage());
        }
        return 0;
    }

    public int getOnBreakCount() {
        String sql = "SELECT COUNT(*) FROM staff WHERE status = 'Break'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting staff on break: " + e.getMessage());
        }
        return 0;
    }

    public boolean existsByName(String name) {
        String sql = "SELECT COUNT(*) FROM staff WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking staff: " + e.getMessage());
        }
        return false;
    }
}