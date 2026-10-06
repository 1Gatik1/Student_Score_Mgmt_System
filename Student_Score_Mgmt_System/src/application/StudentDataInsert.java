package application;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StudentDataInsert class to insert validated student data into database
 */
public class StudentDataInsert {
    
    // Column mapping from CSV headers to database column names
    // Modify this map to match your CSV columns to database columns
    private static final Map<String, String> COLUMN_MAPPING = new HashMap<>();
    
    static {
        // CSV to Database column mappings
        COLUMN_MAPPING.put("student_id", "student_id");
        COLUMN_MAPPING.put("StudentId", "student_id");
        COLUMN_MAPPING.put("studentId", "student_id");
        COLUMN_MAPPING.put("courseID", "course_id");
        COLUMN_MAPPING.put("CourseID", "course_id");
        COLUMN_MAPPING.put("courseId", "course_id");
        COLUMN_MAPPING.put("course_id", "course_id");
        COLUMN_MAPPING.put("courseName", "course_name");
        COLUMN_MAPPING.put("CourseName", "course_name");
        COLUMN_MAPPING.put("course_name", "course_name");
        COLUMN_MAPPING.put("score", "score");
        COLUMN_MAPPING.put("Score", "score");
        COLUMN_MAPPING.put("result", "result");
        COLUMN_MAPPING.put("Result", "result");
        COLUMN_MAPPING.put("semester", "semester");
        COLUMN_MAPPING.put("Semester", "semester");
    }
    
    /**
     * Get all column names from a database table
     * 
     * @param connection Database connection
     * @param tableName Table name
     * @return Set of column names in the table
     */
    private static Set<String> getTableColumns(Connection connection, String tableName) {
        Set<String> columns = new HashSet<>();
        
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet rs = metaData.getColumns(null, null, tableName, null);
            
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error getting table columns: " + e.getMessage());
        }
        
