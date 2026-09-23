package com.orangehrm.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Centralizes explicit-wait logic so page objects never sprinkle raw
 * Thread.sleep() calls or duplicate WebDriverWait boilerplate.
 */
public class WaitUtils {

    private final WebDriver driver;
    private final WebDriverWait wait;

    private final By loaders = By.cssSelector(".oxd-form-loader, .oxd-loading-spinner");

    public WaitUtils(WebDriver driver) {
        this.driver = driver;
        int timeout = ConfigReader.getInt("explicit.wait.seconds");
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
    }

    public void waitForLoadersToDisappear() {
        try {
            if (!driver.findElements(loaders).isEmpty()) {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(loaders));
            }
        } catch (Exception ignored) {
        }
    }

    public WebElement waitForVisible(By locator) {
        waitForLoadersToDisappear();
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForVisibleDirect(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        waitForLoadersToDisappear();
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public void safeClick(By locator) {
        waitForLoadersToDisappear();
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (org.openqa.selenium.ElementClickInterceptedException e) {
            waitForLoadersToDisappear();
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        }
    }

    public boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean waitForUrlContains(String fraction) {
        return wait.until(ExpectedConditions.urlContains(fraction));
    }

    public boolean waitForTextPresent(By locator, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }
}
