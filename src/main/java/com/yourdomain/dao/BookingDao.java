package com.yourdomain.dao;

import com.yourdomain.model.Booking;
import com.yourdomain.model.CateringPackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Booking CRUD, calendar queries and audit logging of every booking event. */
public class BookingDao {

    public static final Set<String> VALID_STATUSES = Set.of("PENDING", "CONFIRMED", "COMPLETED", "CANCELLED");

    private static final String SELECT = "SELECT b.booking_id, b.customer_id, u.full_name, b.package_id, "
            + "p.package_name, b.event_type, b.event_date, b.venue, b.guest_count, b.total_amount, "
            + "b.booking_status, "
            + "COALESCE((SELECT SUM(pay.amount) FROM payments pay WHERE pay.booking_id = b.booking_id), 0) AS paid "
            + "FROM bookings b "
            + "JOIN users u ON u.id = b.customer_id "
            + "JOIN packages p ON p.package_id = b.package_id ";

    private final ActivityLogDao logDao = new ActivityLogDao();

    private Booking map(ResultSet rs) throws SQLException {
        return new Booking(rs.getInt("booking_id"), rs.getInt("customer_id"), rs.getString("full_name"),
                rs.getInt("package_id"), rs.getString("package_name"), rs.getString("event_type"),
                rs.getString("event_date"), rs.getString("venue"), rs.getInt("guest_count"),
                rs.getDouble("total_amount"), rs.getString("booking_status"), rs.getDouble("paid"));
    }

    private List<Booking> query(String sql, Object... params) {
        List<Booking> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
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

    // ---------------- CREATE ----------------

    /** Core business process: a customer submits a booking request. Returns the booking ID or -1. */
    public int createBooking(int customerId, CateringPackage pkg, String eventDate,
                             String venue, int guestCount) {
        double total = guestCount * pkg.pricePerHead();
        String sql = "INSERT INTO bookings (customer_id, package_id, event_type, event_date, venue, "
                + "guest_count, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        int newId = -1;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setInt(2, pkg.id());
            ps.setString(3, pkg.eventType());
            ps.setString(4, eventDate);
            ps.setString(5, venue);
            ps.setInt(6, guestCount);
            ps.setDouble(7, total);
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
            logDao.logAction(customerId, "BOOKING_CREATED",
                    String.format("Booking #%d created: %s on %s for %d guests - Total: P%.2f",
                            newId, pkg.name(), eventDate, guestCount, total));
        }
        return newId;
    }

    // ---------------- READ ----------------

    public List<Booking> getAllBookings() {
        return query(SELECT + "ORDER BY b.booking_id");
    }

    public List<Booking> getBookingsByCustomer(int customerId) {
        return query(SELECT + "WHERE b.customer_id = ? ORDER BY b.booking_id", customerId);
    }

