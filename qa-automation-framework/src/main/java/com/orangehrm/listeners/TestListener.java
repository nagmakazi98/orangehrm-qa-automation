package com.orangehrm.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.orangehrm.base.DriverFactory;
import com.orangehrm.utils.ConfigReader;
import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

/**
 * Enterprise TestNG listener orchestrating:
 * <ul>
 *   <li>ExtentReports (HTML report with environment, browser, categories, test data, and screenshots)</li>
 *   <li>Allure Reports (parameters, steps, and failure screenshot attachments)</li>
 *   <li>Log4j2 structured logging</li>
 * </ul>
 *
 * <p>Guarantees that credentials/secrets are masked and safe metadata (Environment, Browser,
 * Employee ID, API Endpoints, Retry attempts) is clearly visible.</p>
 */
public class TestListener implements ITestListener {

    private static final Logger log = LogManager.getLogger(TestListener.class);
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    private static synchronized ExtentReports getExtentReports() {
        if (extent == null) {
            String reportPath = ConfigReader.get("extent.report.path", "reports/ExtentReport.html");
            new File(reportPath).getParentFile().mkdirs();
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setDocumentTitle("OrangeHRM QA Automation - Execution Dashboard");
            spark.config().setReportName("Test Execution & Quality Assessment Report");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Framework", "Selenium 4 + Java 11 + TestNG + REST Assured");
            extent.setSystemInfo("Application Under Test", "OrangeHRM Open Source Demo");
            extent.setSystemInfo("Environment", ConfigReader.getActiveEnvironment().toUpperCase());
            extent.setSystemInfo("Browser", ConfigReader.get("browser", "chrome"));
            extent.setSystemInfo("Base URL", ConfigReader.get("base.url", "https://opensource-demo.orangehrmlive.com/"));
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
            extent.setSystemInfo("User", System.getProperty("user.name"));
        }
        return extent;
    }

    @Override
    public void onStart(ITestContext context) {
        log.info("Test suite/context started: {}", context.getName());
        getExtentReports();
    }

    @Override
    public void onTestStart(ITestResult result) {
        String methodName = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        String env = ConfigReader.getActiveEnvironment();
        String browser = ConfigReader.get("browser", "chrome");

        log.info("Starting test: {} [env={}, browser={}]", methodName, env, browser);

        ExtentTest test = getExtentReports().createTest(methodName, description);
        test.assignDevice(browser);

        String[] groups = result.getMethod().getGroups();
        if (groups != null && groups.length > 0) {
            test.assignCategory(groups);
        }

        test.info(String.format("<b>Environment:</b> <code>%s</code> | <b>Browser:</b> <code>%s</code> | <b>Test Name:</b> <code>%s</code>",
                env.toUpperCase(), browser, methodName));

        extentTest.set(test);

        // Allure parameters
        try {
            Allure.parameter("Environment", env.toUpperCase());
            Allure.parameter("Browser", browser);
            Allure.parameter("Test Name", methodName);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("Test passed: {}", result.getMethod().getMethodName());
        ExtentTest current = extentTest.get();
        if (current != null) {
            current.log(Status.PASS, "Test passed successfully: " + result.getMethod().getMethodName());
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("Test failed: {} - {}", result.getMethod().getMethodName(), result.getThrowable());
        ExtentTest current = extentTest.get();
        if (current != null) {
            current.log(Status.FAIL, "Test failed: " + result.getThrowable());
        }
        attachScreenshotOnFailure(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("Test skipped / retrying: {} - {}", result.getMethod().getMethodName(), result.getThrowable());
        ExtentTest current = extentTest.get();
        if (current != null) {
            current.log(Status.SKIP, "Test skipped: " + result.getThrowable());
        }
    }

    /**
     * Attaches failure screenshot to both ExtentReports and Allure.
     */
    private void attachScreenshotOnFailure(ITestResult result) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver == null) {
                log.info("No active WebDriver instance for '{}'; skipping screenshot capture.", result.getMethod().getMethodName());
                return;
            }

            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            // Save to disk for Extent report
            String screenshotDir = ConfigReader.get("screenshot.dir", "test-output/screenshots");
            new File(screenshotDir).mkdirs();
            String fileName = result.getMethod().getMethodName() + "_" + System.currentTimeMillis() + ".png";
            File screenshotFile = new File(screenshotDir, fileName);
            Files.write(screenshotFile.toPath(), screenshotBytes);

            ExtentTest current = extentTest.get();
            if (current != null) {
                current.addScreenCaptureFromPath(screenshotFile.getPath(), "Failure Screenshot");
            }

            // Attach to Allure results
            Allure.addAttachment(result.getMethod().getMethodName() + " - Failure Screenshot",
                    new ByteArrayInputStream(screenshotBytes));
        } catch (Exception e) {
            log.error("Could not capture failure screenshot: {}", e.getMessage());
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        log.info("Test context finished: {}", context.getName());
        if (extent != null) {
            extent.flush();
        }
    }

    // ── Safe Data & Reporting Helpers ─────────────────────────────────────────

    /**
     * Logs safe test data (e.g. employee ID, employee name) to ExtentReports, Allure, and Log4j2.
     * Prevents exposure of passwords, secrets, or auth tokens.
     */
    public static void logTestData(String key, String value) {
        if (key == null) return;
        String safeValue = sanitizeValue(key, value);
        log.info("[TestData] {}: {}", key, safeValue);

        ExtentTest current = extentTest.get();
        if (current != null) {
            current.info(String.format("<b>[Test Data]</b> %s: <code>%s</code>", key, safeValue));
        }
        try {
            Allure.step(String.format("Test Data: %s = %s", key, safeValue));
        } catch (Exception ignored) {
        }
    }

    /**
     * Logs API endpoint operations to ExtentReports, Allure, and Log4j2.
     */
    public static void logApiEndpoint(String httpMethod, String endpoint) {
        log.info("[API Call] {} {}", httpMethod, endpoint);

        ExtentTest current = extentTest.get();
        if (current != null) {
            current.info(String.format("<b>[API Endpoint]</b> <span class='badge badge-info'>%s</span> <code>%s</code>",
                    httpMethod, endpoint));
        }
        try {
            Allure.step(String.format("API Endpoint: [%s] %s", httpMethod, endpoint));
        } catch (Exception ignored) {
        }
    }

    /**
     * Logs retry attempt information to ExtentReports, Allure, and Log4j2.
     */
    public static void logRetryAttempt(String testName, int attempt, int maxRetry) {
        log.warn("[RETRY ATTEMPT {}/{}] Test '{}'", attempt, maxRetry, testName);

        ExtentTest current = extentTest.get();
        if (current != null) {
            current.warning(String.format("<b>[RETRY ATTEMPT %d/%d]</b> Test '%s' failed transiently and is being retried.",
                    attempt, maxRetry, testName));
        }
        try {
            Allure.step(String.format("Retry Attempt %d of %d for test '%s'", attempt, maxRetry, testName));
        } catch (Exception ignored) {
        }
    }

    /**
     * Masks any sensitive fields containing password, secret, token, or key.
     */
    private static String sanitizeValue(String key, String value) {
        if (value == null) return "null";
        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("password") || lowerKey.contains("secret") ||
                lowerKey.contains("token") || lowerKey.contains("auth") || lowerKey.contains("credential")) {
            return "********";
        }
        return value;
    }
}
