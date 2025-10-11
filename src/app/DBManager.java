package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DBManager {
    public static final String DB_URL = "jdbc:sqlite:carehome.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found: " + e.getMessage());
        }
    }

    public static void archiveResident(Resident r) throws SQLException {
        String sql = "INSERT INTO residents (id, name, gender, condition, admission_date, discharge_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, r.getResidentId());
            pstmt.setString(2, r.getName());
            pstmt.setString(3, r.getGender().toString());
            pstmt.setString(4, r.getCondition());
            pstmt.setString(5, r.getAdmissionDate().toString());
            pstmt.setString(6, r.getDischargeDate() != null ? r.getDischargeDate().toString() : null);
            pstmt.executeUpdate();
        }
    }

    public static void archivePrescription(String residentId, Prescription p) throws SQLException {
        String sql = "INSERT INTO prescriptions (resident_id, medicine, dose, time) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false); // Use transaction for atomicity
            try {
                for (Prescription.PrescriptionItemData item : p.getPrescriptionItemsData()) {
                    pstmt.setString(1, residentId);
                    pstmt.setString(2, item.getMedicine().getName());
                    pstmt.setString(3, item.getDose());
                    pstmt.setString(4, item.getTime() != null ? item.getTime().toString() : null);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static void archiveAdministration(String residentId, Administration a) throws SQLException {
        String sql = "INSERT INTO administrations (resident_id, medicine, admin_time) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, residentId);
            pstmt.setString(2, a.getMedicine().getName());
            pstmt.setString(3, a.getAdminTime().toString());
            pstmt.executeUpdate();
        }
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS residents (" +
                    "id TEXT PRIMARY KEY, name TEXT, gender TEXT, condition TEXT, admission_date TEXT, discharge_date TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS prescriptions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, resident_id TEXT, medicine TEXT, dose TEXT, time TEXT, " +
                    "FOREIGN KEY (resident_id) REFERENCES residents(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS administrations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, resident_id TEXT, medicine TEXT, admin_time TEXT, " +
                    "FOREIGN KEY (resident_id) REFERENCES residents(id))");
        }
    }

    public static Map<String, String> getResidentDetails(String residentId) throws SQLException {
        Map<String, String> details = new HashMap<>();
        String sql = "SELECT name, gender, condition FROM residents WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, residentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    details.put("name", rs.getString("name"));
                    details.put("gender", rs.getString("gender"));
                    details.put("condition", rs.getString("condition"));
                }
            }
        }
        return details;
    }
}