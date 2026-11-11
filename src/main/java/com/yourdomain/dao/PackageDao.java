package com.yourdomain.dao;

import com.yourdomain.model.CateringPackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Catering package CRUD. Packages are soft-deleted (status DISCONTINUED). */
public class PackageDao {

    private static final String SELECT = "SELECT package_id, package_name, description, event_type, "
            + "price_per_head, min_guests, status FROM packages ";

    private final ActivityLogDao logDao = new ActivityLogDao();

    private CateringPackage map(ResultSet rs) throws SQLException {
        return new CateringPackage(rs.getInt("package_id"), rs.getString("package_name"),
                rs.getString("description"), rs.getString("event_type"),
                rs.getDouble("price_per_head"), rs.getInt("min_guests"), rs.getString("status"));
    }

    /** Adds a package. Returns the new package ID or -1. */
    public int createPackage(int adminId, String name, String description, String eventType,
                             double pricePerHead, int minGuests) {
        String sql = "INSERT INTO packages (package_name, description, event_type, price_per_head, min_guests) "
                + "VALUES (?, ?, ?, ?, ?)";
        int newId = -1;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, eventType.toUpperCase());
            ps.setDouble(4, pricePerHead);
            ps.setInt(5, minGuests);
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
            logDao.logAction(adminId, "PACKAGE_CREATED",
                    String.format("Added package #%d: %s at P%.2f per head", newId, name, pricePerHead));
        }
        return newId;
    }

    public boolean updatePackage(int adminId, int packageId, String name, String description,
                                 String eventType, double pricePerHead, int minGuests) {
        String sql = "UPDATE packages SET package_name = ?, description = ?, event_type = ?, "
                + "price_per_head = ?, min_guests = ? WHERE package_id = ?";
        boolean ok = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, eventType.toUpperCase());
            ps.setDouble(4, pricePerHead);
            ps.setInt(5, minGuests);
            ps.setInt(6, packageId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        if (ok) {
            logDao.logAction(adminId, "PACKAGE_UPDATED",
                    String.format("Edited package #%d: %s at P%.2f per head", packageId, name, pricePerHead));
        }
        return ok;
    }

    /** Soft delete a package (discontinue). Existing bookings keep their reference. */
    public boolean discontinuePackage(int adminId, int packageId) {
        String sql = "UPDATE packages SET status = 'DISCONTINUED' WHERE package_id = ? AND status = 'ACTIVE'";
        boolean ok = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, packageId);
            ok = ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        if (ok) {
            logDao.logAction(adminId, "PACKAGE_REMOVED",
                    String.format("Admin %d discontinued package #%d", adminId, packageId));
        }
        return ok;
    }

    public CateringPackage findById(int packageId) {
        String sql = SELECT + "WHERE package_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, packageId);
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

    /** Every package, including discontinued ones (admin view). */
    public List<CateringPackage> getAllPackages() {
        return run(SELECT + "ORDER BY package_id", new ArrayList<>());
    }

    /** Active packages, optionally filtered by event type (contains) and maximum price per head. */
    public List<CateringPackage> searchActivePackages(String eventType, Double maxPricePerHead) {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE status = 'ACTIVE'");
        List<Object> params = new ArrayList<>();
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND LOWER(event_type) LIKE ?");
            params.add("%" + eventType.trim().toLowerCase() + "%");
        }
        if (maxPricePerHead != null) {
            sql.append(" AND price_per_head <= ?");
            params.add(maxPricePerHead);
        }
        sql.append(" ORDER BY package_id");
        return run(sql.toString(), params);
    }

    private List<CateringPackage> run(String sql, List<Object> params) {
        List<CateringPackage> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
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
}
