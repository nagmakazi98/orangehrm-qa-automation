package com.orangehrm.base;

import com.orangehrm.utils.ConfigReader;
import com.orangehrm.utils.ScreenRecorderUtil;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

/**
 * Parent class for every test class in the suite. Centralizes:
 *  - WebDriver setup/teardown (per test class / per suite run)
 *  - Navigation to the base URL before each test method
 *  - Screen recording start/stop
 *
 * Keeping this logic here (rather than duplicated per test class) is what
 * makes the framework maintainable as more scenarios are added.
 */
public class BaseTest {

    protected WebDriver driver;

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        ScreenRecorderUtil.startRecording("EmployeeLifecycleSuite");
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        DriverFactory.initDriver();
        driver = DriverFactory.getDriver();
        driver.get(ConfigReader.get("base.url"));
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            System.out.println("[BaseTest] Test failed: " + result.getName()
                    + " - " + result.getThrowable());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quitDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        ScreenRecorderUtil.stopRecording();
    }
}
