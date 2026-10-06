package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Page Object for an individual employee's profile screen (the tabbed view
 * reached after Add Employee, or after opening a row from the Employee List).
 * Covers the "Job" tab fields: Job Title and Employment Status.
 */
public class PersonalDetailsPage extends BasePage {

    private final By jobTab = By.xpath("//a[text()='Job']");
    private final By employmentStatusDropdown =
            By.xpath("//label[text()='Employment Status']/../..//div[contains(@class,'oxd-select-text')]");
    private final By jobTitleDropdown =
            By.xpath("//label[text()='Job Title']/../..//div[contains(@class,'oxd-select-text')]");
    private final By dropdownOption = By.cssSelector(".oxd-select-option");
    private final By formLoader = By.cssSelector(".oxd-form-loader");
    private final By saveButton = By.xpath("//button[normalize-space()='Save']");
    private final By successToast = By.cssSelector(".oxd-toast--success, .oxd-toast-content--success");

    public PersonalDetailsPage(WebDriver driver) {
        super(driver);
    }

    public PersonalDetailsPage goToJobTab() {
        log.info("Navigating to Job tab");
        waitUtils.safeClick(jobTab);
        return this;
    }

    /**
     * Strict dropdown selection: searches for exact match first, then case-insensitive
     * trimmed match. Throws a detailed exception listing all available options if not found,
     * preventing silent mis-selection.
     */
    private void selectFromCustomDropdown(By dropdownLocator, String visibleText) {
        log.info("Selecting '{}' from dropdown {}", visibleText, dropdownLocator);
        // The Job form is covered by .oxd-form-loader while its data loads. Wait for the
        // field to render, then for the loader to go away (full explicit wait) before clicking.
        waitUtils.waitForVisibleDirect(dropdownLocator);
        waitUtils.waitForInvisible(formLoader);
        waitUtils.safeClick(dropdownLocator);
        waitUtils.waitForVisible(dropdownOption);

        List<WebElement> options = driver.findElements(dropdownOption);
        List<String> optionTexts = options.stream()
                .map(o -> o.getText().trim())
                .filter(t -> !t.isEmpty() && !t.contains("-- Select --"))
                .collect(Collectors.toList());

        WebElement matchingOption = options.stream()
                .filter(option -> option.getText().trim().equalsIgnoreCase(visibleText.trim()))
                .findFirst()
                .orElseGet(() -> options.stream()
                        .filter(option -> option.getText().trim().toLowerCase().contains(visibleText.trim().toLowerCase()))
                        .findFirst()
                        .orElse(null));

        if (matchingOption == null) {
            throw new IllegalArgumentException(String.format(
                    "Dropdown option '%s' not found. Available options were: %s",
                    visibleText, optionTexts));
        }

        matchingOption.click();
    }

    public PersonalDetailsPage updateJobTitle(String jobTitle) {
        log.info("Updating job title to: {}", jobTitle);
        selectFromCustomDropdown(jobTitleDropdown, jobTitle);
        return this;
    }

    public PersonalDetailsPage updateEmploymentStatus(String employmentStatus) {
        log.info("Updating employment status to: {}", employmentStatus);
        selectFromCustomDropdown(employmentStatusDropdown, employmentStatus);
        return this;
    }

    public PersonalDetailsPage saveJobDetails() {
        log.info("Saving job details");
        waitUtils.safeClick(saveButton);
        return this;
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

    public boolean isPersonalDetailsPageLoaded() {
        try {
            return waitUtils.waitForUrlContains("viewPersonalDetails")
                    || waitUtils.waitForUrlContains("personal-details");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the internal empNumber assigned by OrangeHRM from the current URL.
     * Example URL: /web/index.php/pim/viewPersonalDetails/empNumber/1234
     *
     * @return internal employee number, or -1 if not found
     */
    public int getEmpNumber() {
        String url = getCurrentUrl();
        if (url != null && url.contains("empNumber/")) {
            try {
                String idPart = url.substring(url.indexOf("empNumber/") + 10).replaceAll("[^0-9].*", "");
                return Integer.parseInt(idPart);
            } catch (Exception e) {
                log.debug("Could not parse empNumber from URL {}: {}", url, e.getMessage());
            }
        }
        return -1;
    }
}
