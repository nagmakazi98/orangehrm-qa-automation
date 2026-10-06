package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Page Object for PIM > Employee List: search by Employee Id, open a record,
 * and delete a record. Also exposes navigation to the "Add Employee" screen.
 */
public class PimPage extends BasePage {

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
        super(driver);
    }

    public AddEmployeePage clickAddEmployee() {
        log.info("Clicking 'Add' employee button");
        waitUtils.safeClick(addEmployeeButton);
        return new AddEmployeePage(driver);
    }

    public boolean isAddEmployeeButtonDisplayed() {
        try {
            return waitUtils.waitForVisible(addEmployeeButton).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isSearchButtonDisplayed() {
        try {
            return waitUtils.waitForVisible(searchButton).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Searches the Employee List by Employee Id and waits for the grid to refresh.
     */
    public PimPage searchByEmployeeId(String employeeId) {
        log.info("Searching employee by ID: {}", employeeId);
        WebElement field = waitUtils.waitForVisible(employeeIdSearchInput);
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        field.sendKeys(employeeId);
        waitUtils.safeClick(searchButton);
        waitForGridToLoad();
        return this;
    }

    public void waitForGridToLoad() {
        try {
            waitUtils.waitForInvisible(loadingSpinner);
        } catch (Exception ignored) {
            // spinner already gone
        }
    }

    public boolean isEmployeeResultDisplayed(String employeeId) {
        waitForGridToLoad();
        if (!driver.findElements(noRecordsFound).isEmpty()) {
            return false;
        }
        // The grid refreshes asynchronously after Search; wait until a row shows this ID
        // instead of reading whatever rows are in the DOM at this instant.
        try {
            waitUtils.waitForTextPresent(employeeTableRows, employeeId);
        } catch (TimeoutException e) {
            log.debug("No row containing '{}' appeared within the explicit wait", employeeId);
        }
        List<WebElement> rows = driver.findElements(employeeTableRows);
        return rows.stream().anyMatch(row -> row.getText().contains(employeeId));
    }

    public boolean isNoRecordsFound() {
        waitForGridToLoad();
        try {
            return waitUtils.waitForVisible(noRecordsFound).isDisplayed();
        } catch (Exception e) {
            return !driver.findElements(noRecordsFound).isEmpty()
                    || driver.findElements(employeeTableRows).isEmpty();
        }
    }

    /**
     * Opens the employee's Personal Details record from the search result row
     * (used for the Edit flow).
     */
    public PersonalDetailsPage openFirstSearchResult() {
        log.info("Opening first search result row");
        waitForGridToLoad();
        waitUtils.safeClick(employeeTableRows);
        return new PersonalDetailsPage(driver);
    }

    /**
     * Deletes the first row currently shown in the search results grid and
     * confirms the deletion dialog.
     */
    public PimPage deleteFirstSearchResult() {
        log.info("Deleting first search result in PIM grid");
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
