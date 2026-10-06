package com.orangehrm.pages;

import com.orangehrm.utils.EmployeeData;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;

/**
 * Page Object for PIM > Add Employee form.
 */
public class AddEmployeePage extends BasePage {

    private final By firstNameInput = By.name("firstName");
    private final By lastNameInput = By.name("lastName");
    private final By employeeIdInput =
            By.xpath("//label[text()='Employee Id']/../..//input");
    private final By profilePictureInput = By.cssSelector("input[type='file']");
    private final By saveButton = By.xpath("//button[normalize-space()='Save']");
    private final By successToast = By.cssSelector(".oxd-toast--success, .oxd-toast-content--success");

    public AddEmployeePage(WebDriver driver) {
        super(driver);
    }

    private void clearAndSendKeys(WebElement field, String value) {
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        field.sendKeys(value);
    }

    public AddEmployeePage enterFirstName(String firstName) {
        log.info("Entering firstName: {}", firstName);
        WebElement field = waitUtils.waitForVisible(firstNameInput);
        clearAndSendKeys(field, firstName);
        return this;
    }

    public AddEmployeePage enterLastName(String lastName) {
        log.info("Entering lastName: {}", lastName);
        WebElement field = waitUtils.waitForVisible(lastNameInput);
        clearAndSendKeys(field, lastName);
        return this;
    }

    /**
     * Overwrites the auto-generated Employee Id with the data-driven value
     * so the record can be reliably located later in the search step.
     */
    public AddEmployeePage enterEmployeeId(String employeeId) {
        log.info("Entering employeeId: {}", employeeId);
        WebElement field = waitUtils.waitForVisible(employeeIdInput);
        clearAndSendKeys(field, employeeId);
        return this;
    }

    public AddEmployeePage uploadProfilePicture(String relativePath) {
        log.info("Uploading profile picture: {}", relativePath);
        File file = new File(relativePath);
        if (file.exists()) {
            driver.findElement(profilePictureInput).sendKeys(file.getAbsolutePath());
        } else {
            log.warn("Profile picture not found at '{}', skipping upload", relativePath);
        }
        return this;
    }

    /**
     * Fills the entire Add Employee form from a single data object.
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
        log.info("Saving employee form");
        waitUtils.safeClick(saveButton);
        return new PersonalDetailsPage(driver);
    }

    public boolean isSuccessToastDisplayed() {
        try {
            WebElement toast = waitUtils.waitForVisible(successToast);
            return toast.isDisplayed() && toast.getText().toLowerCase().contains("success");
        } catch (Exception e) {
            log.debug("Success toast not observed: {}", e.getMessage());
            return false;
        }
    }
}
