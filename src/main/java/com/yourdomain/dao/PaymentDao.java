package com.yourdomain.dao;

import com.yourdomain.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Manual payment recording (no online gateway) with audit logging. */
public class PaymentDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    /** Records a down payment or full payment. Returns the payment ID or -1. */
    public int recordPayment(int receivedBy, int bookingId, double amount, String method, String type) {
        String sql = "INSERT INTO payments (booking_id, amount, payment_method, payment_type, received_by) "
                + "VALUES (?, ?, ?, ?, ?)";
        int newId = -1;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bookingId);
            ps.setDouble(2, amount);
            ps.setString(3, method);
            ps.setString(4, type);
            ps.setInt(5, receivedBy);
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
            logDao.logAction(receivedBy, "PAYMENT_RECORDED",
                    String.format("Payment #%d: P%.2f (%s) via %s for Booking #%d",
                            newId, amount, type, method, bookingId));
        }
        return newId;
    }

    public List<Payment> getPaymentsByBooking(int bookingId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT pay.payment_id, pay.booking_id, pay.amount, pay.payment_method, "
                + "pay.payment_type, pay.payment_date, u.full_name "
                + "FROM payments pay JOIN users u ON u.id = pay.received_by "
                + "WHERE pay.booking_id = ? ORDER BY pay.payment_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Payment(rs.getInt(1), rs.getInt(2), rs.getDouble(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getString(7)));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return list;
    }
}
