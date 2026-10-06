package application;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection class to connect to MySQL database
 */
public class DatabaseConnection {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/student_score";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root101"; // Update with your MySQL password
    private static Connection connection = null;
    
    /**
     * Method to connect to MySQL database 'student_score'
     * 
     * @return Connection object if successful, null otherwise
     */
    public static Connection connectToDatabase() {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Establish connection to the database
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            if (connection != null) {
                System.out.println("Successfully connected to database 'student_score'");
                return connection;
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Method to get existing database connection
     * 
     * @return Connection object
     */
    public static Connection getConnection() {
        if (connection == null) {
            return connectToDatabase();
        }
        return connection;
    }
    
    /**
     * Method to close database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
