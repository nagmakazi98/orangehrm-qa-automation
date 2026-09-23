# OrangeHRM Employee Lifecycle — QA Automation Framework

A **Selenium + Java + TestNG** automation framework that validates an end-to-end employee lifecycle on the [OrangeHRM Demo](https://opensource-demo.orangehrmlive.com/).

## Scenario Covered

The framework automates:

1. Login and dashboard validation
2. Add new employee with profile picture
3. Search and verify employee
4. Edit Job Title and Employment Status
5. API validation using ReqRes
6. Delete employee and verify removal
7. Logout and session validation

## Tech Stack

* Java 11+
* Selenium WebDriver 4.21
* TestNG 7.10
* Maven
* WebDriverManager
* REST Assured
* Jackson
* ExtentReports
* Allure
* Log4j2
* Monte Screen Recorder

## Framework Design

* **Page Object Model (POM)**
* Data-driven testing using JSON
* Centralized configuration and explicit waits
* Thread-safe WebDriver using `ThreadLocal`
* TestNG dependencies for end-to-end execution
* Screenshots on failure
* HTML reporting
* Test execution video recording

## Project Structure

```text
src/
├── main/java/com/orangehrm/
│   ├── base/
│   ├── pages/
│   ├── utils/
│   └── listeners/
│
└── test/
    ├── java/com/orangehrm/tests/
    └── resources/
        ├── config.properties
        ├── testdata/
        └── images/

reports/
├── ExtentReport.html
└── videos/

test-output/
└── screenshots/
```

## Prerequisites

* JDK 11+
* Maven 3.8+
* Google Chrome
* Internet connection

WebDriverManager automatically manages the browser driver.

## Setup

```bash
git clone <your-repo-url>
cd qa-automation-framework
mvn clean install -DskipTests
```

Configuration is available in:

```text
src/test/resources/config.properties
```

## Run Tests

### Full Test Suite

```bash
mvn clean test
```

### Headless Execution

```bash
mvn clean test -Dheadless=true
```

## Reports & Video

After execution:

```text
reports/ExtentReport.html
reports/videos/
test-output/screenshots/
```

Allure report:

```bash
mvn allure:serve
```

## API Validation

ReqRes is used as the public test API for the create/update/delete validation layer, as permitted by the assessment. The API layer is isolated in `ApiHelper.java` and can be replaced with a real API endpoint later.

## Notes

OrangeHRM is a shared public demo environment, so test data may occasionally be affected by other users. The framework generates/uses employee IDs to minimize conflicts.

**Assessment:** End-to-end UI automation + API validation + reporting + video evidence.
