package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Queries for the Monthly Bookings & Income Report. yearMonth is formatted yyyy-MM. */
public class ReportDao {

    /** Number of bookings per status for events held in the given month (all four statuses present). */
    public Map<String, Integer> getStatusCounts(String yearMonth) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("PENDING", 0);
        counts.put("CONFIRMED", 0);
        counts.put("COMPLETED", 0);
        counts.put("CANCELLED", 0);

        String sql = "SELECT booking_status, COUNT(*) FROM bookings "
                + "WHERE substr(event_date, 1, 7) = ? GROUP BY booking_status";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, yearMonth);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString(1), rs.getInt(2));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return counts;
    }

    /** Total sales = value of CONFIRMED and COMPLETED bookings with an event date in the month. */
    public double getTotalSales(String yearMonth) {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM bookings "
                + "WHERE substr(event_date, 1, 7) = ? AND booking_status IN ('CONFIRMED', 'COMPLETED')";
        return scalar(sql, yearMonth);
    }

    /** Payments actually received (payment_date) during the month. */
    public double getPaymentsCollected(String yearMonth) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE substr(payment_date, 1, 7) = ?";
        return scalar(sql, yearMonth);
    }

    private double scalar(String sql, String param) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return 0;
        }
    }
}
