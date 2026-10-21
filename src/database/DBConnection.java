package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection - Manages database connection to MySQL.
 * Automatically supports both XAMPP (blank password) and standard MySQL (password: 'root').
 */
public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/hotel_management?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";

    private static final String USER = "root";

    // Auto-detects whether the environment uses "root" or "" (XAMPP default)
    private static String activePassword = "root";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found in classpath: " + e.getMessage());
        }
    }

    /**
     * Obtains a connection to the MySQL database.
     * Tries the primary password ("root"), and if rejected, automatically falls back to "" (XAMPP).
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, activePassword);
        } catch (SQLException e) {
            // If Access Denied (SQLState 28000), try fallback password
            if ("28000".equals(e.getSQLState()) || e.getErrorCode() == 1045) {
                String fallback = "root".equals(activePassword) ? "" : "root";
                try {
                    Connection conn = DriverManager.getConnection(URL, USER, fallback);
                    activePassword = fallback; // Remember working password
                    return conn;
                } catch (SQLException ignored) {
                }
            }
            throw e;
        }
    }

    /**
     * Utility method to quickly test if MySQL is running and reachable.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
