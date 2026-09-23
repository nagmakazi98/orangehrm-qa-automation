package com.orangehrm.pages;

import com.orangehrm.utils.EmployeeData;
import com.orangehrm.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

/**
 * Page Object for PIM > Add Employee form.
 */
public class AddEmployeePage {

    private final WebDriver driver;
    private final WaitUtils waitUtils;

    private final By firstNameInput = By.name("firstName");
    private final By lastNameInput = By.name("lastName");
    private final By employeeIdInput =
            By.xpath("//label[text()='Employee Id']/../..//input");
    private final By profilePictureInput = By.cssSelector("input[type='file']");
    private final By saveButton = By.xpath("//button[normalize-space()='Save']");
    private final By successToast = By.cssSelector(".oxd-toast-content--success");

    public AddEmployeePage(WebDriver driver) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver);
    }

    private void clearAndSendKeys(WebElement field, String value) {
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        field.sendKeys(value);
    }

    public AddEmployeePage enterFirstName(String firstName) {
        WebElement field = waitUtils.waitForVisible(firstNameInput);
        clearAndSendKeys(field, firstName);
        return this;
    }

    public AddEmployeePage enterLastName(String lastName) {
        WebElement field = waitUtils.waitForVisible(lastNameInput);
        clearAndSendKeys(field, lastName);
        return this;
    }

    /**
     * Overwrites the auto-generated Employee Id with the data-driven value
     * so the record can be reliably located later in the search step.
     */
    public AddEmployeePage enterEmployeeId(String employeeId) {
        WebElement field = waitUtils.waitForVisible(employeeIdInput);
        clearAndSendKeys(field, employeeId);
        return this;
    }

    public AddEmployeePage uploadProfilePicture(String relativePath) {
        File file = new File(relativePath);
        String absolutePath = file.getAbsolutePath();
        driver.findElement(profilePictureInput).sendKeys(absolutePath);
        return this;
    }

    /**
     * Fills the entire Add Employee form from a single data object -
     * keeps the test class free of low-level field-by-field calls.
     */
    public AddEmployeePage fillEmployeeForm(EmployeeData data) {
        enterFirstName(data.getFirstName());
        enterLastName(data.getLastName());
        enterEmployeeId(data.getEmployeeId());
        if (data.getProfilePicture() != null && !data.getProfilePicture().isEmpty()) {
            uploadProfilePicture(data.getProfilePicture());
        }
        return this;
    }

    public PersonalDetailsPage clickSave() {
        waitUtils.safeClick(saveButton);
        return new PersonalDetailsPage(driver);
    }

    public boolean isSuccessToastDisplayed() {
        try {
            return waitUtils.waitForVisible(successToast).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
