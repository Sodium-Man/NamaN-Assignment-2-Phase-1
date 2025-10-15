package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectionTest {
    private static final String DB_URL = "jdbc:sqlite:carehome.db";

    public static void main(String[] args) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
            if (conn != null) {
                System.out.println("Successfully connected to the database!");
                // Optional: Create a table to verify
                try (var stmt = conn.createStatement()) {
                    stmt.execute("CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY)");
                    System.out.println("Test table created or already exists.");
                }
            } else {
                System.out.println("Failed to connect to the database.");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("SQLite JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
}