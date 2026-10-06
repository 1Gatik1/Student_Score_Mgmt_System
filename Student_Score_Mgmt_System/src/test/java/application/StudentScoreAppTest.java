package test.java.application;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import application.StudentScoreApp;

public class StudentScoreAppTest {

    private PrintStream originalOut;
    private PrintStream originalErr;

    @Before
    public void saveConsole() {
        originalOut = System.out;
        originalErr = System.err;
    }

    @After
    public void restoreConsole() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private String captureConsoleOutput(Runnable action) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));

        try {
            action.run();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        return out.toString() + err.toString();
    }

    @Test
    public void main_shouldPrintStartupBannerAndTerminate() {
        String output = captureConsoleOutput(() -> StudentScoreApp.main(new String[0]));

        assertTrue(output.contains("========== Student Score Management System =========="));
        assertTrue(output.contains("Step 1: Connecting to database..."));
        assertTrue(output.contains("Application terminated"));
    }

    @Test
    public void main_shouldAttemptDatabaseConnection() {
        String output = captureConsoleOutput(() -> StudentScoreApp.main(new String[0]));

        assertTrue(output.contains("Step 1: Connecting to database..."));
        assertTrue(output.contains("Connection successful!")
                || output.contains("Failed to connect to database")
                || output.contains("MySQL JDBC Driver not found"));
    }

    @Test
    public void demonstrateStepByStep_shouldNotThrow() {
        try {
            StudentScoreApp.demonstrateStepByStep();
        } catch (Exception e) {
            fail("demonstrateStepByStep threw an exception: " + e.getMessage());
        }
    }
}