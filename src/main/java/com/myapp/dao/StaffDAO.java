package com.myapp.dao;

import com.myapp.model.Staff;
import com.myapp.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class StaffDAO {

    public Staff findById(int id) {
        String sql = "SELECT id, name, email, role, shift_start, shift_end, status FROM staff WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Staff(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getTime("shift_start") != null ? rs.getTime("shift_start").toLocalTime() : null,
                        rs.getTime("shift_end") != null ? rs.getTime("shift_end").toLocalTime() : null,
                        rs.getString("status")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding staff by id: " + e.getMessage());
        }
        return null;
    }

    public List<Staff> findAll() {
        List<Staff> staffList = new ArrayList<>();
        String sql = "SELECT id, name, email, role, shift_start, shift_end, status FROM staff ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                staffList.add(new Staff(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
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

    public boolean updateName(int id, String name) {
        String sql = "UPDATE staff SET name = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating name: " + e.getMessage());
        }
        return false;
    }

    public boolean updateEmail(int id, String email) {
        String sql = "UPDATE staff SET email = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating email: " + e.getMessage());
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

    public boolean insert(String name, String email, String password, String role, LocalTime shiftStart, LocalTime shiftEnd) {
        try {
            String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());

            String staffSql = "INSERT INTO staff (name, email, role, shift_start, shift_end, status) VALUES (?, ?, ?, ?, ?, 'Off Shift')";
            int staffId;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(staffSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.setString(3, role);
                stmt.setTime(4, java.sql.Time.valueOf(shiftStart));
                stmt.setTime(5, java.sql.Time.valueOf(shiftEnd));
                stmt.executeUpdate();

                try (java.sql.ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        staffId = rs.getInt(1);
                    } else {
                        return false;
                    }
                }
            }

            String userSql = "INSERT INTO users (staff_id, email, password_hash, role, is_active) VALUES (?, ?, ?, ?, 1)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(userSql)) {
                stmt.setInt(1, staffId);
                stmt.setString(2, email);
                stmt.setString(3, hashedPassword);
                stmt.setString(4, role);
                stmt.executeUpdate();
            }

            return true;
        } catch (SQLException e) {
            System.err.println("Error inserting staff and user: " + e.getMessage());
        }
        return false;
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM staff WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking email: " + e.getMessage());
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

    public List<Staff> findActiveStaff() {
        List<Staff> staffList = new ArrayList<>();
        String sql = "SELECT id, name, email, role, shift_start, shift_end, status FROM staff WHERE status = 'Active' ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                staffList.add(new Staff(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getTime("shift_start") != null ? rs.getTime("shift_start").toLocalTime() : null,
                    rs.getTime("shift_end") != null ? rs.getTime("shift_end").toLocalTime() : null,
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading active staff: " + e.getMessage());
        }
        return staffList;
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