        return columns;
    }
    
    /**
     * Map CSV column names to database column names
     * 
     * @param csvHeaders CSV column headers
     * @return Mapped database column names
     */
    public static String[] mapColumnNames(String[] csvHeaders) {
        String[] mappedColumns = new String[csvHeaders.length];
        for (int i = 0; i < csvHeaders.length; i++) {
            String csvCol = csvHeaders[i].trim();
            // Use mapping if available, otherwise use the CSV column name as-is
            mappedColumns[i] = COLUMN_MAPPING.getOrDefault(csvCol, csvCol);
            System.out.println("Mapped: '" + csvCol + "' -> '" + mappedColumns[i] + "'");
        }
        return mappedColumns;
    }
    
    /**
     * Method to insert validated data into database table
     * 
     * @param validatedData List of maps containing validated row data
     * @param tableName Name of the database table to insert data into
     * @return Number of rows inserted successfully
     */
    public static int insertValidatedData(List<Map<String, String>> validatedData, String tableName) {
        return insertValidatedData(validatedData, tableName, null);
    }
    
    /**
     * Method to insert validated data with column mapping
     * Only inserts columns that exist in the target database table
     * 
     * @param validatedData List of maps containing validated row data
     * @param tableName Name of the database table to insert data into
     * @param columnMapping Map to map CSV columns to database columns
     * @return Number of rows inserted successfully
     */
    public static int insertValidatedData(List<Map<String, String>> validatedData, String tableName, Map<String, String> columnMapping) {
        if (validatedData == null || validatedData.isEmpty()) {
            System.out.println("No data to insert");
            return 0;
        }
        
        Connection connection = DatabaseConnection.getConnection();
        
        if (connection == null) {
            System.err.println("Failed to establish database connection for insertion");
            return 0;
        }
        
        int rowsInserted = 0;
        
        try {
            // Get all valid columns in the target table
            Set<String> tableColumns = getTableColumns(connection, tableName);
            System.out.println("Target table '" + tableName + "' columns: " + tableColumns);
            
            // Start transaction
            connection.setAutoCommit(false);
            
            // Get column names from first row
            Map<String, String> firstRow = validatedData.get(0);
            String[] csvColumnNames = firstRow.keySet().toArray(new String[0]);
            
            // Map CSV columns to database columns, but only keep columns that exist in table
            List<String> validCsvColumns = new ArrayList<>();
            List<String> validDbColumns = new ArrayList<>();
            
            for (String csvCol : csvColumnNames) {
                String dbCol = csvCol;
                
                // Apply mapping if available
                if (columnMapping != null && columnMapping.containsKey(csvCol)) {
                    dbCol = columnMapping.get(csvCol);
                } else if (COLUMN_MAPPING.containsKey(csvCol)) {
                    dbCol = COLUMN_MAPPING.get(csvCol);
                }
                
                // Only include if column exists in table
                if (tableColumns.contains(dbCol.toLowerCase())) {
                    validCsvColumns.add(csvCol);
                    validDbColumns.add(dbCol);
                    System.out.println("Including column: CSV '" + csvCol + "' -> DB '" + dbCol + "'");
                } else {
                    System.out.println("Skipping column: CSV '" + csvCol + "' -> DB '" + dbCol + "' (not in table)");
                }
            }
            
            // Ensure result is generated automatically when table contains a result enum and CSV does not provide it.
            if (tableColumns.contains("result") && !validDbColumns.contains("result")) {
                validCsvColumns.add("score");
                validDbColumns.add("result");
                System.out.println("Auto-generating result column using score >= 40 => pass, else fail");
            }
            
            if (validDbColumns.isEmpty()) {
                System.err.println("No valid columns found to insert!");
                return 0;
            }
            
            // Build INSERT query
            StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tableName + " (");
            for (int i = 0; i < validDbColumns.size(); i++) {
                insertQuery.append(validDbColumns.get(i));
                if (i < validDbColumns.size() - 1) {
                    insertQuery.append(", ");
                }
            }
            insertQuery.append(") VALUES (");
            for (int i = 0; i < validDbColumns.size(); i++) {
                insertQuery.append("?");
                if (i < validDbColumns.size() - 1) {
                    insertQuery.append(", ");
                }
            }
            insertQuery.append(")");
            
            System.out.println("Executing query: " + insertQuery.toString());
            
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery.toString());
            
            // Insert each row
            for (Map<String, String> row : validatedData) {
                int paramIndex = 1;
                
                for (int i = 0; i < validDbColumns.size(); i++) {
                    String csvColumnName = validCsvColumns.get(i);
                    String dbColumnName = validDbColumns.get(i);
                    // Retrieve value using CSV column name (the key in the map)
                    String value = getInsertValue(row, csvColumnName, dbColumnName);
                    
                    if (value == null || value.trim().isEmpty()) {
                        preparedStatement.setNull(paramIndex, java.sql.Types.VARCHAR);
                    } else {
                        preparedStatement.setString(paramIndex, value.trim());
                    }
                    paramIndex++;
                }
                
                try {
                    preparedStatement.addBatch();
                } catch (SQLException e) {
                    System.err.println("Error adding row to batch: " + e.getMessage());
                }
            }
            
            // Execute batch insert
            int[] results = preparedStatement.executeBatch();
            rowsInserted = results.length;
            
            // Commit transaction
            connection.commit();
            connection.setAutoCommit(true);
            
            preparedStatement.close();
            
            System.out.println("Successfully inserted " + rowsInserted + " rows into table '" + tableName + "'");
            
        } catch (SQLException e) {
            System.err.println("Error inserting data into database: " + e.getMessage());
            e.printStackTrace();
            
            try {
                // Rollback on error
                connection.rollback();
                connection.setAutoCommit(true);
                System.out.println("Transaction rolled back");
            } catch (SQLException rollbackException) {
                System.err.println("Error rolling back transaction: " + rollbackException.getMessage());
            }
        }
        
        return rowsInserted;
    }
    
    /**
     * Method to insert data with error handling and detailed reporting
     * 
     * @param validatedData List of maps containing validated row data
     * @param tableName Name of the database table
     * @return InsertResult object containing insertion details
     */
    public static InsertResult insertDataWithReport(List<Map<String, String>> validatedData, String tableName) {
        return insertDataWithReport(validatedData, tableName, null);
    }
    
    /**
     * Method to insert data with error handling, detailed reporting, and column mapping
     * Only inserts columns that exist in the target database table
     * 
     * @param validatedData List of maps containing validated row data
     * @param tableName Name of the database table
     * @param columnMapping Map to map CSV columns to database columns
     * @return InsertResult object containing insertion details
     */
    public static InsertResult insertDataWithReport(List<Map<String, String>> validatedData, String tableName, Map<String, String> columnMapping) {
        InsertResult result = new InsertResult();
        result.setTotalRows(validatedData.size());
        
        if (validatedData == null || validatedData.isEmpty()) {
            result.setStatus("EMPTY");
            result.setMessage("No data to insert");
            return result;
        }
        
        Connection connection = DatabaseConnection.getConnection();
        
        if (connection == null) {
            result.setStatus("FAILED");
            result.setMessage("Failed to establish database connection");
            return result;
        }
        
        try {
            // Get all valid columns in the target table
            Set<String> tableColumns = getTableColumns(connection, tableName);
            System.out.println("Target table '" + tableName + "' columns: " + tableColumns);
            
            connection.setAutoCommit(false);
            
            // Get column names from first row
            Map<String, String> firstRow = validatedData.get(0);
            String[] csvColumnNames = firstRow.keySet().toArray(new String[0]);
            
            // Map CSV columns to database columns, but only keep columns that exist in table
            List<String> validCsvColumns = new ArrayList<>();
            List<String> validDbColumns = new ArrayList<>();
            
            for (String csvCol : csvColumnNames) {
                String dbCol = csvCol;
                
                // Apply mapping if available
                if (columnMapping != null && columnMapping.containsKey(csvCol)) {
                    dbCol = columnMapping.get(csvCol);
                } else if (COLUMN_MAPPING.containsKey(csvCol)) {
                    dbCol = COLUMN_MAPPING.get(csvCol);
                }
                
                // Only include if column exists in table
                if (tableColumns.contains(dbCol.toLowerCase())) {
                    validCsvColumns.add(csvCol);
                    validDbColumns.add(dbCol);
                    System.out.println("Including column: CSV '" + csvCol + "' -> DB '" + dbCol + "'");
                } else {
                    System.out.println("Skipping column: CSV '" + csvCol + "' -> DB '" + dbCol + "' (not in table)");
                }
            }
            
            // Ensure result is generated automatically when table contains a result enum and CSV does not provide it.
            if (tableColumns.contains("result") && !validDbColumns.contains("result")) {
                validCsvColumns.add("score");
                validDbColumns.add("result");
                System.out.println("Auto-generating result column using score >= 40 => pass, else fail");
            }
            
            if (validDbColumns.isEmpty()) {
                result.setStatus("FAILED");
                result.setMessage("No valid columns found to insert!");
                return result;
            }
            
            StringBuilder insertQuery = new StringBuilder("INSERT INTO " + tableName + " (");
            for (int i = 0; i < validDbColumns.size(); i++) {
                insertQuery.append(validDbColumns.get(i));
                if (i < validDbColumns.size() - 1) {
                    insertQuery.append(", ");
                }
            }
            insertQuery.append(") VALUES (");
            for (int i = 0; i < validDbColumns.size(); i++) {
                insertQuery.append("?");
                if (i < validDbColumns.size() - 1) {
                    insertQuery.append(", ");
                }
            }
            insertQuery.append(")");
            
            System.out.println("Executing query: " + insertQuery.toString());
            
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery.toString());
            
            int batchSize = 0;
            for (Map<String, String> row : validatedData) {
                int paramIndex = 1;
                
                for (int i = 0; i < validDbColumns.size(); i++) {
                    String csvColumnName = validCsvColumns.get(i);
                    String dbColumnName = validDbColumns.get(i);
                    // Retrieve value using CSV column name (the key in the map)
                    String value = getInsertValue(row, csvColumnName, dbColumnName);
                    if (value == null || value.trim().isEmpty()) {
                        preparedStatement.setNull(paramIndex, java.sql.Types.VARCHAR);
                    } else {
                        preparedStatement.setString(paramIndex, value.trim());
                    }
                    paramIndex++;
                }
                
                preparedStatement.addBatch();
                batchSize++;
                
                // Execute batch every 1000 rows
                if (batchSize % 1000 == 0) {
                    preparedStatement.executeBatch();
                }
            }
            
            // Execute remaining batch
            preparedStatement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);
            
            preparedStatement.close();
            
            result.setStatus("SUCCESS");
            result.setRowsInserted(validatedData.size());
            result.setMessage("Successfully inserted " + validatedData.size() + " rows");
            
        } catch (SQLException e) {
            System.err.println("Error during insertion: " + e.getMessage());
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackException) {
                System.err.println("Error rolling back: " + rollbackException.getMessage());
            }
            result.setStatus("FAILED");
            result.setMessage("Error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Create one .txt result file per course in the format:
     * StudentID | StudentName | Result
     *
     * Files are stored in a folder named "course_results" inside the project root.
     */
    public static void createCourseResultFiles() throws IOException {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) {
            System.out.println("Database connection failed.");
            return;
        }

        String query = "SELECT c.course_name, s.student_id, s.name, ss.result " +
                "FROM student_score ss " +
                "JOIN students s ON s.student_id = ss.student_id " +
                "JOIN courses c ON c.course_id = ss.course_id " +
                "ORDER BY c.course_name, s.student_id";

        Map<String, StringBuilder> courseResults = new HashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String courseName = rs.getString("course_name");
                String studentId = rs.getString("student_id");
                String studentName = rs.getString("name");
                String result = rs.getString("result");

                courseResults.putIfAbsent(courseName, new StringBuilder());
                StringBuilder builder = courseResults.get(courseName);

                if (builder.length() == 0) {
                    builder.append("StudentID | StudentName | Result").append(System.lineSeparator());
                }

                builder.append(studentId)
                       .append(" | ")
                       .append(studentName)
                       .append(" | ")
                       .append(result)
                       .append(System.lineSeparator());
            }

            Path outputDir = Paths.get("course_results");
            Files.createDirectories(outputDir);

            for (Map.Entry<String, StringBuilder> entry : courseResults.entrySet()) {
                String safeFileName = entry.getKey().replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt";
                Path filePath = outputDir.resolve(safeFileName);

                try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                    writer.write(entry.getValue().toString());
                    System.out.println("Created file: " + filePath.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Error writing course report: " + filePath);
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            System.err.println("Error generating course result files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a readable summary report for every student in a single row.
     * The report is saved in a separate folder named "all_student_reports".
     */
    public static void createAllStudentCourseReport() {
        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) {
            System.out.println("Database connection failed.");
            return;
        }

        String query = "SELECT s.student_id, s.name, c.course_name, ss.score, ss.result " +
                "FROM students s " +
                "LEFT JOIN student_score ss ON ss.student_id = s.student_id " +
                "LEFT JOIN courses c ON c.course_id = ss.course_id " +
                "ORDER BY s.student_id, c.course_name";

        Path outputDir = Paths.get("all_student_reports");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Unable to create report folder: " + outputDir.toAbsolutePath());
            e.printStackTrace();
            return;
        }

        Path filePath = outputDir.resolve("all_student_course_scores.txt");

        Map<Integer, String> studentNames = new HashMap<>();
        Map<Integer, List<String>> courseEntries = new HashMap<>();
        int maxCourseNameLength = 0;
        int maxScoreLength = 0;
        int maxResultLength = 0;

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                int studentId = rs.getInt("student_id");
                String studentName = rs.getString("name");
                String courseName = rs.getString("course_name");
                String score = rs.getString("score");
                String result = rs.getString("result");

                if (courseName == null || courseName.trim().isEmpty()) {
                    courseName = "N/A";
                }
                if (score == null || score.trim().isEmpty()) {
                    score = "N/A";
                }
                if (result == null || result.trim().isEmpty()) {
                    result = "N/A";
                }

                studentNames.put(studentId, studentName);
                courseEntries.computeIfAbsent(studentId, id -> new ArrayList<>())
                        .add(courseName + " : " + score + " (" + result + ")");

                maxCourseNameLength = Math.max(maxCourseNameLength, courseName.length());
                maxScoreLength = Math.max(maxScoreLength, score.length());
                maxResultLength = Math.max(maxResultLength, result.length());
            }

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write(String.format("%-10s | %-20s | Course Details%n", "StudentID", "StudentName"));

                for (Map.Entry<Integer, String> entry : studentNames.entrySet()) {
                    int studentId = entry.getKey();
                    String studentName = entry.getValue();
                    List<String> entries = courseEntries.getOrDefault(studentId, new ArrayList<>());

                    List<String> formattedEntries = new ArrayList<>();
                    for (String courseEntry : entries) {
                        String[] parts = courseEntry.split(" : ", 2);
                        String courseName = parts[0];
                        String scoreAndResult = parts[1];
                        String[] scoreResultParts = scoreAndResult.split(" \\(", 2);
                        String scoreValue = scoreResultParts[0];
                        String resultValue = scoreResultParts[1].replace(")", "");

                        String formattedEntry = String.format("%-" + (maxCourseNameLength + 2) + "s: %-" +
                                (maxScoreLength + 2) + "s (%-" + (maxResultLength + 2) + "s)",
                                courseName, scoreValue, resultValue);
                        formattedEntries.add(formattedEntry);
                    }

                    String details = String.join(" | ", formattedEntries);
                    writer.write(String.format("%-10s | %-20s | %s%n", studentId, studentName, details));
                }

                System.out.println("Created file: " + filePath.toAbsolutePath());
            }

        } catch (SQLException e) {
            System.err.println("Error generating all student course report: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error writing student summary report: " + filePath);
            e.printStackTrace();
        }
    }

    /**
     * Compute result from score based on business rule:
     * pass for score >= 40, fail for score < 40
     *
     * @param scoreText Score value from CSV
     * @return "pass" or "fail"
     */
    private static String determineResult(String scoreText) {
        if (scoreText == null) {
            return "fail";
        }
        try {
            double score = Double.parseDouble(scoreText.trim());
            return score >= 40 ? "pass" : "fail";
        } catch (NumberFormatException e) {
            return "fail";
        }
    }

    /**
     * Get value for a target column.
     * If the target column is result, it is calculated from score.
     *
     * @param row Current CSV row data
     * @param csvColumnName CSV column for this value
     * @param dbColumnName Database column for this value
     * @return Value to insert
     */
    private static String getInsertValue(Map<String, String> row, String csvColumnName, String dbColumnName) {
        if (dbColumnName.equalsIgnoreCase("result")) {
            return determineResult(row.get("score"));
        }
        return row.get(csvColumnName);
    }

    /**
     * Inner class to hold insertion result details
     */
    public static class InsertResult {
        private String status;
        private String message;
        private int totalRows;
        private int rowsInserted;
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public int getTotalRows() {
            return totalRows;
        }
        
        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }
        
        public int getRowsInserted() {
            return rowsInserted;
        }
        
        public void setRowsInserted(int rowsInserted) {
            this.rowsInserted = rowsInserted;
        }
        
        @Override
        public String toString() {
            return "InsertResult{" +
                    "status='" + status + '\'' +
                    ", message='" + message + '\'' +
                    ", totalRows=" + totalRows +
                    ", rowsInserted=" + rowsInserted +
                    '}';
        }
    }
}
