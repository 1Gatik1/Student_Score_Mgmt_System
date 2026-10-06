/**
 * Module definition for Student Score Management System
 * This module handles database connectivity, data validation, and insertion
 */
module Student_Score_Mgmt_System {
    // Requires Java SQL modules for database operations
    requires java.sql;
    requires junit;
    
    // Export all classes in the application package
    exports application;
}