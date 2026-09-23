package com.orangehrm.pages;

import com.orangehrm.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for PIM > Employee List: search by Employee Id, open a record,
 * and delete a record. Also exposes navigation to the "Add Employee" screen.
 */
public class PimPage {

    private final WebDriver driver;
    private final WaitUtils waitUtils;

    private final By addEmployeeButton = By.xpath("//button[normalize-space()='Add']");
    private final By employeeIdSearchInput =
            By.xpath("(//label[text()='Employee Id']/../..//input)[1]");
    private final By searchButton = By.xpath("//button[normalize-space()='Search']");
    private final By employeeTableRows = By.cssSelector(".oxd-table-card");
    private final By loadingSpinner = By.cssSelector(".oxd-loading-spinner");
    private final By deleteTrashIconButton = By.cssSelector(".oxd-table-card .bi-trash");
    private final By checkboxFirstRow = By.cssSelector(".oxd-table-card .oxd-checkbox-input");
    private final By deleteSelectedButton = By.xpath("//button[normalize-space()='Delete Selected']");
    private final By confirmDeleteButton = By.xpath("//button[normalize-space()='Yes, Delete']");
    private final By noRecordsFound = By.xpath("//span[text()='No Records Found']");

    public PimPage(WebDriver driver) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver);
    }

    public AddEmployeePage clickAddEmployee() {
        waitUtils.safeClick(addEmployeeButton);
        return new AddEmployeePage(driver);
    }

    /**
     * Searches the Employee List by Employee Id and waits for the grid to refresh.
     */
    public PimPage searchByEmployeeId(String employeeId) {
        WebElement field = waitUtils.waitForVisible(employeeIdSearchInput);
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        field.sendKeys(employeeId);
        waitUtils.safeClick(searchButton);
        waitForGridToLoad();
        return this;
    }

    private void waitForGridToLoad() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.invisibilityOfElementLocated(loadingSpinner));
        } catch (Exception ignored) {
            // spinner may already be gone
        }
    }

    public boolean isEmployeeResultDisplayed(String employeeId) {
        waitForGridToLoad();
        if (!driver.findElements(noRecordsFound).isEmpty()) {
            return false;
        }
        List<WebElement> rows = driver.findElements(employeeTableRows);
        return rows.stream().anyMatch(row -> row.getText().contains(employeeId));
    }

    public boolean isNoRecordsFound() {
        waitForGridToLoad();
        return !driver.findElements(noRecordsFound).isEmpty();
    }

    /**
     * Opens the employee's Personal Details record from the search result row
     * (used for the Edit flow).
     */
    public PersonalDetailsPage openFirstSearchResult() {
        List<WebElement> rows = driver.findElements(employeeTableRows);
        if (rows.isEmpty()) {
            throw new RuntimeException("No employee row found to open for editing.");
        }
        rows.get(0).click();
        return new PersonalDetailsPage(driver);
    }

    /**
     * Deletes the first row currently shown in the search results grid and
     * confirms the deletion dialog.
     */
    public PimPage deleteFirstSearchResult() {
        waitForGridToLoad();
        if (!driver.findElements(deleteTrashIconButton).isEmpty()) {
            waitUtils.safeClick(deleteTrashIconButton);
        } else {
            waitUtils.safeClick(checkboxFirstRow);
            waitUtils.safeClick(deleteSelectedButton);
        }
        waitUtils.safeClick(confirmDeleteButton);
        waitForGridToLoad();
        return this;
    }
}
