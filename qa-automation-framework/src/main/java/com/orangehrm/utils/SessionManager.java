package com.orangehrm.utils;

import com.orangehrm.pages.DashboardPage;
import com.orangehrm.pages.LoginPage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Reusable authentication and session management mechanism.
 *
 * <p>Handles clean session switching between different user personas (e.g. Admin, ESS)
 * without state bleeding or session carry-over across test executions.</p>
 */
public class SessionManager {

    private static final Logger log = LogManager.getLogger(SessionManager.class);

    private SessionManager() {
        // utility class
    }

    /**
     * Ensures any existing active browser session is terminated and returns to a fresh Login page.
     *
     * @param driver Active WebDriver instance
     * @return {@link LoginPage} ready for input
     */
    public static LoginPage ensureLoggedOut(WebDriver driver) {
        log.info("Ensuring session is logged out and navigating to Login page");
        try {
            DashboardPage dashboard = new DashboardPage(driver);
            String url = driver.getCurrentUrl();
            if (url != null && !url.contains("/auth/login") && (url.contains("dashboard") || url.contains("/web/index.php/"))) {
                try {
                    dashboard.logout();
                } catch (Exception e) {
                    log.debug("UI logout link could not be clicked; clearing browser cookies directly: {}", e.getMessage());
                    driver.manage().deleteAllCookies();
                    driver.get(ConfigReader.get("base.url"));
                }
            }
        } catch (Exception ignored) {
            driver.manage().deleteAllCookies();
            driver.get(ConfigReader.get("base.url"));
        }

        LoginPage loginPage = new LoginPage(driver);
        if (!loginPage.isAt()) {
            driver.get(ConfigReader.get("base.url"));
        }
        return loginPage;
    }

    /**
     * Authenticates as the configured Admin persona.
     *
     * @param driver Active WebDriver instance
     * @return {@link DashboardPage} for Admin
     */
    public static DashboardPage loginAsAdmin(WebDriver driver) {
        log.info("Logging in as Admin user");
        ensureLoggedOut(driver);
        LoginPage loginPage = new LoginPage(driver);
        return loginPage.loginAs(
                ConfigReader.get("login.username", "Admin"),
                ConfigReader.get("login.password", "admin123"));
    }

    /**
     * Authenticates as an ESS (Employee Self Service) user.
     *
     * @param driver Active WebDriver instance
     * @param username ESS username
     * @param password ESS password
     * @return {@link DashboardPage}
     */
    public static DashboardPage loginAsEss(WebDriver driver, String username, String password) {
        log.info("Logging in as ESS user '{}'", username);
        ensureLoggedOut(driver);
        LoginPage loginPage = new LoginPage(driver);
        return loginPage.loginAs(username, password);
    }

    /**
     * Authenticates as the default configured ESS persona.
     */
    public static DashboardPage loginAsDefaultEss(WebDriver driver) {
        return loginAsEss(driver,
                ConfigReader.get("ess.username", "linda.anderson"),
                ConfigReader.get("ess.password", "admin123"));
    }
}
