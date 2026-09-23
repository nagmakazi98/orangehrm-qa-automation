package com.orangehrm.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.orangehrm.base.DriverFactory;
import com.orangehrm.utils.ConfigReader;
import io.qameta.allure.Allure;
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
 * Wires TestNG lifecycle events to:
 *  - ExtentReports (HTML report at reports/ExtentReport.html)
 *  - Allure (screenshot attachment on failure; HTML report generated via
 *    `mvn allure:report` from the raw results in target/allure-results)
 *
 * Registered centrally in testng.xml so no test class needs to know
 * anything about reporting.
 */
public class TestListener implements ITestListener {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    private static ExtentReports getExtentReports() {
        if (extent == null) {
            String reportPath = ConfigReader.get("extent.report.path", "reports/ExtentReport.html");
            new File(reportPath).getParentFile().mkdirs();
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setDocumentTitle("OrangeHRM Employee Lifecycle - Automation Report");
            spark.config().setReportName("QA Automation Technical Assessment");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Framework", "Selenium + Java + TestNG");
            extent.setSystemInfo("Application Under Test", "OrangeHRM Demo");
        }
        return extent;
    }

    @Override
    public void onStart(ITestContext context) {
        getExtentReports();
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTest test = getExtentReports().createTest(
                result.getMethod().getMethodName(),
                result.getMethod().getDescription());
        extentTest.set(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        extentTest.get().log(Status.PASS, "Test passed: " + result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        extentTest.get().log(Status.FAIL, "Test failed: " + result.getThrowable());
        attachScreenshotOnFailure(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        extentTest.get().log(Status.SKIP, "Test skipped: " + result.getThrowable());
    }

    private void attachScreenshotOnFailure(ITestResult result) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            if (driver == null) return;

            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            // Save to disk for the Extent report / manual review
            String screenshotDir = ConfigReader.get("screenshot.dir", "test-output/screenshots");
            new File(screenshotDir).mkdirs();
            String fileName = result.getMethod().getMethodName() + "_" + System.currentTimeMillis() + ".png";
            File screenshotFile = new File(screenshotDir, fileName);
            Files.write(screenshotFile.toPath(), screenshotBytes);
            extentTest.get().addScreenCaptureFromPath(screenshotFile.getPath());

            // Attach to Allure results too
            Allure.addAttachment(result.getMethod().getMethodName() + " - failure screenshot",
                    new ByteArrayInputStream(screenshotBytes));
        } catch (Exception e) {
            System.out.println("[TestListener] Could not capture failure screenshot: " + e.getMessage());
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        getExtentReports().flush();
    }
}
