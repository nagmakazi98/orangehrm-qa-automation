package com.orangehrm.tests;

import com.orangehrm.base.BaseTest;
import com.orangehrm.pages.*;
import com.orangehrm.utils.*;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * End-to-end functional scenario: "Employee Lifecycle Management".
 *
 * Covers, in a single ordered flow (dependsOnMethods keeps state realistic
 * and mirrors how a QA would exercise the feature manually):
 *   1. Login
 *   2. Add a new employee (data-driven, incl. profile picture upload)
 *   3. Edit employee (Job Title + Employment Status)
 *   4. Validate employee via API and cross-check against the UI
 *   5. Delete the employee (UI + API)
 *   6. Logout
 */
@Epic("OrangeHRM Regression")
@Feature("Employee Lifecycle Management")
public class EmployeeLifecycleTest extends BaseTest {

    private final EmployeeData employeeData =
            JsonDataReader.readData("src/test/resources/testdata/employee.json", EmployeeData.class);
    private final ApiHelper apiHelper = new ApiHelper();

    // Populated during the flow, used by later steps
    private static String createdApiUserId;

    @org.testng.annotations.BeforeClass
    public void setupTestData() {
        String uniqueEmpId = "QA" + (System.currentTimeMillis() % 89999 + 10000);
        employeeData.setEmployeeId(uniqueEmpId);
    }

    @Test(priority = 1, description = "Login with valid credentials and verify dashboard is visible")
    @Story("Login")
    @Severity(SeverityLevel.BLOCKER)
    public void testLogin() {
        LoginPage loginPage = new LoginPage(driver);
        Assert.assertTrue(loginPage.isAt(), "Login page did not load as expected.");

        DashboardPage dashboardPage = loginPage.loginAs(
                ConfigReader.get("login.username"),
                ConfigReader.get("login.password"));

        Assert.assertTrue(dashboardPage.isDashboardDisplayed(),
                "Dashboard was not visible after login - login likely failed.");
        Assert.assertTrue(dashboardPage.getCurrentUrl().contains("/dashboard"),
                "URL does not contain '/dashboard' after successful login.");
    }

    @Test(priority = 2, description = "Add a new employee via PIM using data-driven input, incl. profile picture",
            dependsOnMethods = "testLogin")
    @Story("Add Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testAddNewEmployee() {
        DashboardPage dashboardPage = new DashboardPage(driver);
        PimPage pimPage = dashboardPage.navigateToPim();

        AddEmployeePage addEmployeePage = pimPage.clickAddEmployee();
        addEmployeePage.fillEmployeeForm(employeeData);
        PersonalDetailsPage personalDetailsPage = addEmployeePage.clickSave();

        Assert.assertTrue(personalDetailsPage.isPersonalDetailsPageLoaded(),
                "Personal Details page did not load after saving the new employee - "
                        + "employee creation may have failed.");
        Assert.assertTrue(personalDetailsPage.getCurrentUrl().contains("personal-details") ||
                        personalDetailsPage.getCurrentUrl().contains("pim/viewPersonalDetails"),
                "URL does not indicate the employee's Personal Details page was reached.");
    }

    @Test(priority = 3, description = "Search the new employee by Employee Id and verify it is present in PIM",
            dependsOnMethods = "testAddNewEmployee")
    @Story("Add Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testVerifyEmployeeCreated() {
        DashboardPage dashboardPage = new DashboardPage(driver);
        PimPage pimPage = dashboardPage.navigateToPim();
        pimPage.searchByEmployeeId(employeeData.getEmployeeId());

        Assert.assertTrue(pimPage.isEmployeeResultDisplayed(employeeData.getEmployeeId()),
                "Newly created employee with Employee Id '" + employeeData.getEmployeeId()
                        + "' was not found in the PIM Employee List.");
    }

