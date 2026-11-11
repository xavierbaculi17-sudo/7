package com.yourdomain.dao;

import com.yourdomain.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates every table on startup (if missing) and seeds sample data on a fresh database. */
public class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        String users = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT NOT NULL UNIQUE, "
                + "password_hash TEXT NOT NULL, "
                + "full_name TEXT NOT NULL, "
                + "contact_no TEXT, "
                + "email TEXT, "
                + "roles TEXT NOT NULL, "
                + "status TEXT NOT NULL)";

        String packages = "CREATE TABLE IF NOT EXISTS packages ("
                + "package_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "package_name TEXT NOT NULL, "
                + "description TEXT, "
                + "event_type TEXT NOT NULL, "
                + "price_per_head REAL NOT NULL, "
                + "min_guests INTEGER NOT NULL, "
                + "status TEXT NOT NULL DEFAULT 'ACTIVE')";

        String bookings = "CREATE TABLE IF NOT EXISTS bookings ("
                + "booking_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "customer_id INTEGER NOT NULL, "
                + "package_id INTEGER NOT NULL, "
                + "event_type TEXT NOT NULL, "
                + "event_date TEXT NOT NULL, "
                + "venue TEXT NOT NULL, "
                + "guest_count INTEGER NOT NULL, "
                + "total_amount REAL NOT NULL, "
                + "booking_status TEXT NOT NULL DEFAULT 'PENDING', "
                + "date_created TEXT DEFAULT (datetime('now','localtime')), "
                + "FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE, "
                + "FOREIGN KEY (package_id) REFERENCES packages(package_id))";

        String payments = "CREATE TABLE IF NOT EXISTS payments ("
                + "payment_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "booking_id INTEGER NOT NULL, "
                + "amount REAL NOT NULL, "
                + "payment_method TEXT NOT NULL, "
                + "payment_type TEXT NOT NULL, "
                + "payment_date TEXT DEFAULT (datetime('now','localtime')), "
                + "received_by INTEGER NOT NULL, "
                + "FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE, "
                + "FOREIGN KEY (received_by) REFERENCES users(id))";

        // Audit trail (Deliverable 8). Plays the role of "audit_logs" in the proposal.
        String logs = "CREATE TABLE IF NOT EXISTS activity_logs ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "user_id INTEGER NOT NULL, "
                + "action_type TEXT NOT NULL, "
                + "description TEXT NOT NULL, "
                + "timestamp DATETIME DEFAULT (datetime('now','localtime')), "
                + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute(users);
                st.execute(packages);
                st.execute(bookings);
                st.execute(payments);
                st.execute(logs);
            }
            seedUsers(conn);
            seedPackages(conn);
        } catch (SQLException e) {
            System.out.println("Database initialization error: " + e.getMessage());
        }
    }

    private static boolean isEmpty(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void seedUsers(Connection conn) throws SQLException {
        if (!isEmpty(conn, "SELECT COUNT(*) FROM users")) {
            return;
        }
        String sql = "INSERT INTO users (username, password_hash, full_name, contact_no, email, roles, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String[][] seed = {
                {"admin",     "admin123", "Francis Baculi",   "09170000001", "owner@catering.com",  "ADMIN",    "APPROVED"},
                {"staff1",    "staff123", "Maria Santos",     "09170000002", "staff@catering.com",  "STAFF",    "APPROVED"},
                {"customer1", "cust123",  "Juan Dela Cruz",   "09170000003", "juan@mail.com",       "CUSTOMER", "APPROVED"},
                {"customer2", "cust123",  "Ana Reyes",        "09170000004", "ana@mail.com",        "CUSTOMER", "PENDING"}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] u : seed) {
                ps.setString(1, u[0]);
                ps.setString(2, PasswordUtil.hash(u[1]));
                ps.setString(3, u[2]);
                ps.setString(4, u[3]);
                ps.setString(5, u[4]);
                ps.setString(6, u[5]);
                ps.setString(7, u[6]);
                ps.executeUpdate();
            }
        }
    }

    private static void seedPackages(Connection conn) throws SQLException {
        if (!isEmpty(conn, "SELECT COUNT(*) FROM packages")) {
            return;
        }
        String sql = "INSERT INTO packages (package_name, description, event_type, price_per_head, min_guests) "
                + "VALUES (?, ?, ?, ?, ?)";
        Object[][] seed = {
                {"Wedding Feast Package", "Roast pork, beef caldereta, chicken cordon bleu, pasta, rice, dessert bar, drinks",
                        "WEDDING", 650.00, 50},
                {"Birthday Party Package", "Fried chicken, spaghetti, pancit, lumpia, rice, cake, juice",
                        "BIRTHDAY", 350.00, 20},
                {"Corporate Lunch Package", "Buffet lunch: 2 mains, 1 pasta, rice, salad, dessert, coffee",
                        "CORPORATE", 450.00, 30},
                {"Debut Celebration Package", "Appetizer trio, 3 mains, pasta, rice, dessert station, mocktails",
                        "DEBUT", 550.00, 50}
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] p : seed) {
                ps.setString(1, (String) p[0]);
                ps.setString(2, (String) p[1]);
                ps.setString(3, (String) p[2]);
                ps.setDouble(4, (Double) p[3]);
                ps.setInt(5, (Integer) p[4]);
                ps.executeUpdate();
            }
        }
    }
}
