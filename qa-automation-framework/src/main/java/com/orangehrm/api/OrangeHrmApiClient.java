package com.orangehrm.api;

import com.orangehrm.api.models.EmployeeListResponse;
import com.orangehrm.api.models.EmployeeSummaryDto;
import com.orangehrm.api.models.JobDetailsDto;
import com.orangehrm.api.models.JobDetailsResponse;
import com.orangehrm.api.models.PersonalDetailsDto;
import com.orangehrm.api.models.PersonalDetailsResponse;
import com.orangehrm.utils.ConfigReader;
import com.orangehrm.utils.EmployeeData;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

/**
 * Dedicated API Client layer for interacting with the <strong>OrangeHRM v2 REST API</strong>.
 *
 * <p>Key Architecture Highlights:
 * <ul>
 *   <li><strong>Session Synchronization:</strong> Seamlessly captures authenticated session cookies
 *       (including the {@code orangehrm} session cookie) from an active Selenium {@link WebDriver}
 *       instance, allowing API calls to execute under the exact same user identity and context.</li>
 *   <li><strong>Standalone Authentication Fallback:</strong> If no WebDriver session is available,
 *       authenticates headlessly via {@code /web/index.php/auth/validate} using configured credentials.</li>
 *   <li><strong>Strongly Typed DTOs:</strong> Parses API responses into strongly-typed DTO models
 *       such as {@link EmployeeListResponse}, {@link EmployeeSummaryDto}, {@link PersonalDetailsDto},
 *       and {@link JobDetailsDto}.</li>
 *   <li><strong>Dual Access Mode:</strong> Exposes both typed DTO helper methods and raw {@link Response}
 *       accessors for low-level HTTP status code/header validations.</li>
 * </ul>
 * </p>
 */
public class OrangeHrmApiClient {

    protected static final Logger log = LogManager.getLogger(OrangeHrmApiClient.class);

    public static final String PIM_EMPLOYEES_ENDPOINT = "/web/index.php/api/v2/pim/employees";
    public static final String USERS_ENDPOINT         = "/web/index.php/api/v2/admin/users";

    protected final Map<String, String> sessionCookies = new HashMap<>();
    protected final String baseUrl;

    public OrangeHrmApiClient() {
        this(null);
    }

    public OrangeHrmApiClient(WebDriver driver) {
        String url = ConfigReader.get("api.base.url", ConfigReader.get("base.url", "https://opensource-demo.orangehrmlive.com"));
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.baseUrl = url;
        RestAssured.baseURI = this.baseUrl;

        if (driver != null) {
            setSessionFromDriver(driver);
        }
    }