    public Booking findById(int bookingId) {
        List<Booking> list = query(SELECT + "WHERE b.booking_id = ?", bookingId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Pending and confirmed bookings between two ISO dates (staff/admin calendar). */
    public List<Booking> getBookingsInRange(String fromDate, String toDate) {
        return query(SELECT + "WHERE b.event_date BETWEEN ? AND ? "
                + "AND b.booking_status IN ('PENDING', 'CONFIRMED') ORDER BY b.event_date, b.booking_id",
                fromDate, toDate);
    }

    /** Confirmed events between two ISO dates (upcoming events report). */
    public List<Booking> getConfirmedInRange(String fromDate, String toDate) {
        return query(SELECT + "WHERE b.event_date BETWEEN ? AND ? "
                + "AND b.booking_status = 'CONFIRMED' ORDER BY b.event_date, b.booking_id",
                fromDate, toDate);
    }

    /** Dates already taken by a CONFIRMED booking (customer calendar: no personal data). */
    public List<String> getConfirmedDates(String fromDate, String toDate) {
        List<String> dates = new ArrayList<>();
        String sql = "SELECT DISTINCT event_date FROM bookings WHERE booking_status = 'CONFIRMED' "
                + "AND event_date BETWEEN ? AND ? ORDER BY event_date";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fromDate);
            ps.setString(2, toDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dates.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return dates;
    }

    /** How many CONFIRMED bookings already use this date (excluding one booking ID). */
    public int countConfirmedOnDate(String eventDate, int excludeBookingId) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE event_date = ? "
                + "AND booking_status = 'CONFIRMED' AND booking_id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventDate);
            ps.setInt(2, excludeBookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return 0;
        }
    }

    // ---------------- UPDATE ----------------

    /** Lifecycle change. Returns null on success, otherwise the reason it was refused. */
    public String updateBookingStatus(int actorId, int bookingId, String newStatus) {
        Booking b = findById(bookingId);
        if (b == null) {
            return "Booking not found.";
        }
        String status = newStatus.toUpperCase();
        if (!VALID_STATUSES.contains(status)) {
            return "Invalid booking status.";
        }
        if (b.status().equals(status)) {
            return "Booking is already " + status + ".";
        }
        if (b.status().equals("CANCELLED") || b.status().equals("COMPLETED")) {
            return "A " + b.status() + " booking can no longer be changed.";
        }
        if (status.equals("CONFIRMED") && countConfirmedOnDate(b.eventDate(), bookingId) > 0) {
            return "Another booking is already CONFIRMED on " + b.eventDate() + " (double booking blocked).";
        }

        String sql = "UPDATE bookings SET booking_status = ? WHERE booking_id = ?";
        boolean ok = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, bookingId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        if (!ok) {
            return "Update failed.";
        }
        logDao.logAction(actorId, "BOOKING_UPDATED",
                String.format("Updated Booking #%d status from %s to %s", bookingId, b.status(), status));
        return null;
    }

    /** Edits date, venue and guest count (total is recalculated at the same price per head). */
    public String updateBookingDetails(int actorId, int bookingId, String eventDate, String venue, int guestCount) {
        Booking b = findById(bookingId);
        if (b == null) {
            return "Booking not found.";
        }
        if (!b.status().equals("PENDING") && !b.status().equals("CONFIRMED")) {
            return "Only PENDING or CONFIRMED bookings can be edited.";
        }
        if (b.status().equals("CONFIRMED") && !eventDate.equals(b.eventDate())
                && countConfirmedOnDate(eventDate, bookingId) > 0) {
            return "Another booking is already CONFIRMED on " + eventDate + ".";
        }

        double newTotal = b.totalAmount() / b.guestCount() * guestCount;
        String sql = "UPDATE bookings SET event_date = ?, venue = ?, guest_count = ?, total_amount = ? "
                + "WHERE booking_id = ?";
        boolean ok = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventDate);
            ps.setString(2, venue);
            ps.setInt(3, guestCount);
            ps.setDouble(4, newTotal);
            ps.setInt(5, bookingId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        if (!ok) {
            return "Update failed.";
        }
        logDao.logAction(actorId, "BOOKING_UPDATED",
                String.format("Edited Booking #%d details: %s, %d guests, Total: P%.2f",
                        bookingId, eventDate, guestCount, newTotal));
        return null;
    }

    // ---------------- DELETE / CANCEL ----------------

    /** A customer cancels their own PENDING booking. */
    public boolean cancelOwnBooking(int customerId, int bookingId) {
        String sql = "UPDATE bookings SET booking_status = 'CANCELLED' "
                + "WHERE booking_id = ? AND customer_id = ? AND booking_status = 'PENDING'";
        boolean ok = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.setInt(2, customerId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        if (ok) {
            logDao.logAction(customerId, "BOOKING_CANCELLED",
                    String.format("Customer %d cancelled Booking #%d", customerId, bookingId));
        }
        return ok;
    }

    /** Admin removes an unconfirmed (PENDING or CANCELLED) booking from the database. */
    public boolean deleteUnconfirmedBooking(int adminId, int bookingId) {
        String sql = "DELETE FROM bookings WHERE booking_id = ? AND booking_status IN ('PENDING', 'CANCELLED')";
        boolean ok = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        if (ok) {
            logDao.logAction(adminId, "BOOKING_DELETED",
                    String.format("Admin %d removed Booking #%d", adminId, bookingId));
        }
        return ok;
    }
}
