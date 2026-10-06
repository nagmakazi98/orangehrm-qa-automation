# OrangeHRM QA Automation Framework

[![QA Automation CI](https://github.com/nagmakazi98/orangehrm-qa-automation/actions/workflows/qa-automation.yml/badge.svg)](https://github.com/nagmakazi98/orangehrm-qa-automation/actions/workflows/qa-automation.yml)
[![Java 11](https://img.shields.io/badge/Java-11-blue.svg)](https://www.oracle.com/java/)
[![Selenium 4.21](https://img.shields.io/badge/Selenium-4.21.0-green.svg)](https://www.selenium.dev/)
[![TestNG 7.10](https://img.shields.io/badge/TestNG-7.10.2-red.svg)](https://testng.org/)
[![REST Assured 5.4](https://img.shields.io/badge/REST%20Assured-5.4.0-orange.svg)](https://rest-assured.io/)

---

## 1. Project Overview

Enterprise-grade test automation framework for validating the OrangeHRM Human Resource Management application. The framework covers UI workflows, REST API contract validation, and role-based access control across two user personas (Admin and ESS).

**Application under test:** https://opensource-demo.orangehrmlive.com

The framework is designed to run reliably against a shared public sandbox that is subject to concurrent access, periodic database resets, and variable network latency. Every design decision — from independent test setup to automatic retry — addresses the unique challenges that come with testing a live public demo environment.

---

## 2. Technology Stack

| Component | Technology | Version |
|---|---|---|
| Language | Java | 11 |
| Test runner | TestNG | 7.10.2 |
| Browser automation | Selenium WebDriver | 4.21.0 |
| Driver management | WebDriverManager | 5.8.0 |
| API testing | REST Assured | 5.4.0 |
| JSON parsing | Jackson Databind | 2.17.1 |
| HTML reporting | ExtentReports (Spark) | 5.1.1 |
| Allure reporting | Allure TestNG adapter | 2.27.0 |
| Logging | Log4j2 | 2.23.1 |
| Video capture | Monte Screen Recorder | 0.7.7.0 |
| Build tool | Apache Maven | 3.8+ |
| CI/CD | GitHub Actions | — |

---

## 3. Architecture

### Execution Flow

`
Test Method
  │
  ├─► @BeforeMethod  ──► API: create employee precondition (where needed)
  │
  ├─► Page Object Layer  ──►  Selenium WebDriver  ──►  OrangeHRM UI
  │        │
  │        └─► WaitUtils (explicit waits, no Thread.sleep)
  │
  ├─► API Client Layer   ──►  REST Assured  ──►  OrangeHRM v2 REST API
  │        │
  │        └─► Session cookies shared from active WebDriver instance
  │
  ├─► Assertions (TestNG Assert)
  │
  ├─► @AfterMethod(alwaysRun=true)  ──►  API: delete employee (cleanup)
  │
  └─► TestListener ──►  ExtentReports + Allure + Log4j2
                        (screenshot attached on failure)
`

### CI/CD Flow

`
GitHub Push / PR / workflow_dispatch
  │
  ├─► Smoke Job (PR only, Chrome)  ──►  testng-smoke.xml
  │
  └─► Test Matrix Job (Chrome + Firefox, parallel)
        │
        ├─► JDK 11 setup + Maven cache
        ├─► Browser + WebDriver binary setup
        ├─► Xvfb virtual display (:99, 1920x1080)
        ├─► mvn clean test -Denv=qa -Dheadless=true -Dbrowser=<browser>
        └─► Upload artifacts (Allure results, Extent HTML, screenshots, logs, videos)
`

---

## 4. Framework Structure

`
qa-automation-framework/
├── .github/
│   └── workflows/
│       └── qa-automation.yml           # GitHub Actions CI/CD pipeline
├── pom.xml                             # Maven build, dependencies, Surefire config
├── testng.xml                          # Full regression suite (all groups)
├── testng-smoke.xml                    # Smoke suite (group filter: smoke)
└── src/
    ├── main/
    │   ├── java/com/orangehrm/
    │   │   ├── api/
    │   │   │   ├── OrangeHrmApiClient.java     # OrangeHRM v2 REST API client
    │   │   │   └── models/                     # Strongly-typed Jackson DTO models
    │   │   │       ├── EmployeeListResponse.java
    │   │   │       ├── EmployeeSummaryDto.java
    │   │   │       ├── PersonalDetailsResponse.java
    │   │   │       ├── PersonalDetailsDto.java
    │   │   │       ├── JobDetailsResponse.java
    │   │   │       ├── JobDetailsDto.java
    │   │   │       ├── JobTitleDto.java
    │   │   │       └── EmploymentStatusDto.java
    │   │   ├── base/
    │   │   │   ├── BaseTest.java               # Suite/class lifecycle hooks, driver wiring
    │   │   │   └── DriverFactory.java          # ThreadLocal WebDriver factory
    │   │   ├── pages/                          # Page Objects (all extend BasePage)
    │   │   │   ├── BasePage.java               # Shared wait helpers, JS utilities, logging
    │   │   │   ├── LoginPage.java
    │   │   │   ├── DashboardPage.java
    │   │   │   ├── PimPage.java
    │   │   │   ├── AddEmployeePage.java
    │   │   │   └── PersonalDetailsPage.java
    │   │   ├── listeners/
    │   │   │   ├── TestListener.java            # ExtentReports + Allure + screenshot wiring
    │   │   │   ├── RetryAnalyzer.java           # Configurable retry (IRetryAnalyzer)
    │   │   │   └── RetryAnnotationTransformer.java  # Applies retry to all @Test methods
    │   │   └── utils/
    │   │       ├── ConfigReader.java            # 4-tier property resolution
    │   │       ├── SessionManager.java          # Multi-user login/logout helpers
    │   │       ├── ApiHelper.java               # Backward-compatible OrangeHrmApiClient adapter
    │   │       ├── DataFactory.java             # Runtime unique employee data generator
    │   │       ├── EmployeeData.java            # Employee POJO
    │   │       ├── JsonDataReader.java          # Jackson-backed JSON loader (classpath + FS)
    │   │       ├── WaitUtils.java               # Explicit wait utilities (zero Thread.sleep)
    │   │       └── ScreenRecorderUtil.java      # Monte Media video capture
    │   └── resources/
    │       └── log4j2.xml                      # Log4j2: console + file appenders
    └── test/
        ├── java/com/orangehrm/tests/
        │   ├── EmployeeLifecycleTest.java       # Employee CRUD lifecycle (7 tests)
        │   └── RoleBasedAccessTest.java         # RBAC: Admin vs ESS persona (3 tests)
        └── resources/
            ├── config.properties               # Framework config (browser, timeouts, URLs)
            ├── config-qa.properties            # QA environment overrides
            ├── config-dev.properties           # Dev environment overrides
            ├── config-stage.properties         # Stage environment overrides
            ├── testdata.properties             # Test data defaults (name pool, job title, ID prefix)
            ├── allure.properties               # Allure results directory
            ├── testdata/
            │   └── employee.json              # JSON template (identity fields overwritten at runtime)
            └── images/
                └── profile.png               # Avatar image for upload tests
`

---

## 5. Test Coverage

### Employee Lifecycle (EmployeeLifecycleTest)

Each test method is fully independent. Setup and cleanup use the REST API.

| # | Method | Groups | What it validates |
|---|---|---|---|
| 1 | 	estLogin | smoke, egression, ui | Valid credentials authenticate; Dashboard URL is reached |
| 2 | 	estAddNewEmployee | egression, ui | PIM Add Employee form submission; API cross-validates firstName/lastName/employeeId |
| 3 | 	estVerifyEmployeeCreated | egression, ui | API-created employee is findable in PIM search grid |
| 4 | 	estEditEmployeeInformation | egression, ui | Job Title and Employment Status updates persist; API reads updated values back |
| 5 | 	estValidateEmployeeViaApi | egression, pi | API-created employee is verifiable via /personal-details and /pim/employees list |
| 6 | 	estDeleteEmployee | egression, ui | Deleted employee disappears from PIM grid; API returns non-200 for deleted record |
| 7 | 	estLogout | smoke, egression, ui | Logout invalidates session; direct Dashboard URL redirects back to Login |

### Role-Based Access Control (RoleBasedAccessTest)

| # | Method | Groups | What it validates |
|---|---|---|---|
| 1 | 	estAdminCanAccessPimAndEmployeeManagement | smoke, egression, ole, ui | Admin sees PIM menu, navigates to PIM, sees Add/Search buttons |
| 2 | 	estEssUserRestrictedFromPimAndAdminMenus | egression, ole, ui | ESS user cannot see PIM or Admin navigation menus |
| 3 | 	estEssUserDirectUrlAccessToProtectedPimDenied | egression, ole, ui | Direct URL navigation to PIM employee list is blocked for ESS persona |

---

## 6. API Validation

The framework integrates directly with OrangeHRM's internal REST API (/web/index.php/api/v2/) using REST Assured.

### Session Synchronization

OrangeHrmApiClient extracts authenticated session cookies directly from the active Selenium WebDriver instance (driver.manage().getCookies()). This means API calls execute under the exact same authenticated identity as the browser session — no separate login is required.

If the session cookie is absent or expired (HTTP 401), the client automatically re-authenticates via the POST /web/index.php/auth/validate endpoint.

### Endpoints Used

| Method | Endpoint | Used For |
|---|---|---|
| POST | /web/index.php/api/v2/pim/employees | Create employee (test setup) |
| GET | /web/index.php/api/v2/pim/employees | Search employee list |
| GET | /web/index.php/api/v2/pim/employees/{empNumber}/personal-details | Read and cross-validate name, employeeId |
| GET | /web/index.php/api/v2/pim/employees/{empNumber}/job-details | Read and cross-validate job title |
| DELETE | /web/index.php/api/v2/pim/employees (bulk by ids) | Delete employee (test cleanup) |
| POST | /web/index.php/api/v2/admin/users | Create ESS test user (RBAC setup) |
| DELETE | /web/index.php/api/v2/admin/users (bulk by ids) | Delete ESS test user (RBAC cleanup) |

### API Limitation

Profile picture upload is not supported by the OrangeHRM v2 REST API. The 	estAddNewEmployee test uploads the avatar through the UI only.

---

## 7. Role-Based Testing

The RBAC suite validates two distinct user personas.

**Admin persona** — created by logging in with the configured admin credentials. Asserts:
- PIM navigation menu is visible
- /pim/viewEmployeeList URL is accessible
- Add Employee button and Search button are present

**ESS persona** — a dedicated ESS user is provisioned at @BeforeClass via the API (POST /web/index.php/api/v2/admin/users, userRoleId=2) linked to a freshly API-created employee. If API provisioning fails (demo throttling), the configured ess.username / ess.password fallback is used. Asserts:
- PIM menu is NOT visible
- Admin menu is NOT visible
- Direct navigation to /pim/viewEmployeeList does NOT render the employee table or Add button

Both the ESS user and the linked employee are deleted via API in @AfterClass(alwaysRun=true).

---

## 8. Test Data Strategy

### Four-Layer Data Separation

| Layer | File | Contains |
|---|---|---|
| Framework config | config.properties / config-<env>.properties | Browser, headless flag, timeouts, base URLs |
| Credentials | config.properties + environment variables | login.username, login.password, ess.username, ess.password |
| Test data defaults | 	estdata.properties | Job title, employment status, first-name pool, ID prefix |
| Generated runtime data | DataFactory (in-memory) | Unique first name, last name, employee ID — never stored to disk |

### Uniqueness Strategy

DataFactory.randomEmployee() generates per-test unique records:

`
First name  → random pick from configurable pool (Alex, Jordan, Morgan, Taylor, …)
Last name   → "Test" + 6-character UUID hex suffix (e.g. "Test3fa2c1")
Employee ID → configurable prefix (default "A") + 8-digit zero-padded atomic counter
              total length = 9 characters — within OrangeHRM's field limit
              e.g. "A01234567"
`

- The UUID hex suffix in the last name is process-independent, so parallel JVM forks never collide.
- The atomic counter ensures ordering within a single JVM and is seeded from the millisecond clock mod 99,000,000 to diverge across runs.
- The first-name pool produces readable output in reports instead of machine-generated strings.

### fromJson() Is Safe for Parallel Use

DataFactory.fromJson() loads 	estdata/employee.json as a template for default values (job title, profile picture path), then **always overwrites** irstName, lastName, and employeeId with andomEmployee() values before returning the object. The JSON file can never cause ID collisions.

### Runtime Overrides

`ash
# Change job title used in Edit Employee tests
mvn test -Dtestdata.employee.updatedJobTitle="Automation Tester"

# Change first-name pool
mvn test -Dtestdata.firstname.pool="Sam,Alex,Lee"

# Change employee ID prefix
mvn test -Dtestdata.employeeid.prefix=T
`

---

## 9. Environment Configuration

Configuration is resolved in strict priority order:

`
1. -D system property (highest — always wins in CI and CLI)
2. Environment variable (ORANGEHRM_USERNAME, ORANGEHRM_PASSWORD, etc.)
3. config-<env>.properties (selected by -Denv=qa|dev|stage)
4. config.properties (base fallback)
5. Coded default in ConfigReader (lowest)
`

### Available Properties (config.properties)

`properties
# Application
env=qa
base.url=https://opensource-demo.orangehrmlive.com/
browser=chrome
headless=false
implicit.wait.seconds=10
explicit.wait.seconds=20
page.load.timeout.seconds=30

# API
api.base.url=https://opensource-demo.orangehrmlive.com

# Credentials (override via env vars in CI — never commit real secrets)
login.username=Admin
login.password=admin123
ess.username=linda.anderson
ess.password=admin123

# Paths
image.profile.path=src/test/resources/images/profile.png

# Reporting
screenshot.dir=test-output/screenshots
video.dir=reports/videos
extent.report.path=reports/ExtentReport.html

# Retry (0 = disabled, max 3)
retry.count=1
`

### Credential Environment Variables (CI)

| Environment Variable | Maps To |
|---|---|
| ORANGEHRM_USERNAME | login.username |
| ORANGEHRM_PASSWORD | login.password |
| ORANGEHRM_ESS_USERNAME | ess.username |
| ORANGEHRM_ESS_PASSWORD | ess.password |

Set these in **GitHub → Repository → Settings → Secrets and variables → Actions**.

---

## 10. Running Tests

### Prerequisites

- Java 11+
- Maven 3.8+
- Chrome or Firefox installed (WebDriverManager downloads the matching driver binary automatically)

### Full Regression Suite

`ash
mvn clean test
`

### Full Suite — Headless

`ash
mvn clean test -Dheadless=true
`

### Smoke Suite Only

`ash
mvn clean test -DsuiteXmlFile=testng-smoke.xml -Dheadless=true
`

### Run by Group

`ash
# Smoke tests (login + logout + admin access)
mvn clean test -Dgroups=smoke

# Full regression
mvn clean test -Dgroups=regression

# API validation tests only
mvn clean test -Dgroups=api

# Role-based access tests only
mvn clean test -Dgroups=role

# Combine groups
mvn clean test -Dgroups="regression,api"
`

### Cross-Browser

`ash
# Firefox (headless)
mvn clean test -Dbrowser=firefox -Dheadless=true

# Chrome (headless)
mvn clean test -Dbrowser=chrome -Dheadless=true
`

> **Note:** Only chrome and irefox are supported. Edge is not wired in DriverFactory.

### Against a Different Environment

`ash
mvn clean test -Denv=qa
mvn clean test -Denv=stage
mvn clean test -Denv=dev
`

### Run a Single Test Class or Method

`ash
mvn clean test -Dtest=EmployeeLifecycleTest
mvn clean test -Dtest=RoleBasedAccessTest
mvn clean test -Dtest=EmployeeLifecycleTest#testAddNewEmployee
`

> Note: -Dtest= uses Surefire method filtering. The TestNG suite XML listeners (retry, Allure) are still active because they are also declared via @Listeners on BaseTest.

### CI Maven Command (as used in GitHub Actions)

`ash
mvn clean test \
  -Denv=qa \
  -Dheadless=true \
  -Dbrowser=chrome \
  -DsuiteXmlFile=testng.xml \
  --no-transfer-progress -B
`

---

## 11. TestNG Groups

Tests are tagged with groups at the @Test annotation level. Groups can be selected without editing any Java source file.

| Group | Tests | Purpose |
|---|---|---|
| smoke | 	estLogin, 	estLogout, 	estAdminCanAccessPimAndEmployeeManagement | Fast sanity gate — run on every PR before full matrix |
| egression | All 10 tests | Full end-to-end functional regression |
| pi | 	estValidateEmployeeViaApi | REST API contract validation only |
| ole | All 3 RoleBasedAccessTest methods | RBAC / authorization boundary checks |
| ui | All 7 EmployeeLifecycleTest + all 3 RoleBasedAccessTest | Browser UI tests |

### XML-Level Group Filter

Uncomment the <groups> block in 	estng.xml to run specific groups without a CLI flag:

`xml
<groups>
    <run>
        <include name="smoke"/>
    </run>
</groups>
`

### Suite Files

| File | Contents |
|---|---|
| 	estng.xml | Full suite — all classes, all groups, listeners registered |
| 	estng-smoke.xml | Smoke gate — filters smoke group from both test classes |

---

## 12. Reports and Artifacts

### ExtentReports (HTML Dashboard)

Generated at: eports/ExtentReport.html

Each test node shows:
- Environment name and active browser
- TestNG groups (categories)
- Test data logged during the run (Employee ID, API endpoints, retry attempts)
- Pass/Fail/Skip status with stack trace
- Failure screenshot embedded inline (PNG)

Credentials and secrets are never logged — TestListener.logTestData() masks any key containing password, secret, 	oken, uth, or credential.

### Allure Reports

Raw results are written to 	arget/allure-results.

`ash
# Generate and open interactive HTML report
mvn allure:serve

# Generate static HTML only
mvn allure:report
# Open: target/site/allure-maven-plugin/index.html
`

Each test carries Allure annotations:
- @Epic, @Feature, @Story for hierarchical categorization
- @Severity (BLOCKER / CRITICAL / NORMAL)
- @Allure.parameter for Environment and Browser
- Failure screenshots attached as binary attachments

### Execution Logs

- Log file: logs/test-run.log (Log4j2 File appender, overwritten at the start of each run; no rotation)
- Console output mirrors the file appender

### Video Recordings

ScreenRecorderUtil (Monte Media) starts one AVI recording per suite (@BeforeSuite) and stops it at @AfterSuite. The file is written to eports/videos/.

**Video behavior:**
- Recordings are generated per-suite, not per-test. A single .avi covers the entire suite run.
- Video recording is activated unconditionally — there is no config flag to disable it without modifying BaseTest.
- On Linux (CI), the Xvfb virtual display is required for recording to capture frames. The file is uploaded to GitHub Actions artifacts with a 7-day retention period.
- Generated .avi files are excluded from Git via .gitignore.

### Screenshot on Failure

TestListener.onTestFailure() captures a screenshot via TakesScreenshot, saves it to 	est-output/screenshots/<methodName>_<timestamp>.png, embeds it in the Extent report, and attaches it to Allure. Screenshots are captured for any test failure, including failures during retry attempts.

---

## 13. CI/CD

Pipeline file: .github/workflows/qa-automation.yml

### Triggers

| Event | Condition | Behavior |
|---|---|---|
| push | main or develop branches (non-doc changes) | Runs full matrix (Chrome + Firefox) |
| pull_request | main or develop | Runs smoke gate first, then full matrix |
| workflow_dispatch | Manual via GitHub UI | Selectable suite, groups, and environment |

### Manual Dispatch Inputs

| Input | Default | Options |
|---|---|---|
| 	est_suite | 	estng.xml | 	estng.xml, 	estng-smoke.xml, 	estng-individual.xml |
| 	est_groups | *(blank = all)* | smoke, egression, ole, pi |
| environment | qa | qa, stage, dev |

### Jobs

**smoke job** (PR only):
- Chrome only, ubuntu-latest
- Runs 	estng-smoke.xml
- Uploads screenshots artifact

**	est matrix job**:
- Parallel across chrome and irefox with ail-fast: false
- ubuntu-latest runner with Xvfb virtual display (:99, 1920x1080x24)
- JDK 11 Temurin + Maven dependency cache

### Artifacts Uploaded (all with if: always())

| Artifact | Content | Retention |
|---|---|---|
| llure-results-<browser> | Raw Allure JSON | 30 days |
| llure-report-<browser> | Generated Allure HTML | 30 days |
| extent-report-<browser> | ExtentReports HTML dashboard | 30 days |
| screenshots-<browser> | Failure screenshots | 30 days |
| 	est-logs-<browser> | logs/test-run.log | 30 days |
| ideos-<browser> | Suite AVI recording | 7 days |
| surefire-reports-<browser> | TestNG XML surefire output | 14 days |
| smoke-screenshots | Screenshots from PR smoke gate | Default |

Artifacts are uploaded even when tests fail, enabling post-failure diagnosis without re-running.

### Status Badge

Replace <owner>/<repo> with your GitHub repository path:

`markdown
[![QA Automation CI](https://github.com/<owner>/<repo>/actions/workflows/qa-automation.yml/badge.svg)](https://github.com/<owner>/<repo>/actions/workflows/qa-automation.yml)
`

---

## 14. Stability / Flaky Test Strategy

### Why OrangeHRM Demo Tests Can Be Flaky

1. **Shared public sandbox** — The demo instance is accessed by thousands of automated scripts and manual users simultaneously. Data created by other users may collide with or delete test data mid-run.
2. **Periodic database resets** — The demo server periodically re-seeds its database, which can invalidate active sessions or wipe records mid-execution.
3. **Asynchronous DOM hydration** — OrangeHRM uses client-side rendering (Vue.js). Elements appear in the DOM before event listeners are attached; animated overlays (.oxd-loading-spinner, form loaders) temporarily intercept clicks.
4. **Network variability** — Variable response times from cloud hosting can cause transient HTTP 429/504 errors or slow asset loading.

### Synchronization Strategy

- **Zero Thread.sleep() policy** — No hardcoded pauses. Every wait is event-driven.
- **Explicit waits via WaitUtils** — waitForVisibility, waitForElementToBeClickable, waitForInvisibility (spinner/overlay disappearance), waitForUrlContains.
- **StaleElementReferenceException resilience** — Grid interactions re-locate elements after DOM updates. JavaScript fallbacks (jsClick, scrollIntoView) are used when native clicks are intercepted.
- **Implicit wait is set to 0** — DriverFactory explicitly sets implicitlyWait(Duration.ZERO) so that explicit waits are the sole synchronization mechanism.

### Retry Policy

Retry behavior is implemented in RetryAnalyzer and applied globally by RetryAnnotationTransformer:

- **Configurable count** — etry.count=1 in config.properties (or -Dretry.count=N). Clamped to the range 0–3.
- **AssertionError bypass** — Tests that fail with an AssertionError are **never retried**. An assertion failure means the product did not behave as expected — that is a defect, not transient infrastructure noise.
- **Retries target** — WebDriverException, socket timeouts, StaleElementReferenceException, and demo-server hiccups.

**Log4j2 retry markers:**

`
WARN  [ORIGINAL FAILURE]   — first failure; retry is scheduled
WARN  [RETRY ATTEMPT n/N]  — subsequent failure; another retry will follow
ERROR [FINAL FAILURE]      — all retries exhausted; test is FAILED
ERROR [ASSERTION FAILURE]  — product defect detected; retries bypassed
`

### Dynamic Data Strategy

- Each test generates a fully unique employee at runtime via DataFactory.randomEmployee().
- No test relies on data left by a previous test.
- @AfterMethod(alwaysRun=true) cleans up any employee created during the test, regardless of pass/fail.

### Quarantine Policy

A test must be moved to group quarantine and excluded from the active suite when:

- It fails or requires retries in more than 10% of CI runs over a rolling 7-day period.
- Its failure mode is a genuine race condition or unhandled UI state — not fixed by increasing the retry count.
- A known backend or infrastructure outage makes the test unreliable.

**Quarantine procedure:**
1. Assign @Test(groups = "quarantine") and remove it from 	estng.xml.
2. File a defect or framework issue with full logs, screenshots, and Allure traces.
3. Investigate in an isolated branch.
4. Reinstate to the main suite only after 20+ consecutive stable runs.

---

## 15. Cleanup Strategy

Every test that creates an employee registers its internal empNumber and guarantees cleanup.

### Mechanism

`java
// Registered per-test
private int createdEmpNumber = -1;

@BeforeMethod(alwaysRun = true)
public void setupMethod() {
    createdEmpNumber = -1;            // reset tracker
    apiClient.setSessionFromDriver(driver);
}

@AfterMethod(alwaysRun = true)
public void cleanupEmployee() {
    if (createdEmpNumber > 0) {
        try {
            apiClient.deleteEmployee(createdEmpNumber);
            log.info("Cleanup: deleted empNumber={}", createdEmpNumber);
        } catch (Exception e) {
            log.warn("Cleanup failed for empNumber={}: {}", createdEmpNumber, e.getMessage());
        } finally {
            createdEmpNumber = -1;    // always reset
        }
    }
}
`

### Cleanup Contract

1. Check createdEmpNumber > 0 before attempting any API call — no spurious DELETE requests.
2. Call piClient.deleteEmployee(empNumber) via the OrangeHRM bulk-delete REST endpoint.
3. Log the result (INFO on success, WARN on exception) via Log4j2.
4. Catch all exceptions in cleanup — they are logged but **never rethrown**, so the original test failure reason is always preserved.
5. Reset createdEmpNumber = -1 in the inally block — cleanup runs once, not twice.

### Special Case: testDeleteEmployee

This test deletes the employee through the UI as part of its assertion. It resets createdEmpNumber = -1 after confirming deletion succeeds, so @AfterMethod does not attempt a redundant API delete.

### RBAC Cleanup

RoleBasedAccessTest uses @AfterClass(alwaysRun=true) to delete both the dynamically provisioned ESS user and the linked employee via the admin API after all three role tests complete.

---

## 16. Known Limitations

| Limitation | Root Cause | Impact |
|---|---|---|
| Profile picture upload not API-testable | OrangeHRM v2 REST API has no file upload endpoint for profile pictures | 	estAddNewEmployee exercises the upload through the UI only; the API cross-check validates name/ID but not the avatar |
| Shared public sandbox collisions | Multiple concurrent users may create/delete conflicting data | Mitigated by UUID-suffix names and unique IDs, but not fully eliminatable |
| Demo database resets mid-run | OrangeHRM demo instance resets periodically | May cause 401s or missing records; handled by automatic session refresh in OrangeHrmApiClient |
| No Edge browser support | DriverFactory only switches on chrome and irefox; Edge falls through to the Chrome default | Run with -Dbrowser=chrome or -Dbrowser=firefox only |
| Video is one file per suite | ScreenRecorderUtil starts/stops once per suite in BaseTest | A single AVI covers the full run; per-test clip extraction is not supported |
| Video cannot be disabled via config | BaseTest.@BeforeSuite calls ScreenRecorderUtil.startRecording() unconditionally | Requires BaseTest code change to disable recording |
| Suite-level parallelism not tested | 	estng.xml uses parallel="none" | DriverFactory is ThreadLocal-safe for parallel execution, but the suites have not been validated at parallel="methods" |
| 9-character employee ID field limit | OrangeHRM enforces a max length on the Employee ID field | Employee IDs are generated as 1-char prefix + 8 digits = 9 chars total; prefix and counter range are constrained accordingly |

---

## 17. Design Decisions

**Independent tests over shared state** — dependsOnMethods chaining was removed entirely. Each test creates its own prerequisites via the REST API (where supported) or via the UI (profile picture upload). A single failure cannot cascade to downstream tests.

**API session reuse instead of double login** — OrangeHrmApiClient extracts session cookies from the active Selenium WebDriver, so API calls operate under the same authenticated session as the browser. This avoids a second login round-trip and ensures the API and UI see the same server-side state.

**AssertionError is not retryable** — RetryAnalyzer inspects the thrown Throwable. If it is an AssertionError, retries are unconditionally skipped. This design decision prevents flaky retry policies from silently hiding genuine product defects.

**Zero implicit wait** — DriverFactory sets implicitlyWait to zero. Mixing implicit and explicit waits in Selenium produces unpredictable timeout behavior. All synchronization goes through WaitUtils explicit conditions.

**ThreadLocal driver for parallelism** — DriverFactory stores the WebDriver in a ThreadLocal. No static mutable driver reference is used. The design is ready for parallel="tests" or parallel="methods" in TestNG without code changes.

**testdata.properties as a separate layer** — Job titles, employment statuses, and name pools are test data, not framework configuration. Placing them in a separate file (	estdata.properties) means test engineers can update test data inputs without touching browser/environment config files.

**RetryAnnotationTransformer instead of per-test etryAnalyzer=** — Applying RetryAnalyzer globally via IAnnotationTransformer eliminates repetitive annotation boilerplate on every @Test method. The transformer only sets the analyzer when one is not already configured, so individual tests can still opt out or use a custom implementation.

**ExtentReports + Allure in parallel** — Extent provides an immediate, self-contained HTML dashboard that requires no CLI tool to open. Allure provides deeper drill-down (steps, parameters, history trends). Both are generated on every run; Extent for day-to-day use, Allure for regression analysis and CI artifact storage.
