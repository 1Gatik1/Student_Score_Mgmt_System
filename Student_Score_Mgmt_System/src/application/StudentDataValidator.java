package application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * StudentDataValidator class to read and validate student data from CSV file
 */
public class StudentDataValidator {
    
    private static final String INPUT_FOLDER = "E:\\Java Student Mgmt Project\\INPUT_file";
    private String csvFilePath;
    private List<Map<String, String>> validatedData;
    private List<String> validationErrors;
    private Map<String, ColumnMetadata> tableMetadata;
    private Map<String, String> columnMapping;
    
    // Column mapping from CSV headers to database column names
    private static final Map<String, String> DEFAULT_COLUMN_MAPPING = new HashMap<>();
    
    static {
        // Example mappings - adjust these to match your CSV and database columns
        DEFAULT_COLUMN_MAPPING.put("student_id", "student_id");
        DEFAULT_COLUMN_MAPPING.put("StudentId", "student_id");
        DEFAULT_COLUMN_MAPPING.put("courseID", "course_id");
        DEFAULT_COLUMN_MAPPING.put("CourseID", "course_id");
        DEFAULT_COLUMN_MAPPING.put("course_id", "course_id");
        DEFAULT_COLUMN_MAPPING.put("courseName", "course_name");
        DEFAULT_COLUMN_MAPPING.put("CourseName", "course_name");
        DEFAULT_COLUMN_MAPPING.put("course_name", "course_name");
        DEFAULT_COLUMN_MAPPING.put("score", "score");
        DEFAULT_COLUMN_MAPPING.put("Score", "score");
        DEFAULT_COLUMN_MAPPING.put("semester", "semester");
        DEFAULT_COLUMN_MAPPING.put("Semester", "semester");
    }
    
    /**
     * Inner class to store column metadata from database
     */
    private static class ColumnMetadata {
        String columnName;
        String dataType;
        int maxLength;
        boolean isNullable;
        
        ColumnMetadata(String columnName, String dataType, int maxLength, boolean isNullable) {
            this.columnName = columnName;
            this.dataType = dataType;
            this.maxLength = maxLength;
            this.isNullable = isNullable;
        }
    }
    
    public StudentDataValidator() {
        this.validatedData = new ArrayList<>();
        this.validationErrors = new ArrayList<>();
        this.tableMetadata = new HashMap<>();
        this.columnMapping = new HashMap<>(DEFAULT_COLUMN_MAPPING);
        this.csvFilePath = findCsvFile();
    }
    
