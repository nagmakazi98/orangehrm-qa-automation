# OrangeHRM Employee Lifecycle — QA Automation Framework

A **Selenium + Java + TestNG** automation framework that executes an end-to-end
**Employee Lifecycle Management** scenario against the public OrangeHRM demo site.

> **Live demo site:** https://opensource-demo.orangehrmlive.com/
> **API validation layer:** https://reqres.in (simulated, per assessment guidance)

---

## Table of Contents

1. [What This Framework Tests](#1-what-this-framework-tests)
2. [Prerequisites](#2-prerequisites)
3. [Project Structure](#3-project-structure)
4. [One-Time Setup](#4-one-time-setup)
5. [How to Run the Tests](#5-how-to-run-the-tests)
6. [Viewing Reports](#6-viewing-reports)
7. [Configuration Reference](#7-configuration-reference)
8. [Test Case Details](#8-test-case-details)
9. [Troubleshooting](#9-troubleshooting)
10. [Dependencies](#10-dependencies)

---

## 1. What This Framework Tests

The suite runs **7 ordered test cases** that together cover the full employee lifecycle:

| # | Test Method | What It Does |
|---|---|---|
| 1 | `testLogin` | Logs in as Admin and verifies the dashboard loads |
| 2 | `testAddNewEmployee` | Fills the Add Employee form (name, ID, photo) and confirms redirect to Personal Details |
| 3 | `testVerifyEmployeeCreated` | Searches PIM by Employee ID and confirms the record exists |
| 4 | `testEditEmployeeInformation` | Opens the employee, updates Job Title + Employment Status, asserts success toast |
| 5 | `testValidateEmployeeViaApi` | Posts the same data to the ReqRes API, updates it, and cross-checks name + job title |
| 6 | `testDeleteEmployee` | Deletes the employee from PIM, verifies no records found, then deletes via API |
| 7 | `testLogout` | Logs out and verifies the session is invalidated (protected URL redirects to login) |

Each test **depends on the previous one** (`dependsOnMethods`), so they run in strict order
and a failing step skips all downstream steps — matching a realistic manual QA flow.

---

## 2. Prerequisites

Make sure **all** of the following are installed before you run anything.

### Required Software

| Tool | Version | How to verify |
|---|---|---|
| **JDK** | 11 or later | `java -version` |
| **Maven** | 3.8 or later | `mvn -version` |
| **Google Chrome** | Latest stable | Open Chrome → `chrome://settings/help` |
| **Internet access** | — | Must reach `opensource-demo.orangehrmlive.com` and `reqres.in` |

> **ChromeDriver** is managed automatically by **WebDriverManager** — you do **not** need to
> download or set it up manually.

### Check your setup (run these in a terminal)

```bash
java -version
# Expected: java version "11.x.x" or higher

mvn -version
# Expected: Apache Maven 3.8.x or higher
```

If either command fails, install the tool and add it to your system `PATH`.

---

## 3. Project Structure

```
qa-automation-framework/
├── pom.xml                          ← Maven build file (dependencies + plugins)
├── testng.xml                       ← TestNG suite: listeners, test class, order
├── README.md
│
├── src/
│   ├── main/java/com/orangehrm/
│   │   ├── base/
│   │   │   ├── DriverFactory.java   ← Creates/destroys WebDriver (Thread-safe)
│   │   │   └── BaseTest.java        ← @BeforeClass / @AfterClass / @AfterSuite hooks
│   │   ├── pages/                   ← Page Object Model
│   │   │   ├── LoginPage.java
│   │   │   ├── DashboardPage.java
│   │   │   ├── PimPage.java         ← Employee list: search, open, delete
│   │   │   ├── AddEmployeePage.java ← Add Employee form + photo upload
│   │   │   └── PersonalDetailsPage.java ← Job tab: edit Title & Status
│   │   └── utils/
│   │       ├── ConfigReader.java    ← Reads config.properties
│   │       ├── JsonDataReader.java  ← Reads JSON test data via Jackson
│   │       ├── EmployeeData.java    ← POJO matching employee.json
│   │       ├── WaitUtils.java       ← Explicit waits (no Thread.sleep)
│   │       ├── ApiHelper.java       ← REST Assured API calls (ReqRes)
│   │       └── ScreenRecorderUtil.java ← Monte Media video capture
│   │   └── listeners/
│   │       └── TestListener.java    ← ExtentReports + screenshot on failure
│   │
│   └── test/
│       ├── java/com/orangehrm/tests/
│       │   └── EmployeeLifecycleTest.java  ← The 7-step end-to-end test
│       └── resources/
│           ├── config.properties    ← All config: URL, browser, credentials
│           ├── testdata/
│           │   └── employee.json    ← Test data: names, IDs, job titles
│           └── images/
│               └── profile.png     ← Sample profile picture for upload
│
├── reports/
│   ├── ExtentReport.html            ← Generated HTML report after each run
│   └── videos/                     ← Screen recordings (.avi) of each run
│
└── test-output/
    └── screenshots/                ← Auto-captured screenshots on failure
```

---

## 4. One-Time Setup

### Step 1 — Clone the repository

```bash
git clone <your-repo-url>
cd qa-automation-framework/qa-automation-framework
```

### Step 2 — Download all dependencies

```bash
mvn clean install -DskipTests
```

This downloads Selenium, TestNG, REST Assured, Allure, and all other libraries into your
local Maven cache (`~/.m2`). **Run this only once** (or after changing `pom.xml`).

Expected output at the end:
```
[INFO] BUILD SUCCESS
```

### Step 3 — Verify config.properties

Open `src/test/resources/config.properties` and confirm these values:

```properties
# OrangeHRM demo site
base.url=https://opensource-demo.orangehrmlive.com/

# Browser to use: chrome | firefox
browser=chrome

# Set true for headless/CI runs, false for visible browser
headless=false

# Login credentials (shared demo site defaults)
login.username=Admin
login.password=admin123

# API validation base URL (ReqRes public test API)
api.base.url=https://reqres.in/api

# Timeout in seconds for explicit waits
explicit.wait.seconds=15
```

> You should not need to change anything here for a standard local run.

---

## 5. How to Run the Tests

Open a terminal **inside the project directory**
(`qa-automation-framework/qa-automation-framework`) for all commands below.

### Run the full suite (recommended)

```bash
mvn clean test
```

This will:
1. Compile all source code
2. Launch Chrome browser
3. Execute all 7 test cases in order
4. Generate reports in `reports/ExtentReport.html`
5. Save Allure raw results to `target/allure-results/`

**Expected output:**

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running Employee Lifecycle Management
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

### Run in headless mode (no browser window)

```bash
mvn clean test -Dheadless=true
```

Use this for CI/CD pipelines or if you want tests to run in the background without a
visible browser window.

---

### Run a single specific test method

```bash
mvn clean test -Dtest=EmployeeLifecycleTest#testLogin
```

Replace `testLogin` with any of these test method names:
- `testLogin`
- `testAddNewEmployee`
- `testVerifyEmployeeCreated`
- `testEditEmployeeInformation`
- `testValidateEmployeeViaApi`
- `testDeleteEmployee`
- `testLogout`

> **Note:** Because tests depend on each other (`dependsOnMethods`), running a downstream
> test in isolation may skip or fail. Run from the beginning for a complete flow.

---

### Run via IntelliJ IDEA

1. Open the project (`File → Open → select the `qa-automation-framework` folder`)
2. Wait for Maven to sync (progress bar in the bottom right)
3. Right-click `testng.xml` → **Run 'testng.xml'**

OR right-click `EmployeeLifecycleTest.java` → **Run 'EmployeeLifecycleTest'**

---

## 6. Viewing Reports

### ExtentReports (HTML — opens instantly after run)

```bash
# Open this file in any browser after a run:
reports/ExtentReport.html
```

This file is generated automatically after every `mvn clean test`. It shows:
- Pass / Fail status per test
- Execution timeline
- Screenshots embedded on failure

---

### Allure Report (interactive, with steps and severity)

**Generate and open in one command:**
```bash
mvn allure:serve
```

Or — generate only (no browser open):
```bash
mvn allure:report
# Then open: target/site/allure-maven-plugin/index.html
```

The Allure report shows:
- Test severity levels (Blocker / Critical / Normal)
- Story and Epic groupings
- Step-by-step breakdown
- Embedded screenshots on failure

---

### Screen Recording

When running with `headless=false` (visible browser), a video (`.avi`) of the
entire test run is saved to:
```
reports/videos/EmployeeLifecycleSuite.avi
```

On headless environments without a display, video capture is automatically skipped
with a console warning — the tests still complete normally.

---

### Screenshots on Failure

If any test fails, a screenshot is automatically taken and saved to:
```
test-output/screenshots/<TestName>_<timestamp>.png
```

The screenshot is also embedded directly in both the Extent and Allure reports.

---

## 7. Configuration Reference

All settings are in `src/test/resources/config.properties`:

| Property | Default Value | Description |
|---|---|---|
| `base.url` | `https://opensource-demo.orangehrmlive.com/` | OrangeHRM demo site URL |
| `browser` | `chrome` | Browser to use: `chrome` or `firefox` |
| `headless` | `false` | Run without a visible window (`true` for CI) |
| `login.username` | `Admin` | OrangeHRM login username |
| `login.password` | `admin123` | OrangeHRM login password |
| `api.base.url` | `https://reqres.in/api` | Base URL for API validation layer |
| `explicit.wait.seconds` | `15` | Timeout for all WebDriverWait calls |

**Test data** is in `src/test/resources/testdata/employee.json`:

```json
{
  "firstName": "Nagma",
  "lastName": "QATest",
  "employeeId": "QA10245",
  "profilePicture": "src/test/resources/images/profile.png",
  "updatedJobTitle": "Software Engineer",
  "updatedEmploymentStatus": "Full-Time Permanent"
}
```

> The Employee ID is overridden at runtime with a unique value (`QA` + timestamp) to
> avoid conflicts with other users on the shared demo site.

---

## 8. Test Case Details

### TC-1: Login (`testLogin`)
- Navigates to the base URL
- Asserts the login form is present
- Enters `Admin` / `admin123`
- Asserts the Dashboard header is displayed and URL contains `/dashboard`

### TC-2: Add New Employee (`testAddNewEmployee`)
- Navigates to PIM → Add Employee
- Fills: First Name, Last Name, Employee ID, uploads profile photo
- Clicks Save
- Asserts redirect to Personal Details page (URL check + header check)

### TC-3: Verify Employee Created (`testVerifyEmployeeCreated`)
- Returns to PIM Employee List
- Searches by the generated Employee ID
- Asserts the employee row appears in the results grid

### TC-4: Edit Employee Information (`testEditEmployeeInformation`)
- Opens the employee record from search results
- Navigates to the **Job** tab
- Selects a new Job Title and Employment Status from dropdowns
- Clicks Save
- Asserts the green success toast notification appears

### TC-5: Validate via API (`testValidateEmployeeViaApi`)
- Calls `POST /users` on ReqRes with the employee's name and job title → expects **201**
- Calls `PUT /users/{id}` to update → expects **200**
- Cross-checks that `name` and `job` fields in the API response match what was set in the UI
- Simulates API-UI consistency validation (ReqRes used per assessment guidance since
  the OrangeHRM demo has no open REST API without OAuth)

### TC-6: Delete Employee (`testDeleteEmployee`)
- Searches for the employee in PIM by Employee ID
- Deletes the record from the UI (trash icon or checkbox + Delete Selected)
- Confirms the dialog
- Searches again and asserts "No Records Found"
- Calls `DELETE /users/{id}` on ReqRes → expects **204**

### TC-7: Logout (`testLogout`)
- Clicks the user dropdown → Logout
- Asserts the login page is displayed again
- Navigates directly to the dashboard URL and asserts it redirects back to login
  (confirms the session is properly invalidated)

---

## 9. Troubleshooting

### `BUILD FAILURE` on `mvn clean install`
- Check your Java and Maven versions: `java -version` and `mvn -version`
- Ensure internet access is available (Maven downloads from Maven Central)
- Delete `target/` and retry: `mvn clean install -DskipTests`

### Chrome fails to launch
- Ensure Google Chrome is installed and up to date
- WebDriverManager automatically downloads a matching ChromeDriver. If it fails due to
  network restrictions, manually download ChromeDriver from https://chromedriver.chromium.org
  and place it on your `PATH`.
- Try running headless: `mvn clean test -Dheadless=true`

### `TimeoutException` during test execution
- The OrangeHRM demo site is a shared sandbox and can be slow
- Increase the wait timeout in `config.properties`: `explicit.wait.seconds=30`
- Check that the site is accessible: https://opensource-demo.orangehrmlive.com

### TC-2 fails: "Personal Details page did not load"
- The demo site can be slow to redirect after saving a new employee
- Increase `explicit.wait.seconds` to `30` or higher
- Verify the site is not in maintenance mode

### TC-3 fails: "Employee not found in PIM"
- The Employee ID may already exist (shared demo site). The test generates a unique ID
  at runtime, but collisions are possible. Re-run the test — it generates a fresh ID each time.

### TC-5 fails: API status code mismatch
- ReqRes is a public free API — it may occasionally be slow or unavailable
- Check https://reqres.in is reachable from your machine
- This test is isolated from the OrangeHRM UI state and does not affect TC-6 or TC-7

### Allure report is empty or missing
- Make sure you ran `mvn clean test` first to generate `target/allure-results/`
- Then run `mvn allure:serve`

---

## 10. Dependencies

| Library | Version | Purpose |
|---|---|---|
| Java | 11 | Language |
| Maven | 3.8+ | Build & dependency management |
| Selenium WebDriver | 4.21.0 | Browser automation |
| WebDriverManager (Bonigarcia) | 5.8.0 | Auto-manages ChromeDriver/GeckoDriver binaries |
| TestNG | 7.10.2 | Test runner, ordering, assertions, suite XML |
| ExtentReports (Spark) | 5.1.1 | Standalone HTML report (`reports/ExtentReport.html`) |
| Allure TestNG | 2.27.0 | Rich interactive HTML report with steps/severity |
| REST Assured | 5.4.0 | API request/response validation layer |
| Jackson Databind | 2.17.1 | Parses `employee.json` into `EmployeeData` POJO |
| Log4j2 | 2.23.1 | Application logging |
| Monte Screen Recorder | 0.7.7.0 | Desktop video capture during test run |
| AspectJ Weaver | 1.9.22 | Required by Allure for bytecode instrumentation |

---

## Quick Reference Card

```bash
# 1. One-time setup — download all dependencies
mvn clean install -DskipTests

# 2. Run all 7 tests (visible browser)
mvn clean test

# 3. Run headless (no browser window, good for CI)
mvn clean test -Dheadless=true

# 4. Run a single test method
mvn clean test -Dtest=EmployeeLifecycleTest#testLogin

# 5. Generate + open Allure report in browser
mvn allure:serve

# 6. View Extent report — open this file in a browser
reports/ExtentReport.html
```

---

## 1. Scenario Automated

| Step | Action |
|------|--------|
| 1 | Login with valid credentials (`Admin` / `admin123`) and verify the dashboard loads |
| 2 | Add a new employee via **PIM > Add Employee** using data-driven input (JSON), including First Name, Last Name, Employee Id, and a profile picture upload |
| 3 | Search the employee by Employee Id, edit **Job Title** and **Employment Status**, verify success |
| 4 | Validate employee data via an API layer and cross-check it against the UI |
| 5 | Delete the employee from the UI, verify removal via both UI and API |
| 6 | Logout and confirm the session is invalidated |

> **Note on the API step:** the public OrangeHRM demo instance does not expose an open REST API
> for employee CRUD without additional OAuth app registration that's outside the scope of a
> shared demo/sandbox account. As explicitly permitted by the assessment ("*or simulate API with
> any public test API like ReqRes*"), `ApiHelper` talks to **https://reqres.in** to exercise and
> assert the create/update/delete/response-validation layer end-to-end. All API calls are isolated
> in `ApiHelper` behind a single `api.base.url` config value — pointing this at a real OrangeHRM
> API host later requires no test-code changes, only adjusting the JSON field mapping.

---

## 2. Framework Structure

```
qa-automation-framework/
├── pom.xml                                # Maven dependencies & build/report plugins
├── testng.xml                             # TestNG suite definition + listeners
├── README.md
├── src/
│   ├── main/java/com/orangehrm/
│   │   ├── base/
│   │   │   ├── DriverFactory.java         # Thread-safe WebDriver creation (Chrome/Firefox)
│   │   │   └── BaseTest.java              # Shared TestNG lifecycle hooks
│   │   ├── pages/                         # Page Object Model
│   │   │   ├── LoginPage.java
│   │   │   ├── DashboardPage.java
│   │   │   ├── PimPage.java               # Employee List: search / delete / navigate to Add
│   │   │   ├── AddEmployeePage.java       # Add Employee form incl. picture upload
│   │   │   └── PersonalDetailsPage.java   # Employee profile / Job tab (edit Job Title & Status)
│   │   ├── utils/
│   │   │   ├── ConfigReader.java          # Reads config.properties
│   │   │   ├── JsonDataReader.java        # Generic Jackson-based JSON reader
│   │   │   ├── EmployeeData.java          # POJO for data-driven employee.json
│   │   │   ├── WaitUtils.java             # Centralized explicit waits
│   │   │   ├── ApiHelper.java             # REST Assured API layer / UI-API cross-check
│   │   │   └── ScreenRecorderUtil.java    # Monte Media screen recorder (video of test run)
│   │   └── listeners/
│   │       └── TestListener.java          # ExtentReports + Allure wiring, screenshot-on-failure
│   └── test/
│       ├── java/com/orangehrm/tests/
│       │   └── EmployeeLifecycleTest.java # The 7-step end-to-end test (Allure-annotated)
│       └── resources/
│           ├── config.properties          # Base URL, browser, timeouts, credentials, API URL
│           ├── testdata/employee.json     # Data-driven employee payload
│           └── images/profile.png         # Sample profile picture used for upload
├── reports/
│   ├── ExtentReport.html                  # Generated after a run
│   └── videos/                            # Generated screen recordings of the run
└── test-output/
    └── screenshots/                       # Auto-captured screenshots on test failure
```

**Design principles applied:**
- **Page Object Model** — every page's locators + actions live in one class; tests never touch
  a `By` locator directly.
- **Fluent, reusable methods** — page objects return the next logical page object (e.g.
  `loginAs()` returns `DashboardPage`), keeping test code readable as a linear story.
- **Data-driven** — employee data lives in `employee.json`, not hardcoded in the test.
- **Centralized config & waits** — `ConfigReader` and `WaitUtils` avoid duplicated boilerplate
  and magic strings/timeouts scattered across page objects.
- **Meaningful assertions** — every `Assert` carries a descriptive failure message explaining
  *what* was expected and *why* it matters.
- **Thread-safe driver management** — `DriverFactory` uses a `ThreadLocal<WebDriver>`, so the
  suite is ready for parallel execution if the suite grows.

---

## 3. Dependencies Used

| Tool / Library | Purpose |
|---|---|
| Java 11 | Language |
| Maven | Build & dependency management |
| Selenium WebDriver 4.21 | Browser automation |
| WebDriverManager (Bonigarcia) | Auto-downloads/manages browser driver binaries |
| TestNG 7.10 | Test runner: annotations, `dependsOnMethods`, assertions, suite XML |
| ExtentReports 5.1 (Spark reporter) | Standalone HTML report (`reports/ExtentReport.html`) |
| Allure TestNG 2.27 | Rich HTML report with steps/severity/attachments |
| REST Assured 5.4 | API request/response layer for the API-validation step |
| Jackson Databind | Parses `employee.json` into a POJO for data-driven testing |
| Log4j2 | Logging |
| Monte Screen Recorder | Captures a video (`.avi`) of the desktop during the test run |

---

## 4. Setup Instructions

### Prerequisites
- JDK 11 or later (`java -version`)
- Maven 3.8+ (`mvn -version`)
- Google Chrome (or Firefox) installed locally
- Internet access to reach `opensource-demo.orangehrmlive.com` and `reqres.in`

### Install
```bash
git clone <your-repo-url>
cd qa-automation-framework
mvn clean install -DskipTests
```
`WebDriverManager` handles the ChromeDriver/GeckoDriver binary automatically — no manual driver
download is needed.

### Configuration
All environment values live in `src/test/resources/config.properties`:
```properties
base.url=https://opensource-demo.orangehrmlive.com/
browser=chrome          # chrome | firefox
headless=false          # set true for CI
login.username=Admin
login.password=admin123
api.base.url=https://reqres.in/api
```

---

## 5. How to Run the Tests

### Run the full suite (Maven + TestNG)
```bash
mvn clean test
```
This uses `testng.xml`, which registers `TestListener` (ExtentReports) and `AllureTestNg`
(Allure results) automatically.

### Run headless (e.g. CI)
```bash
mvn clean test -Dheadless=true
```
*(or set `headless=true` directly in `config.properties`)*

### View the ExtentReports HTML report
Open directly after the run — no extra command needed:
```
reports/ExtentReport.html
```

### Generate & view the Allure HTML report
```bash
mvn allure:report      # builds target/site/allure-maven-plugin from target/allure-results
mvn allure:serve       # builds AND opens the interactive report in your browser
```

### Video of the run
If running with a visible display (`headless=false`), a screen recording is saved to:
```
reports/videos/
```
On headless/CI environments without a display, video capture is automatically skipped (logged
to console) so the suite still completes — use a screen recorder if you need a video from a
headless pipeline instead.

### Screenshots on failure
Any failed test step is automatically captured to `test-output/screenshots/` and embedded in
both the Extent and Allure reports.

---

## 6. Notes & Assumptions

- The OrangeHRM demo site is a **shared public sandbox** — data can be reset/modified by other
  users at any time, and Employee Ids may occasionally collide. The test uses a distinctive
  Employee Id (`QA10245`) from `employee.json` to minimize collisions; change it if a run fails
  because the Id is already taken.
- Selenium was chosen (per the assessment's "Use Selenium with Java" instruction) over
  Playwright, with TestNG as the runner and Page Object Model for maintainability.
  ("oSelenium" in the brief is read as "Use Selenium".)
- The API validation step uses ReqRes (a public test API), per the assessment's explicit
  fallback option, since the demo OrangeHRM instance has no open API without OAuth setup.
