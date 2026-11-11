package com.yourdomain.dao;

import com.yourdomain.model.User;
import com.yourdomain.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** All user-related SQL. Every query is a parameterized PreparedStatement. */
public class UserDao {

    private static final String COLS = "id, username, full_name, contact_no, email, roles, status";

    private final ActivityLogDao logDao = new ActivityLogDao();

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("contact_no"), rs.getString("email"),
                rs.getString("roles"), rs.getString("status"));
    }

    /** Returns the matching user (any status), or null when the credentials are wrong. */
    public User login(String username, String password) {
        String sql = "SELECT " + COLS + " FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Customer self-registration: role CUSTOMER, status PENDING. Returns the new user ID or -1. */
    public int register(String username, String password, String fullName, String contactNo, String email) {
        String sql = "INSERT INTO users (username, password_hash, full_name, contact_no, email, roles, status) "
                + "VALUES (?, ?, ?, ?, ?, 'CUSTOMER', 'PENDING')";
        int newId = -1;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, fullName);
            ps.setString(4, contactNo);
            ps.setString(5, email);
            if (ps.executeUpdate() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        newId = keys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        if (newId > 0) {
            logDao.logAction(newId, "USER_REGISTERED",
                    String.format("New account registered: %s (ID: %d)", username, newId));
        }
        return newId;
    }

    public User findById(int id) {
        String sql = "SELECT " + COLS + " FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    /** All registered accounts, regardless of role or status. */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM users ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }

    /** All accounts with status = 'PENDING', across every role. */
    public List<User> getPendingUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT " + COLS + " FROM users WHERE status = ? ORDER BY id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }

    /** Deliverable 5: sets the account's status to APPROVED. */
    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = 'APPROVED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Deliverable 5 + 8: approve, then log USER_APPROVED. */
    public boolean approveUser(int adminId, int userId) {
        boolean ok = approveUser(userId);
        if (ok) {
            logDao.logAction(adminId, "USER_APPROVED",
                    String.format("Admin %d approved account for User ID %d", adminId, userId));
        }
        return ok;
    }

    /** Deliverable 7: plain update of a user's role and status (inputs upper-cased). */
    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus) {
        String sql = "UPDATE users SET roles = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole.toUpperCase());
            ps.setString(2, newStatus.toUpperCase());
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Deliverable 7 + 8: same update, then log USER_UPDATED. */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus) {
        boolean ok = updateUserRoleAndStatus(userId, newRole, newStatus);
        if (ok) {
            logDao.logAction(adminId, "USER_UPDATED",
                    String.format("Admin %d changed role of User %d to %s and status to %s",
                            adminId, userId, newRole.toUpperCase(), newStatus.toUpperCase()));
        }
        return ok;
    }

    /** Soft delete: marks the account INACTIVE (the row stays in the database). */
    public boolean deactivateUser(int adminId, int userId) {
        boolean ok = setStatus(userId, "INACTIVE");
        if (ok) {
            logDao.logAction(adminId, "USER_DEACTIVATED",
                    String.format("Admin %d deactivated User ID %d", adminId, userId));
        }
        return ok;
    }

    /** Reverses a soft delete by setting the account back to APPROVED. */
    public boolean reactivateUser(int adminId, int userId) {
        boolean ok = setStatus(userId, "APPROVED");
        if (ok) {
            logDao.logAction(adminId, "USER_REACTIVATED",
                    String.format("Admin %d reactivated User ID %d", adminId, userId));
        }
        return ok;
    }

    private boolean setStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }
}
