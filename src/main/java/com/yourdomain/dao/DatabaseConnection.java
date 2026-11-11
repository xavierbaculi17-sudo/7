package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Opens SQLite connections with foreign key constraints enabled. */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:students.db";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");   // must be set on every SQLite connection
        }
        return conn;
    }
}
