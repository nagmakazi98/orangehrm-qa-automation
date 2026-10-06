package com.orangehrm.pages;

import com.orangehrm.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Abstract base for all Page Objects.
 *
 * Centralises the WebDriver reference, WaitUtils, and shared
 * helpers so page subclasses only need to declare their own locators and actions.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WaitUtils waitUtils;
    protected final Logger log;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver);
        this.log = LogManager.getLogger(getClass());
    }

    /** Returns the current browser URL. */
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /** Waits for loaders/spinners to disappear using centralized WaitUtils. */
    public void waitForLoadersToDisappear() {
        waitUtils.waitForLoadersToDisappear();
    }

    /** Waits for element to be visible and returns it. */
    protected WebElement findElement(By locator) {
        return waitUtils.waitForVisible(locator);
    }

    /** Safe click that waits for element to be clickable. */
    protected void click(By locator) {
        waitUtils.safeClick(locator);
    }

    /** Clears input field and sends keys. */
    protected void type(By locator, String text) {
        WebElement field = waitUtils.waitForVisible(locator);
        field.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        field.sendKeys(text);
    }

    /** Gets visible text from element. */
    protected String getText(By locator) {
        return waitUtils.waitForVisible(locator).getText();
    }

    /** Returns true if element is visible within explicit wait timeout. */
    protected boolean isDisplayed(By locator) {
        try {
            return waitUtils.waitForVisibleDirect(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /** Scrolls the given element into view via JavaScript. */
    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    /** Clicks an element via JavaScript. */
    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }
}
