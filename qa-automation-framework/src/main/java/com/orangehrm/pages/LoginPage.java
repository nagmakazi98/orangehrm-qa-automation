package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Page Object for the OrangeHRM Login page.
 */
public class LoginPage extends BasePage {

    // Locators
    private final By usernameInput = By.name("username");
    private final By passwordInput = By.name("password");
    private final By loginButton = By.cssSelector("button[type='submit']");
    private final By loginErrorAlert = By.cssSelector(".oxd-alert-content-text");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage enterUsername(String username) {
        log.info("Entering username: {}", username);
        WebElement field = waitUtils.waitForVisible(usernameInput);
        field.clear();
        field.sendKeys(username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        log.info("Entering password");
        WebElement field = waitUtils.waitForVisible(passwordInput);
        field.clear();
        field.sendKeys(password);
        return this;
    }

    public DashboardPage clickLogin() {
        log.info("Clicking login button");
        waitUtils.waitForClickable(loginButton).click();
        return new DashboardPage(driver);
    }

    /**
     * Convenience method encapsulating the full login flow.
     */
    public DashboardPage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        return clickLogin();
    }

    public boolean isLoginErrorDisplayed() {
        try {
            return waitUtils.waitForVisible(loginErrorAlert).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAt() {
        try {
            return waitUtils.waitForVisible(usernameInput).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