    public OrangeHrmApiClient(String baseUrl, WebDriver driver) {
        String url = baseUrl != null ? baseUrl : ConfigReader.get("base.url", "https://opensource-demo.orangehrmlive.com");
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.baseUrl = url;
        RestAssured.baseURI = this.baseUrl;

        if (driver != null) {
            setSessionFromDriver(driver);
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Map<String, String> getSessionCookies() {
        return Collections.unmodifiableMap(sessionCookies);
    }

    /**
     * Extracts and synchronizes session cookies from the active Selenium WebDriver instance.
     *
     * @param driver Active WebDriver instance with authenticated session
     */
    public synchronized void setSessionFromDriver(WebDriver driver) {
        if (driver == null) {
            return;
        }
        try {
            for (Cookie cookie : driver.manage().getCookies()) {
                sessionCookies.put(cookie.getName(), cookie.getValue());
            }
            log.info("Synchronized OrangeHrmApiClient with {} cookies from active WebDriver session", sessionCookies.size());
        } catch (Exception e) {
            log.warn("Could not extract cookies from WebDriver: {}", e.getMessage());
        }
    }

    /**
     * Injects a specific cookie name and value into this client's session.
     */
    public synchronized void setCookie(String name, String value) {
        sessionCookies.put(name, value);
    }

    /**
     * Injects a bulk set of cookies into this client's session.
     */
    public synchronized void setCookies(Map<String, String> cookies) {
        if (cookies != null) {
            sessionCookies.putAll(cookies);
        }
    }

    /**
     * Clears all session cookies.
     */
    public synchronized void clearCookies() {
        sessionCookies.clear();
    }

    /**
     * Ensures an authenticated session is established.
     * If session cookies were not provided via WebDriver, executes a headless login against
     * the OrangeHRM auth endpoint.
     */
    public synchronized void ensureAuthenticated() {
        if (!sessionCookies.isEmpty()) {
            return;
        }

        try {
            log.info("No UI session cookies found in client; authenticating standalone via web login endpoint...");
            Response loginPageResp = given()
                    .baseUri(baseUrl)
                    .when()
                    .get("/web/index.php/auth/login");

            sessionCookies.putAll(loginPageResp.getCookies());

            String html = loginPageResp.getBody().asString();
            String token = null;
            Matcher m = Pattern.compile(":token=\"([^\"]+)\"").matcher(html);
            if (m.find()) {
                token = m.group(1);
            }

            String username = ConfigReader.get("login.username", "Admin");
            String password = ConfigReader.get("login.password", "admin123");

            RequestSpecification req = given()
                    .baseUri(baseUrl)
                    .cookies(sessionCookies)
                    .contentType("application/x-www-form-urlencoded");

            if (token != null) {
                req.formParam("_token", token);
            }
            req.formParam("username", username);
            req.formParam("password", password);

            Response authResp = req.when().post("/web/index.php/auth/validate");
            sessionCookies.putAll(authResp.getCookies());
            log.info("Standalone API authentication completed for user '{}'", username);
        } catch (Exception e) {
            log.warn("Standalone API authentication encountered an issue: {}", e.getMessage());
        }
    }

    /**
     * Clears expired session cookies and re-authenticates standalone.
     */
    public synchronized void refreshAuthentication() {
        log.warn("Session expired or invalid (HTTP 401); clearing stale cookies and re-authenticating standalone");
        sessionCookies.clear();
        ensureAuthenticated();
    }

    /**
     * Constructs a pre-configured, authenticated RequestSpecification.
     */
    protected RequestSpecification authenticated() {
        ensureAuthenticated();
        return given()
                .baseUri(baseUrl)
                .cookies(sessionCookies)
                .header("Accept", "application/json")
                .contentType(ContentType.JSON);
    }

    // ── Employee List & Query Operations ──────────────────────────────────────

    /**
     * Retrieves the employee list response from {@code GET /web/index.php/api/v2/pim/employees}.
     *
     * @param queryParams optional query parameters (limit, offset, etc.)
     * @return raw {@link Response}
     */
    public Response getEmployeesResponse(Map<String, ?> queryParams) {
        RequestSpecification req = authenticated();
        if (queryParams != null && !queryParams.isEmpty()) {
            req.queryParams(queryParams);
        }
        Response resp = req.when()
                .get(PIM_EMPLOYEES_ENDPOINT)
                .then()
                .extract()
                .response();
        if (resp.getStatusCode() == 401) {
            refreshAuthentication();
            RequestSpecification retryReq = authenticated();
            if (queryParams != null && !queryParams.isEmpty()) {
                retryReq.queryParams(queryParams);
            }
            resp = retryReq.when()
                    .get(PIM_EMPLOYEES_ENDPOINT)
                    .then()
                    .extract()
                    .response();
        }
        return resp;
    }

    /**
     * Retrieves the employee list strongly-typed as {@link EmployeeListResponse}.
     *
     * @param limit maximum records to retrieve
     * @param offset pagination offset
     * @return {@link EmployeeListResponse} DTO
     */
    public EmployeeListResponse getEmployees(int limit, int offset) {
        Map<String, Object> params = new HashMap<>();
        params.put("limit", limit);
        params.put("offset", offset);
        return getEmployeesResponse(params).as(EmployeeListResponse.class);
    }

    /**
     * Retrieves default paginated list of employees.
     */
    public EmployeeListResponse getEmployees() {
        return getEmployees(50, 0);
    }

    /**
     * Searches the PIM employees list for an employee with the specified {@code employeeId}.
     *
     * @param employeeId Employee ID string (e.g. "EMP12345")
     * @return {@link EmployeeSummaryDto} if found, or {@code null}
     */
    public EmployeeSummaryDto findEmployeeById(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return null;
        }

        // Try direct query if supported by instance
        Map<String, Object> params = new HashMap<>();
        params.put("employeeId", employeeId);
        params.put("limit", 50);
        Response response = getEmployeesResponse(params);
        if (response.getStatusCode() == 200) {
            EmployeeListResponse list = response.as(EmployeeListResponse.class);
            if (list.getData() != null) {
                for (EmployeeSummaryDto emp : list.getData()) {
                    if (employeeId.equalsIgnoreCase(emp.getEmployeeId())) {
                        return emp;
                    }
                }
            }
        }

        // Fallback: fetch without filter and match locally
        EmployeeListResponse all = getEmployees(100, 0);
        if (all.getData() != null) {
            for (EmployeeSummaryDto emp : all.getData()) {
                if (employeeId.equalsIgnoreCase(emp.getEmployeeId())) {
                    return emp;
                }
            }
        }
        return null;
    }

    /**
     * Finds an employee summary item by internal {@code empNumber}.
     */
    public EmployeeSummaryDto findEmployeeByEmpNumber(int empNumber) {
        if (empNumber <= 0) return null;
        EmployeeListResponse list = getEmployees(100, 0);
        if (list.getData() != null) {
            for (EmployeeSummaryDto emp : list.getData()) {
                if (emp.getEmpNumber() == empNumber) {
                    return emp;
                }
            }
        }
        return null;
    }

    // ── Personal Details Operations ───────────────────────────────────────────

    /**
     * Fetches raw HTTP {@link Response} for an employee's personal details.
     *
     * @param empNumber Internal OrangeHRM employee number
     */
    public Response getPersonalDetailsResponse(int empNumber) {
        Response resp = authenticated()
                .when()
                .get(PIM_EMPLOYEES_ENDPOINT + "/" + empNumber + "/personal-details")
                .then()
                .extract()
                .response();
        if (resp.getStatusCode() == 401) {
            refreshAuthentication();
            resp = authenticated()
                    .when()
                    .get(PIM_EMPLOYEES_ENDPOINT + "/" + empNumber + "/personal-details")
                    .then()
                    .extract()
                    .response();
        }
        return resp;
    }

    /**
     * Fetches strongly-typed {@link PersonalDetailsDto} for an employee.
     *
     * @param empNumber Internal OrangeHRM employee number
     * @return {@link PersonalDetailsDto}, or {@code null} if retrieval failed
     */
    public PersonalDetailsDto getPersonalDetails(int empNumber) {
        Response resp = getPersonalDetailsResponse(empNumber);
        if (resp.getStatusCode() == 200 || resp.getStatusCode() == 201) {
            PersonalDetailsResponse wrapper = resp.as(PersonalDetailsResponse.class);
            return wrapper != null ? wrapper.getData() : null;
        }
        log.warn("getPersonalDetails for empNumber {} returned HTTP {}: {}",
                empNumber, resp.getStatusCode(), resp.getBody().asString());
        return null;
    }

    // ── Job Details Operations ────────────────────────────────────────────────

    /**
     * Fetches raw HTTP {@link Response} for an employee's job details.
     *
     * @param empNumber Internal OrangeHRM employee number
     */
    public Response getJobDetailsResponse(int empNumber) {
        Response resp = authenticated()
                .when()
                .get(PIM_EMPLOYEES_ENDPOINT + "/" + empNumber + "/job-details")
                .then()
                .extract()
                .response();
        if (resp.getStatusCode() == 401) {
            refreshAuthentication();
            resp = authenticated()
                    .when()
                    .get(PIM_EMPLOYEES_ENDPOINT + "/" + empNumber + "/job-details")
                    .then()
                    .extract()
                    .response();
        }
        return resp;
    }

    /**
     * Fetches strongly-typed {@link JobDetailsDto} for an employee.
     *
     * @param empNumber Internal OrangeHRM employee number
     * @return {@link JobDetailsDto}, or {@code null} if retrieval failed
     */
    public JobDetailsDto getJobDetails(int empNumber) {
        Response resp = getJobDetailsResponse(empNumber);
        if (resp.getStatusCode() == 200 || resp.getStatusCode() == 201) {
            JobDetailsResponse wrapper = resp.as(JobDetailsResponse.class);
            return wrapper != null ? wrapper.getData() : null;
        }
        log.warn("getJobDetails for empNumber {} returned HTTP {}: {}",
                empNumber, resp.getStatusCode(), resp.getBody().asString());
        return null;
    }

    // ── Employee CRUD Operations ──────────────────────────────────────────────

    /**
     * Creates an employee via OrangeHRM v2 API.
     *
     * @param employee {@link EmployeeData} containing employee details
     * @return raw {@link Response}
     */
    public Response createEmployeeResponse(EmployeeData employee) {
        String middleName = employee.getMiddleName() != null ? employee.getMiddleName() : "";
        String payload = String.format(
                "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"middleName\":\"%s\",\"employeeId\":\"%s\"}",
                employee.getFirstName(), employee.getLastName(), middleName, employee.getEmployeeId());

        Response response = authenticated()
                .body(payload)
                .when()
                .post(PIM_EMPLOYEES_ENDPOINT)
                .then()
                .extract()
                .response();

        if (response.getStatusCode() == 401) {
            refreshAuthentication();
            response = authenticated()
                    .body(payload)
                    .when()
                    .post(PIM_EMPLOYEES_ENDPOINT)
                    .then()
                    .extract()
                    .response();
        }

        return response;
    }

    /**
     * Creates an employee via OrangeHRM v2 API and extracts their {@code empNumber}.
     *
     * @param employee {@link EmployeeData}
     * @return internal {@code empNumber} assigned by OrangeHRM, or -1 on failure
     */
    public int createEmployee(EmployeeData employee) {
        Response response = createEmployeeResponse(employee);
        int status = response.getStatusCode();
        if (status != 200 && status != 201) {
            log.warn("API createEmployee returned HTTP {}: {}", status, response.getBody().asString());
            return -1;
        }

        try {
            int empNumber = response.jsonPath().getInt("data.empNumber");
            log.info("API created employee successfully: empNumber={} name='{} {}'",
                    empNumber, employee.getFirstName(), employee.getLastName());
            return empNumber;
        } catch (Exception e) {
            log.warn("Could not parse empNumber from create response: {}", response.getBody().asString());
            return -1;
        }
    }

    /**
     * Deletes one or more employees using the OrangeHRM bulk-delete endpoint.
     *
     * @param empNumbers array of internal employee numbers
     * @return raw {@link Response}
     */
    public Response deleteEmployeeResponse(int... empNumbers) {
        if (empNumbers == null || empNumbers.length == 0) {
            return authenticated().body("{\"ids\":[]}").delete(PIM_EMPLOYEES_ENDPOINT);
        }

        StringBuilder ids = new StringBuilder("[");
        for (int i = 0; i < empNumbers.length; i++) {
            ids.append(empNumbers[i]);
            if (i < empNumbers.length - 1) ids.append(",");
        }
        ids.append("]");

        return authenticated()
                .body("{\"ids\":" + ids + "}")
                .when()
                .delete(PIM_EMPLOYEES_ENDPOINT)
                .then()
                .extract()
                .response();
    }

    /**
     * Deletes a single employee by {@code empNumber}.
     *
     * @param empNumber internal OrangeHRM employee number
     * @return {@code true} if HTTP 200/204
     */
    public boolean deleteEmployee(int empNumber) {
        if (empNumber <= 0) return false;
        Response resp = deleteEmployeeResponse(empNumber);
        boolean success = resp.getStatusCode() == 200 || resp.getStatusCode() == 204;
        log.info("API deleted employee empNumber={} — HTTP {} (success={})",
                empNumber, resp.getStatusCode(), success);
        return success;
    }

    /**
     * Deletes a list of employees.
     */
    public boolean deleteEmployees(List<Integer> empNumbers) {
        if (empNumbers == null || empNumbers.isEmpty()) return true;
        int[] arr = empNumbers.stream().mapToInt(Integer::intValue).toArray();
        Response resp = deleteEmployeeResponse(arr);
        return resp.getStatusCode() == 200 || resp.getStatusCode() == 204;
    }

    // ── User Management Operations ────────────────────────────────────────────
 
    /**
     * Creates an ESS user linked to an employee.
     *
     * @param username user login name
     * @param password user password
     * @param empNumber internal employee number to associate
     * @return created user ID, or -1 on failure
     */
    public int createEssUser(String username, String password, int empNumber) {
        return createUser(username, password, empNumber, 2);
    }

    /**
     * Creates an Admin user linked to an employee.
     */
    public int createAdminUser(String username, String password, int empNumber) {
        return createUser(username, password, empNumber, 1);
    }

    /**
     * Creates a user in OrangeHRM with specified role ID (1 = Admin, 2 = ESS).
     */
    public int createUser(String username, String password, int empNumber, int userRoleId) {
        String payload = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"status\":true,\"userRoleId\":%d,\"empNumber\":%d}",
                username, password, userRoleId, empNumber);

        Response response = authenticated()
                .body(payload)
                .when()
                .post(USERS_ENDPOINT)
                .then()
                .extract()
                .response();

        int status = response.getStatusCode();
        if (status != 200 && status != 201) {
            log.warn("API createUser returned HTTP {}: {}", status, response.getBody().asString());
            return -1;
        }

        try {
            int userId = response.jsonPath().getInt("data.id");
            log.info("API created user successfully: id={} username='{}' roleId={}", userId, username, userRoleId);
            return userId;
        } catch (Exception e) {
            log.warn("Could not parse user id from createUser response: {}", response.getBody().asString());
            return -1;
        }
    }

    public int createUser(String username, String password, int empNumber, String role) {
        int roleId = "Admin".equalsIgnoreCase(role) ? 1 : 2;
        return createUser(username, password, empNumber, roleId);
    }

    public boolean deleteUser(int userId) {
        if (userId <= 0) return false;
        Response resp = authenticated()
                .body("{\"ids\":[" + userId + "]}")
                .when()
                .delete(USERS_ENDPOINT)
                .then()
                .extract()
                .response();
        return resp.getStatusCode() == 200 || resp.getStatusCode() == 204;
    }

    public void deleteUsers(int... userIds) {
        if (userIds == null || userIds.length == 0) return;

        StringBuilder ids = new StringBuilder("[");
        for (int i = 0; i < userIds.length; i++) {
            ids.append(userIds[i]);
            if (i < userIds.length - 1) ids.append(",");
        }
        ids.append("]");

        Response response = authenticated()
                .body("{\"ids\":" + ids + "}")
                .when()
                .delete(USERS_ENDPOINT)
                .then()
                .extract()
                .response();

        log.info("API deleted user(s) {} — HTTP {}", ids, response.getStatusCode());
    }
}
