package application;

import java.util.List;
import java.util.Map;

/**
 * Main application class to demonstrate the Student Score Management System
 * Integrates database connection, data validation, and insertion
 */
public class StudentScoreApp {
    
    public static void main(String[] args) {
        System.out.println("========== Student Score Management System ==========\n");
        
        try {
            // Step 1: Connect to the database
            System.out.println("Step 1: Connecting to database...");
            if (DatabaseConnection.connectToDatabase() == null) {
                System.err.println("Failed to connect to database. Exiting...");
                return;
            }
            System.out.println("Connection successful!\n");
            
            // Step 2: Validate data from CSV file
            System.out.println("Step 2: Reading and validating CSV data...");
            StudentDataValidator validator = new StudentDataValidator();
            
            // Insert into student_score table (contains: student_id, course_id, score, result)
            String tableName = "student_score";
            
            if (!validator.readAndValidateData(tableName)) {
                System.err.println("Data validation failed!");
                validator.printValidationReport();
                return;
            }
            
            validator.printValidationReport();
            
            // Step 3: Insert validated data into database
            System.out.println("Step 3: Inserting validated data into database...");
            List<Map<String, String>> validatedData = validator.getValidatedData();
            
            if (validatedData.isEmpty()) {
                System.out.println("No valid data to insert");
                return;
            }
            
            StudentDataInsert.InsertResult result = StudentDataInsert.insertDataWithReport(validatedData, tableName);
            
            System.out.println("\n========== Insertion Report ==========");
            System.out.println("Status: " + result.getStatus());
            System.out.println("Message: " + result.getMessage());
            System.out.println("Total Rows: " + result.getTotalRows());
            System.out.println("Rows Inserted: " + result.getRowsInserted());
            System.out.println("=====================================\n");
            
            // Step 4: Generate one text file per course
            System.out.println("Step 4: Generating course result files...");
            StudentDataInsert.createCourseResultFiles();
            System.out.println("Course result files generated successfully.");
            
            // Step 5: Generate a combined report for all students and all courses
            System.out.println("Step 5: Generating all-student course summary...");
            StudentDataInsert.createAllStudentCourseReport();
            System.out.println("All-student course summary generated successfully.");
            
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close database connection
            DatabaseConnection.closeConnection();
            System.out.println("Application terminated");
        }
    }
    
    /**
     * Alternative method showing step-by-step usage
     */
    public static void demonstrateStepByStep() {
        // Step 1: Connect to database
        if (DatabaseConnection.connectToDatabase() != null) {
            
            // Step 2: Validate CSV data
            StudentDataValidator validator = new StudentDataValidator();
            if (validator.readAndValidateData("student_scores")) {
                
                // Step 3: Insert validated data
                List<Map<String, String>> data = validator.getValidatedData();
                int inserted = StudentDataInsert.insertValidatedData(data, "student_scores");
                System.out.println("Inserted " + inserted + " rows successfully");
                
            } else {
                System.out.println("Validation errors:");
                for (String error : validator.getValidationErrors()) {
                    System.out.println("  " + error);
                }
            }
        }
        
        DatabaseConnection.closeConnection();
    }
}