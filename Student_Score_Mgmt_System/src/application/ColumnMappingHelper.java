package application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to help identify and configure column mappings
 */
public class ColumnMappingHelper {
    
    private static final String CSV_FILE_PATH = "E:\\Java Student Mgmt Project\\student_course_scores.csv";
    
    /**
     * Read and display all CSV column headers
     */
    public static List<String> getCSVHeaders() {
        List<String> headers = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
            String firstLine = br.readLine();
            if (firstLine != null) {
                String[] headerArray = firstLine.split(",");
                for (String header : headerArray) {
                    headers.add(header.trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
        
        return headers;
    }
    
    /**
     * Display CSV headers with suggestions for database column mapping
     */
    public static void displayMappingSuggestions() {
        System.out.println("\n========== CSV COLUMN MAPPING GUIDE ==========");
        System.out.println("CSV Columns found:");
        
        List<String> headers = getCSVHeaders();
        
        for (String header : headers) {
            String suggestion = suggestDatabaseColumnName(header);
            System.out.println("  CSV: '" + header + "' -> Suggested DB Column: '" + suggestion + "'");
        }
        
        System.out.println("\nVerify these column names exist in your database table!");
        System.out.println("If not, update the COLUMN_MAPPING in StudentDataInsert.java and StudentDataValidator.java");
        System.out.println("=============================================\n");
    }
    
    /**
     * Suggest database column name based on CSV column name
     */
    private static String suggestDatabaseColumnName(String csvColumnName) {
        // Convert camelCase to snake_case
        String suggested = csvColumnName
                .replaceAll("([a-z])([A-Z])", "$1_$2")  // Insert underscore before uppercase
                .toLowerCase();
        
        // Common mappings
        if (csvColumnName.equalsIgnoreCase("StudentID") || csvColumnName.equalsIgnoreCase("StudentId")) {
            return "student_id";
        } else if (csvColumnName.equalsIgnoreCase("CourseID") || csvColumnName.equalsIgnoreCase("CourseId")) {
            return "course_id";
        } else if (csvColumnName.equalsIgnoreCase("CourseName")) {
            return "course_name";
        } else if (csvColumnName.equalsIgnoreCase("Score")) {
            return "score";
        } else if (csvColumnName.equalsIgnoreCase("Semester")) {
            return "semester";
        }
        
        return suggested;
    }
}