    /**
     * Find the first CSV file in the INPUT_FOLDER
     * 
     * @return Path to the CSV file, or null if not found
     */
    private String findCsvFile() {
        try {
            Path inputPath = Paths.get(INPUT_FOLDER);
            
            if (!Files.exists(inputPath)) {
                System.err.println("Input folder does not exist: " + INPUT_FOLDER);
                return null;
            }
            
            try (Stream<Path> paths = Files.list(inputPath)) {
                Path csvFile = paths
                    .filter(path -> path.toString().endsWith(".csv") || path.toString().endsWith(".CSV"))
                    .findFirst()
                    .orElse(null);
                
                if (csvFile != null) {
                    System.out.println("CSV file found: " + csvFile.toString());
                    return csvFile.toString();
                } else {
                    System.err.println("No CSV file found in folder: " + INPUT_FOLDER);
                    return null;
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error searching for CSV file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Set custom column mapping
     * 
     * @param customMapping Map of CSV column names to database column names
     */
    public void setColumnMapping(Map<String, String> customMapping) {
        if (customMapping != null) {
            this.columnMapping.putAll(customMapping);
        }
    }
    
    /**
     * Get the mapped database column name for a CSV column
     * 
     * @param csvColumnName CSV column name
     * @return Database column name
     */
    private String getMappedColumnName(String csvColumnName) {
        if (columnMapping.containsKey(csvColumnName)) {
            return columnMapping.get(csvColumnName);
        }
        return csvColumnName;
    }
    
    /**
     * Method to retrieve table metadata (column names, data types, lengths) from database
     * 
     * @param connection Database connection object
     * @param tableName Name of the table
     * @return true if metadata retrieved successfully, false otherwise
     */
    private boolean retrieveTableMetadata(Connection connection, String tableName) {
        try {
            String query = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE " +
                          "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + 
                          "' AND TABLE_SCHEMA = 'student_score'";
            
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                String dataType = resultSet.getString("DATA_TYPE");
                int maxLength = resultSet.getObject("CHARACTER_MAXIMUM_LENGTH") != null ? 
                                resultSet.getInt("CHARACTER_MAXIMUM_LENGTH") : 0;
                boolean isNullable = "YES".equalsIgnoreCase(resultSet.getString("IS_NULLABLE"));
                
                tableMetadata.put(columnName, new ColumnMetadata(columnName, dataType, maxLength, isNullable));
            }
            
            resultSet.close();
            statement.close();
            
            System.out.println("Table metadata retrieved for table: " + tableName);
            return true;
        } catch (SQLException e) {
            System.err.println("Error retrieving table metadata: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Method to validate data against database schema
     * 
     * @param value Value to validate
     * @param metadata Column metadata
     * @return true if valid, false otherwise
     */
    private boolean validateDataType(String value, ColumnMetadata metadata) {
        if (value == null || value.trim().isEmpty()) {
            return metadata.isNullable;
        }
        
        try {
            switch (metadata.dataType.toLowerCase()) {
                case "int":
                case "integer":
                    Integer.parseInt(value.trim());
                    return true;
                    
                case "varchar":
                case "char":
                case "text":
                    String strValue = value.trim();
                    if (metadata.maxLength > 0 && strValue.length() > metadata.maxLength) {
                        return false;
                    }
                    return true;
                    
                case "float":
                case "double":
                case "decimal":
                    Double.parseDouble(value.trim());
                    return true;
                    
                case "date":
                    java.time.LocalDate.parse(value.trim());
                    return true;
                    
                case "boolean":
                case "bit":
                    String boolVal = value.trim().toLowerCase();
                    return boolVal.equals("true") || boolVal.equals("false") || 
                           boolVal.equals("1") || boolVal.equals("0");
                    
                default:
                    return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Method to read and validate data from CSV file
     * 
     * @param tableName Name of the database table for validation reference
     * @return true if validation successful, false otherwise
     */
    public boolean readAndValidateData(String tableName) {
        if (csvFilePath == null) {
            validationErrors.add("CSV file not found in input folder: " + INPUT_FOLDER);
            return false;
        }
        
        Connection connection = DatabaseConnection.getConnection();
        
        if (connection == null) {
            validationErrors.add("Failed to establish database connection");
            return false;
        }
        
        // Retrieve table metadata
        if (!retrieveTableMetadata(connection, tableName)) {
            validationErrors.add("Failed to retrieve table metadata for validation");
            return false;
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            String[] headers = null;
            int rowNumber = 0;
            
            while ((line = br.readLine()) != null) {
                rowNumber++;
                
                // Read headers from first line
                if (headers == null) {
                    headers = line.split(",");
                    System.out.println("CSV Headers: " + String.join(", ", headers));
                    continue;
                }
                
                // Parse CSV row
                String[] values = line.split(",");
                
                if (values.length != headers.length) {
                    validationErrors.add("Row " + rowNumber + ": Column count mismatch. Expected " + 
                                       headers.length + ", found " + values.length);
                    continue;
                }
                
                // Validate each column value
                Map<String, String> rowData = new HashMap<>();
                boolean isRowValid = true;
                
                for (int i = 0; i < headers.length; i++) {
                    String csvColumnName = headers[i].trim();
                    String dbColumnName = getMappedColumnName(csvColumnName);
                    String value = values[i].trim();
                    
                    if (tableMetadata.containsKey(dbColumnName)) {
                        ColumnMetadata metadata = tableMetadata.get(dbColumnName);
                        
                        if (!validateDataType(value, metadata)) {
                            validationErrors.add("Row " + rowNumber + ", Column '" + csvColumnName + 
                                               "': Invalid data type or length. Value: " + value);
                            isRowValid = false;
                        }
                    }
                    
                    // Store with CSV column name as key for later retrieval
                    rowData.put(csvColumnName, value);
                }
                
                if (isRowValid) {
                    validatedData.add(rowData);
                    System.out.println("Row " + rowNumber + " validated successfully");
                }
            }
            
            System.out.println("CSV file processed. Total valid rows: " + validatedData.size());
            return validationErrors.isEmpty();
            
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            validationErrors.add("Error reading CSV file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Method to get validated data
     * 
     * @return List of maps containing validated row data
     */
    public List<Map<String, String>> getValidatedData() {
        return validatedData;
    }
    
    /**
     * Method to get validation errors
     * 
     * @return List of validation error messages
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    /**
     * Method to print validation report
     */
    public void printValidationReport() {
        System.out.println("\n========== VALIDATION REPORT ==========");
        System.out.println("CSV File: " + csvFilePath);
        System.out.println("Total valid rows: " + validatedData.size());
        System.out.println("Total errors: " + validationErrors.size());
        
        if (!validationErrors.isEmpty()) {
            System.out.println("\nErrors found:");
            for (String error : validationErrors) {
                System.out.println("  - " + error);
            }
        }
        System.out.println("========================================\n");
    }
}