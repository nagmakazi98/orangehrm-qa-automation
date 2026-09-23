package com.orangehrm.pages;

import com.orangehrm.utils.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for the OrangeHRM Dashboard (post-login landing page)
 * and the shared top-navigation / user-dropdown menu used for logout.
 */
public class DashboardPage {

    private final WebDriver driver;
    private final WaitUtils waitUtils;

    private final By dashboardHeader = By.xpath("//h6[text()='Dashboard']");
    private final By sideMenu = By.cssSelector(".oxd-sidepanel");
    private final By pimMenuItem = By.xpath("//span[text()='PIM']");
    private final By userDropdown = By.cssSelector(".oxd-userdropdown-tab");
    private final By logoutLink = By.xpath("//a[text()='Logout']");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver);
    }

    public boolean isDashboardDisplayed() {
        try {
            return waitUtils.waitForVisible(dashboardHeader).isDisplayed()
                    && driver.findElement(sideMenu).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public PimPage navigateToPim() {
        waitUtils.waitForClickable(pimMenuItem).click();
        return new PimPage(driver);
    }

    public LoginPage logout() {
        waitUtils.waitForClickable(userDropdown).click();
        waitUtils.waitForClickable(logoutLink).click();
        return new LoginPage(driver);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
