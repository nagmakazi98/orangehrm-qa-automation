package com.orangehrm.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page Object for the OrangeHRM Dashboard (post-login landing page)
 * and the shared top-navigation / user-dropdown menu used for logout.
 */
public class DashboardPage extends BasePage {

    private final By dashboardHeader = By.xpath("//h6[normalize-space()='Dashboard']");
    private final By sideMenu = By.cssSelector(".oxd-sidepanel");
    private final By pimMenuItem = By.xpath("//span[normalize-space()='PIM'] | //a[contains(@href,'viewPimModule')]");
    private final By userDropdown = By.cssSelector(".oxd-userdropdown-tab");
    private final By logoutLink = By.xpath("//a[text()='Logout']");
    private final By hamburgerButton = By.cssSelector(".oxd-topbar-header-hamburger, i.bi-list");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    private void ensureSidebarExpanded() {
        try {
            if (!driver.findElements(hamburgerButton).isEmpty() && driver.findElement(hamburgerButton).isDisplayed()) {
                if (driver.findElements(sideMenu).isEmpty() || !driver.findElement(sideMenu).isDisplayed()) {
                    log.info("Sidebar collapsed; clicking hamburger toggle button to expand menu");
                    driver.findElement(hamburgerButton).click();
                }
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isDashboardDisplayed() {
        try {
            if (waitUtils.waitForUrlContains("/dashboard")) {
                return true;
            }
        } catch (Exception ignored) {
        }
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl != null && currentUrl.contains("/dashboard")) {
            return true;
        }
        try {
            return waitUtils.waitForVisible(dashboardHeader).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isPimMenuDisplayed() {
        ensureSidebarExpanded();
        try {
            return waitUtils.waitForVisibleDirect(pimMenuItem).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public PimPage navigateToPim() {
        ensureSidebarExpanded();
        log.info("Navigating to PIM module");
        waitUtils.waitForClickable(pimMenuItem).click();
        return new PimPage(driver);
    }

    public LoginPage logout() {
        log.info("Logging out");
        waitUtils.waitForClickable(userDropdown).click();
        waitUtils.waitForClickable(logoutLink).click();
        return new LoginPage(driver);
    }
}
