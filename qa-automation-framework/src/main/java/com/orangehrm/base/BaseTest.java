package com.orangehrm.base;

import com.orangehrm.utils.ApiHelper;
import com.orangehrm.utils.ConfigReader;
import com.orangehrm.utils.ScreenRecorderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * Parent class for every test class in the suite. Centralizes:
 *  - WebDriver setup/teardown (per test class / per suite run)
 *  - Navigation to the base URL before tests
 *  - Screen recording start/stop
 *  - Log4j2 logger initialized per subclass
 *  - API helper initialized with the active browser session
 */
@Listeners({com.orangehrm.listeners.TestListener.class, com.orangehrm.listeners.RetryAnnotationTransformer.class})
public class BaseTest {

    protected final Logger log = LogManager.getLogger(getClass());
    protected WebDriver driver;
    protected ApiHelper apiHelper;
    protected com.orangehrm.api.OrangeHrmApiClient apiClient;

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        log.info("Starting test suite execution");
        ScreenRecorderUtil.startRecording("EmployeeLifecycleSuite");
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        log.info("Initializing WebDriver for class: {}", getClass().getSimpleName());
        DriverFactory.initDriver();
        driver = DriverFactory.getDriver();
        apiClient = new com.orangehrm.api.OrangeHrmApiClient(driver);
        apiHelper = new ApiHelper(driver);
        driver.get(ConfigReader.get("base.url"));
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            log.error("Test method '{}' FAILED: {}", result.getName(), result.getThrowable());
        } else if (result.getStatus() == ITestResult.SUCCESS) {
            log.info("Test method '{}' PASSED", result.getName());
        } else if (result.getStatus() == ITestResult.SKIP) {
            log.warn("Test method '{}' SKIPPED", result.getName());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        log.info("Tearing down WebDriver for class: {}", getClass().getSimpleName());
        DriverFactory.quitDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        log.info("Stopping screen recording and completing suite execution");
        ScreenRecorderUtil.stopRecording();
    }
}
