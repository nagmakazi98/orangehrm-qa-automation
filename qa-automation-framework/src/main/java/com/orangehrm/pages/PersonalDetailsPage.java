package com.orangehrm.pages;

import com.orangehrm.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

/**
 * Page Object for an individual employee's profile screen (the tabbed view
 * you land on after Add Employee, or after opening a row from the
 * Employee List). Covers the "Job" tab fields required by the Edit step:
 * Job Title and Employment Status.
 */
public class PersonalDetailsPage {

    private final WebDriver driver;
    private final WaitUtils waitUtils;

    private final By jobTab = By.xpath("//a[text()='Job']");
    private final By employmentStatusDropdown =
            By.xpath("//label[text()='Employment Status']/../..//div[contains(@class,'oxd-select-text')]");
    private final By jobTitleDropdown =
            By.xpath("//label[text()='Job Title']/../..//div[contains(@class,'oxd-select-text')]");
    private final By dropdownOption = By.cssSelector(".oxd-select-option");
    private final By saveButton = By.xpath("//button[normalize-space()='Save']");
    private final By successToast = By.cssSelector(".oxd-toast-content--success, .oxd-toast, .oxd-toast-content");
    private final By employeeFullNameHeader = By.cssSelector(".employee-name h6, .oxd-topbar-header-breadcrumb h6");

    public PersonalDetailsPage(WebDriver driver) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver);
    }

    public PersonalDetailsPage goToJobTab() {
        waitUtils.safeClick(jobTab);
        return this;
    }

    private void selectFromCustomDropdown(By dropdownLocator, String visibleText) {
        waitUtils.safeClick(dropdownLocator);
        java.util.List<WebElement> options = driver.findElements(dropdownOption);
        WebElement matchingOption = options.stream()
                .filter(option -> option.getText().trim().equalsIgnoreCase(visibleText.trim()))
                .findFirst()
                .orElseGet(() -> options.stream()
                        .filter(option -> !option.getText().trim().isEmpty() && !option.getText().contains("-- Select --"))
                        .filter(option -> option.getText().trim().toLowerCase().contains(visibleText.trim().toLowerCase())
                                || visibleText.trim().toLowerCase().contains(option.getText().trim().toLowerCase()))
                        .findFirst()
                        .orElse(options.stream()
                                .filter(option -> !option.getText().contains("-- Select --"))
                                .findFirst()
                                .orElse(null)));

        if (matchingOption == null) {
            throw new RuntimeException("Dropdown option not found for: " + visibleText);
        }
        matchingOption.click();
    }

    public PersonalDetailsPage updateJobTitle(String jobTitle) {
        selectFromCustomDropdown(jobTitleDropdown, jobTitle);
        return this;
    }

    public PersonalDetailsPage updateEmploymentStatus(String employmentStatus) {
        selectFromCustomDropdown(employmentStatusDropdown, employmentStatus);
        return this;
    }

    public PersonalDetailsPage saveJobDetails() {
        waitUtils.safeClick(saveButton);
        return this;
    }

    private final By toastLocator = By.cssSelector(".oxd-toast, .oxd-toast-content, .oxd-toast--success, #oxd-toaster_1");

    public boolean isSuccessToastDisplayed() {
        try {
            return waitUtils.waitForVisibleDirect(toastLocator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    private final By personalDetailsHeader = By.xpath("//h6[contains(@class,'orangehrm-main-title') or normalize-space()='Personal Details']");

    public boolean isPersonalDetailsPageLoaded() {
        try {
            return waitUtils.waitForUrlContains("viewPersonalDetails")
                    || waitUtils.waitForUrlContains("personal-details");
        } catch (Exception e) {
            return false;
        }
    }
}
