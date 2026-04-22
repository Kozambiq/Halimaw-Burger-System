package com.myapp.dao;

import com.myapp.model.User;
import com.myapp.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDAO {

    public Optional<User> authenticate(String email, String password) {
        String sql = "SELECT u.id, u.staff_id, u.email, u.password_hash, u.role, u.is_active " +
                     "FROM users u JOIN staff s ON u.staff_id = s.id " +
                     "WHERE u.email = ? AND u.is_active = 1 AND s.status != 'Disabled'";
        
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

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, staff_id, email, role, is_active FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

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
            System.err.println("Error finding user by email: " + e.getMessage());
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
        
        // Check if it's a BCrypt hash (starts with $2a$ or $2b$)
        if (storedHash.startsWith("$2") && storedHash.length() == 60) {
            try {
                return BCrypt.checkpw(password, storedHash);
            } catch (Exception e) {
                System.err.println("Error verifying password: " + e.getMessage());
                return false;
            }
        }
        
        // Plain text comparison for backwards compatibility
        return password.equals(storedHash);
    }

    public Optional<String> findPasswordHashByStaffId(int staffId) {
        String sql = "SELECT password_hash FROM users WHERE staff_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, staffId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("password_hash"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding password hash: " + e.getMessage());
        }
        
        return Optional.empty();
    }
    
    public boolean updatePasswordByStaffId(int staffId, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE staff_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            stmt.setInt(2, staffId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
        
        return false;
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