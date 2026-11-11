package com.yourdomain.dao;

import com.yourdomain.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the activity_logs table (audit trail) lives here. */
public class ActivityLogDao {

    private static final String SELECT = "SELECT id, user_id, action_type, description, timestamp FROM activity_logs ";
    private static final String ORDER = " ORDER BY timestamp DESC, id DESC";

    private ActivityLog map(ResultSet rs) throws SQLException {
        return new ActivityLog(rs.getInt("id"), rs.getInt("user_id"), rs.getString("action_type"),
                rs.getString("description"), rs.getString("timestamp"));
    }

    /** Inserts one audit entry. Returns true when the row was saved. */
    public boolean logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Logging error: " + e.getMessage());
            return false;
        }
    }

    public List<ActivityLog> getAllLogs() {
        return query(SELECT + ORDER, null);
    }

    public List<ActivityLog> getLogsByUserId(int userId) {
        return query(SELECT + "WHERE user_id = ?" + ORDER, userId);
    }

    /** Booking Audit Log (proposal): booking creation, status changes, cancellations, payments. */
    public List<ActivityLog> getBookingAuditLogs() {
        return query(SELECT + "WHERE action_type LIKE 'BOOKING%' OR action_type = 'PAYMENT_RECORDED'" + ORDER, null);
    }

    /** User Activity Log (proposal): login/logout and registration events. */
    public List<ActivityLog> getUserActivityLogs() {
        return query(SELECT + "WHERE action_type IN ('USER_LOGIN', 'USER_LOGOUT', 'USER_REGISTERED')" + ORDER, null);
    }

    private List<ActivityLog> query(String sql, Integer param) {
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
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

    /** Deletes every log row. Returns how many rows were removed (-1 on error). */
    public int clearAllLogs() {
        String sql = "DELETE FROM activity_logs";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return -1;
        }
    }
}
