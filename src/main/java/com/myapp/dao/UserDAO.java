package com.myapp.dao;

import com.myapp.model.User;
import com.myapp.util.DatabaseConnection;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDAO {

    public Optional<User> authenticate(String email, String password) {
        String sql = "SELECT id, staff_id, email, password_hash, role, is_active FROM users WHERE email = ? AND is_active = 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    if (verifyPassword(password, storedHash)) {
                        User user = new User(
                            rs.getInt("id"),
                            rs.getInt("staff_id"),
                            rs.getString("email"),
                            rs.getString("role")
                        );
                        user.setPasswordHash(storedHash);
                        user.setActive(rs.getInt("is_active") == 1);
                        
                        updateLastLogin(user.getId());
                        return Optional.of(user);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT id, staff_id, email, role, is_active FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                        rs.getInt("id"),
                        rs.getInt("staff_id"),
                        rs.getString("email"),
                        rs.getString("role")
                    );
                    user.setActive(rs.getInt("is_active") == 1);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        
        return Optional.empty();
    }

    private boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null || password == null) {
            return false;
        }
        
        // Plain text comparison (e.g., "admin123")
        return password.equals(storedHash);
    }

    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }
}