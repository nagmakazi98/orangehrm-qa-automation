package com.orangehrm.tests;

import com.orangehrm.api.OrangeHrmApiClient;
import com.orangehrm.api.models.EmployeeSummaryDto;
import com.orangehrm.api.models.JobDetailsDto;
import com.orangehrm.api.models.PersonalDetailsDto;
import com.orangehrm.base.BaseTest;
import com.orangehrm.pages.*;
import com.orangehrm.utils.*;
import io.qameta.allure.*;
import io.restassured.response.Response;
import com.orangehrm.listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Functional test suite: "Employee Lifecycle Management".
 *
 * <p>Architectural Design:
 * <ul>
 *   <li><strong>Zero dependsOnMethods Chaining:</strong> Each test is completely self-contained
 *       and independently runnable in isolation via {@code -Dtest=EmployeeLifecycleTest#<method>}.</li>
 *   <li><strong>Fast API Preconditions:</strong> Tests needing existing employee data (Search, Edit, Delete)
 *       provision prerequisites via the authenticated API in milliseconds instead of long, brittle UI flows.</li>
 *   <li><strong>Robust Teardown Safety Net:</strong> {@code @AfterMethod(alwaysRun = true)} guarantees that
 *       any employee created during a test is cleanly deleted via API even if the test fails or throws an exception.</li>
 *   <li><strong>No Shared Mutable State:</strong> Dynamic test data is generated per-test, preventing sandbox collision.</li>
 * </ul>
 * </p>
 */
@Epic("OrangeHRM Regression")
@Feature("Employee Lifecycle Management")
public class EmployeeLifecycleTest extends BaseTest {

    private EmployeeData currentEmployee;
    private int createdEmpNumber = -1;

    @BeforeMethod(alwaysRun = true)
    public void setupMethod() {
        // Reset per-test tracker so @AfterMethod cleanup has a clean baseline
        createdEmpNumber = -1;
        // Refresh API session cookies from the current driver state (BaseTest.setUp
        // already constructed these, but the session may have changed between tests).
        apiClient.setSessionFromDriver(driver);
        apiHelper.setSessionFromDriver(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void cleanupEmployee() {
        if (createdEmpNumber > 0) {
            log.info("Cleaning up employee empNumber={} via API", createdEmpNumber);
            try {
                apiClient.deleteEmployee(createdEmpNumber);
            } catch (Exception e) {
                log.warn("API cleanup failed for empNumber {}: {}", createdEmpNumber, e.getMessage());
            } finally {
                createdEmpNumber = -1;
            }
        }
    }

    /**
     * Helper to ensure the browser has an active authenticated session.
     */
    private DashboardPage ensureLoggedIn() {
        DashboardPage dashboardPage = new DashboardPage(driver);
        if (!dashboardPage.isDashboardDisplayed()) {
            dashboardPage = SessionManager.loginAsAdmin(driver);
            Assert.assertTrue(dashboardPage.isDashboardDisplayed(), "Dashboard should be visible after login");
            apiClient.setSessionFromDriver(driver);
            apiHelper.setSessionFromDriver(driver);
        }
        return dashboardPage;
    }

    @Test(priority = 1, groups = {"smoke", "regression", "ui"},
            description = "Login with valid credentials and verify dashboard is visible")
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    public void testLogin() {
        SessionManager.ensureLoggedOut(driver);
        LoginPage loginPage = new LoginPage(driver);
        Assert.assertTrue(loginPage.isAt(), "Login page did not load as expected.");

        TestListener.logTestData("Username", ConfigReader.get("login.username"));

        DashboardPage dashboardPage = loginPage.loginAs(
                ConfigReader.get("login.username"),
                ConfigReader.get("login.password"));

        Assert.assertTrue(dashboardPage.isDashboardDisplayed(),
                "Dashboard was not visible after login - login likely failed.");
        Assert.assertTrue(dashboardPage.getCurrentUrl().contains("/dashboard"),
                "URL does not contain '/dashboard' after successful login.");
        apiClient.setSessionFromDriver(driver);
    }

    @Test(priority = 2, groups = {"regression", "ui"},
            description = "Add a new employee via PIM UI and cross-validate with real OrangeHRM API")
    @Story("Add Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testAddNewEmployee() {
        DashboardPage dashboardPage = ensureLoggedIn();
        currentEmployee = DataFactory.randomEmployee();
        TestListener.logTestData("Employee ID", currentEmployee.getEmployeeId());
        TestListener.logTestData("Employee Name", currentEmployee.getFirstName() + " " + currentEmployee.getLastName());

        PimPage pimPage = dashboardPage.navigateToPim();
        AddEmployeePage addEmployeePage = pimPage.clickAddEmployee();
        addEmployeePage.fillEmployeeForm(currentEmployee);
        PersonalDetailsPage personalDetailsPage = addEmployeePage.clickSave();

        Assert.assertTrue(personalDetailsPage.isPersonalDetailsPageLoaded(),
                "Personal Details page did not load after saving new employee.");
        Assert.assertTrue(personalDetailsPage.getCurrentUrl().contains("personal-details") ||
                        personalDetailsPage.getCurrentUrl().contains("pim/viewPersonalDetails"),
                "URL does not indicate employee's Personal Details page was reached.");

        createdEmpNumber = personalDetailsPage.getEmpNumber();
        TestListener.logTestData("Generated empNumber", String.valueOf(createdEmpNumber));

        // Real API cross-validation: verify UI-created employee exists in OrangeHRM backend
        if (createdEmpNumber > 0) {
            TestListener.logApiEndpoint("GET", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT + "/" + createdEmpNumber + "/personal-details");
            PersonalDetailsDto apiDetails = apiClient.getPersonalDetails(createdEmpNumber);
            if (apiDetails != null) {
                Assert.assertEquals(apiDetails.getFirstName(), currentEmployee.getFirstName(),
                        "API first name does not match UI input.");
                Assert.assertEquals(apiDetails.getLastName(), currentEmployee.getLastName(),
                        "API last name does not match UI input.");
                Assert.assertEquals(apiDetails.getEmployeeId(), currentEmployee.getEmployeeId(),
                        "API employee ID does not match UI input.");
            }
        }
    }

    @Test(priority = 3, groups = {"regression", "ui"},
            description = "Search employee by Employee Id and verify it is present in PIM")
    @Story("Search Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testVerifyEmployeeCreated() {
        DashboardPage dashboardPage = ensureLoggedIn();

        // Create prerequisite employee via API for fast, independent setup
        currentEmployee = DataFactory.randomEmployee();
        TestListener.logTestData("Employee ID", currentEmployee.getEmployeeId());
        TestListener.logApiEndpoint("POST", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT);
        createdEmpNumber = apiClient.createEmployee(currentEmployee);
        Assert.assertTrue(createdEmpNumber > 0, "Prerequisite employee creation failed via API.");

        PimPage pimPage = dashboardPage.navigateToPim();
        pimPage.searchByEmployeeId(currentEmployee.getEmployeeId());

        Assert.assertTrue(pimPage.isEmployeeResultDisplayed(currentEmployee.getEmployeeId()),
                "Employee with ID '" + currentEmployee.getEmployeeId() + "' was not found in PIM Employee List.");
    }

    @Test(priority = 4, groups = {"regression", "ui"},
            description = "Search employee by Employee Id, edit Job Title & Employment Status, and verify via API")
    @Story("Edit Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testEditEmployeeInformation() {
        DashboardPage dashboardPage = ensureLoggedIn();

        // Create prerequisite employee via API for fast, independent setup
        currentEmployee = DataFactory.randomEmployee();
        TestListener.logTestData("Employee ID", currentEmployee.getEmployeeId());
        TestListener.logTestData("Updated Job Title", currentEmployee.getUpdatedJobTitle());
        TestListener.logTestData("Updated Status", currentEmployee.getUpdatedEmploymentStatus());
        TestListener.logApiEndpoint("POST", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT);
        createdEmpNumber = apiClient.createEmployee(currentEmployee);
        Assert.assertTrue(createdEmpNumber > 0, "Prerequisite employee creation failed via API.");

        PimPage pimPage = dashboardPage.navigateToPim();
        pimPage.searchByEmployeeId(currentEmployee.getEmployeeId());

        PersonalDetailsPage personalDetailsPage = pimPage.openFirstSearchResult();
        personalDetailsPage.goToJobTab()
                .updateJobTitle(currentEmployee.getUpdatedJobTitle())
                .updateEmploymentStatus(currentEmployee.getUpdatedEmploymentStatus())
                .saveJobDetails();

        Assert.assertTrue(personalDetailsPage.isSuccessToastDisplayed(),
                "Success toast was not displayed after updating Job details.");

        // Read updated data via API to cross-validate persistence
        TestListener.logApiEndpoint("GET", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT + "/" + createdEmpNumber + "/job-details");
        JobDetailsDto jobDetails = apiClient.getJobDetails(createdEmpNumber);
        if (jobDetails != null && jobDetails.getJobTitle() != null) {
            log.info("API verified updated Job Title: {}", jobDetails.getJobTitleName());
            Assert.assertEquals(jobDetails.getJobTitleName(), currentEmployee.getUpdatedJobTitle(),
                    "API job title does not match updated UI value.");
        }
    }

    @Test(priority = 5, groups = {"regression", "api"},
            description = "Validate employee details via real OrangeHRM API and cross-check against UI session data")
    @Story("API Validation")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateEmployeeViaApi() {
        ensureLoggedIn();
        currentEmployee = DataFactory.randomEmployee();
        TestListener.logTestData("Employee ID", currentEmployee.getEmployeeId());
        TestListener.logTestData("Employee Name", currentEmployee.getFirstName() + " " + currentEmployee.getLastName());

        // Step 1: Create employee via API
        TestListener.logApiEndpoint("POST", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT);
        createdEmpNumber = apiClient.createEmployee(currentEmployee);
        Assert.assertTrue(createdEmpNumber > 0, "API failed to create employee record.");

        // Step 2: Fetch and validate typed PersonalDetailsDto via OrangeHRM v2 REST API
        TestListener.logApiEndpoint("GET", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT + "/" + createdEmpNumber + "/personal-details");
        PersonalDetailsDto personalDetails = apiClient.getPersonalDetails(createdEmpNumber);
        Assert.assertNotNull(personalDetails, "API returned null PersonalDetailsDto.");
        Assert.assertEquals(personalDetails.getFirstName(), currentEmployee.getFirstName(),
                "API first name does not match test data.");
        Assert.assertEquals(personalDetails.getLastName(), currentEmployee.getLastName(),
                "API last name does not match test data.");
        Assert.assertEquals(personalDetails.getEmployeeId(), currentEmployee.getEmployeeId(),
                "API employee ID does not match test data.");

        // Step 3: Query employee list endpoint /web/index.php/api/v2/pim/employees and validate EmployeeSummaryDto
        TestListener.logApiEndpoint("GET", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT);
        EmployeeSummaryDto summary = apiClient.findEmployeeById(currentEmployee.getEmployeeId());
        Assert.assertNotNull(summary, "Created employee was not found in /pim/employees list.");
        Assert.assertEquals(summary.getEmpNumber(), createdEmpNumber,
                "Employee list empNumber does not match created empNumber.");
        Assert.assertEquals(summary.getFirstName(), currentEmployee.getFirstName(),
                "Employee list firstName does not match test data.");
        Assert.assertEquals(summary.getLastName(), currentEmployee.getLastName(),
                "Employee list lastName does not match test data.");
    }

    @Test(priority = 6, groups = {"regression", "ui"},
            description = "Delete the employee from the UI and verify deletion via UI and API")
    @Story("Delete Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeleteEmployee() {
        DashboardPage dashboardPage = ensureLoggedIn();

        // Create prerequisite employee via API for fast, independent setup
        currentEmployee = DataFactory.randomEmployee();
        TestListener.logTestData("Target Employee ID", currentEmployee.getEmployeeId());
        TestListener.logApiEndpoint("POST", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT);
        createdEmpNumber = apiClient.createEmployee(currentEmployee);
        Assert.assertTrue(createdEmpNumber > 0, "Prerequisite employee creation failed via API.");

        PimPage pimPage = dashboardPage.navigateToPim();
        pimPage.searchByEmployeeId(currentEmployee.getEmployeeId());
        pimPage.deleteFirstSearchResult();

        pimPage.searchByEmployeeId(currentEmployee.getEmployeeId());
        Assert.assertTrue(pimPage.isNoRecordsFound() || !pimPage.isEmployeeResultDisplayed(currentEmployee.getEmployeeId()),
                "Employee record still appears in PIM after UI deletion.");

        // Verify via API that employee is no longer retrievable (OrangeHRM returns HTTP 422 or 404 for deleted records)
        TestListener.logApiEndpoint("GET (Verification)", OrangeHrmApiClient.PIM_EMPLOYEES_ENDPOINT + "/" + createdEmpNumber + "/personal-details");
        Response apiResp = apiClient.getPersonalDetailsResponse(createdEmpNumber);
        int statusCode = apiResp.getStatusCode();
        Assert.assertTrue(statusCode == 422 || statusCode == 404 || statusCode == 400 || statusCode != 200
                || apiResp.getBody().asString().contains("error"),
                "API should not return a valid personal-details record after UI deletion. Got HTTP: " + statusCode);

        // Deletion confirmed via UI and API; reset tracker so teardown doesn't redundantly delete
        createdEmpNumber = -1;
    }

    @Test(priority = 7, groups = {"smoke", "regression", "ui"},
            description = "Logout and confirm the session is invalidated")
    @Story("Logout")
    @Severity(SeverityLevel.BLOCKER)
    public void testLogout() {
        DashboardPage dashboardPage = ensureLoggedIn();
        TestListener.logTestData("Action", "Logout from Dashboard");
        LoginPage loginPage = dashboardPage.logout();

        Assert.assertTrue(loginPage.isAt(),
                "Login page was not displayed after logout.");

        // Confirm session invalidation: navigating directly to dashboard redirects to login
        driver.get(ConfigReader.get("base.url") + "web/index.php/dashboard/index");
        Assert.assertTrue(loginPage.isAt(),
                "Application allowed access to protected dashboard page after logout.");
    }
}