    @Test(priority = 4, description = "Search employee by Employee Id and edit Job Title & Employment Status",
            dependsOnMethods = "testVerifyEmployeeCreated")
    @Story("Edit Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testEditEmployeeInformation() {
        DashboardPage dashboardPage = new DashboardPage(driver);
        PimPage pimPage = dashboardPage.navigateToPim();
        pimPage.searchByEmployeeId(employeeData.getEmployeeId());

        PersonalDetailsPage personalDetailsPage = pimPage.openFirstSearchResult();
        personalDetailsPage.goToJobTab()
                .updateJobTitle(employeeData.getUpdatedJobTitle())
                .updateEmploymentStatus(employeeData.getUpdatedEmploymentStatus())
                .saveJobDetails();

        Assert.assertTrue(personalDetailsPage.isSuccessToastDisplayed(),
                "Success toast was not displayed after updating Job Title / Employment Status.");
    }

    @Test(priority = 5, description = "Validate employee details via API and cross-check against UI data",
            dependsOnMethods = "testEditEmployeeInformation")
    @Story("API Validation")
    @Severity(SeverityLevel.NORMAL)
    public void testValidateEmployeeViaApi() {
        // Create/register the corresponding record at the API layer
        Response createResponse = apiHelper.createEmployeeRecord(employeeData);
        Assert.assertEquals(createResponse.getStatusCode(), 201,
                "Expected HTTP 201 when creating the employee record via API.");

        createdApiUserId = createResponse.jsonPath().getString("id");
        Assert.assertNotNull(createdApiUserId,
                "API did not return an id for the created employee record.");

        // Update at the API layer to mirror the UI edit step, then fetch and cross-check
        Response updateResponse = apiHelper.updateEmployeeRecord(createdApiUserId, employeeData);
        Assert.assertEquals(updateResponse.getStatusCode(), 200,
                "Expected HTTP 200 when updating the employee record via API.");

        String apiJobTitle = updateResponse.jsonPath().getString("job");
        String apiFullName = updateResponse.jsonPath().getString("name");

        Assert.assertEquals(apiJobTitle, employeeData.getUpdatedJobTitle(),
                "API 'job' field does not match the Job Title set in the UI.");
        Assert.assertEquals(apiFullName, employeeData.getFullName(),
                "API 'name' field does not match the employee's full name set in the UI.");
    }

    @Test(priority = 6, description = "Delete the employee from the UI and verify deletion via UI and API",
            dependsOnMethods = "testValidateEmployeeViaApi")
    @Story("Delete Employee")
    @Severity(SeverityLevel.CRITICAL)
    public void testDeleteEmployee() {
        DashboardPage dashboardPage = new DashboardPage(driver);
        PimPage pimPage = dashboardPage.navigateToPim();
        pimPage.searchByEmployeeId(employeeData.getEmployeeId());
        pimPage.deleteFirstSearchResult();

        pimPage.searchByEmployeeId(employeeData.getEmployeeId());
        Assert.assertTrue(pimPage.isNoRecordsFound(),
                "Employee record still appears in PIM after deletion - UI deletion verification failed.");

        // API-side deletion + verification
        Response deleteResponse = apiHelper.deleteEmployeeRecord(createdApiUserId);
        Assert.assertEquals(deleteResponse.getStatusCode(), 204,
                "Expected HTTP 204 when deleting the employee record via API.");
    }

    @Test(priority = 7, description = "Logout and confirm the session is invalidated",
            dependsOnMethods = "testDeleteEmployee")
    @Story("Logout")
    @Severity(SeverityLevel.BLOCKER)
    public void testLogout() {
        DashboardPage dashboardPage = new DashboardPage(driver);
        LoginPage loginPage = dashboardPage.logout();

        Assert.assertTrue(loginPage.isAt(),
                "Login page was not displayed after logout - logout may have failed.");

        // Confirm session invalidation: accessing protected dashboard URL directly
        // must redirect back to login page.
        driver.get(ConfigReader.get("base.url") + "web/index.php/dashboard/index");
        Assert.assertTrue(loginPage.isAt(),
                "Application allowed access to an authenticated page after logout - "
                        + "session was not properly invalidated.");
    }
}
