package com.orangehrm.utils;

import com.orangehrm.api.OrangeHrmApiClient;
import com.orangehrm.api.models.EmployeeListResponse;
import com.orangehrm.api.models.EmployeeSummaryDto;
import com.orangehrm.api.models.JobDetailsDto;
import com.orangehrm.api.models.PersonalDetailsDto;
import io.restassured.response.Response;
import org.openqa.selenium.WebDriver;

/**
 * Adapter and backward-compatible utility wrapping {@link OrangeHrmApiClient}.
 *
 * <p>Preserves all existing method signatures used across test suites while extending
 * the capabilities with the full {@link OrangeHrmApiClient} typed API client.</p>
 */
public class ApiHelper extends OrangeHrmApiClient {

    public ApiHelper() {
        super();
    }

    public ApiHelper(WebDriver driver) {
        super(driver);
    }

    public ApiHelper(String baseUrl, WebDriver driver) {
        super(baseUrl, driver);
    }

    /**
     * Backward-compatible alias for fetching employee personal details response.
     */
    public Response getEmployee(int empNumber) {
        return getPersonalDetailsResponse(empNumber);
    }

    public Response createEmployeeRecord(EmployeeData employee) {
        return createEmployeeResponse(employee);
    }

    public Response updateEmployeeRecord(String empNumberOrId, EmployeeData employee) {
        String payload = String.format(
                "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"middleName\":\"%s\",\"employeeId\":\"%s\"}",
                employee.getFirstName(), employee.getLastName(),
                employee.getMiddleName() != null ? employee.getMiddleName() : "",
                employee.getEmployeeId());

        return authenticated()
                .body(payload)
                .when()
                .put(PIM_EMPLOYEES_ENDPOINT + "/" + empNumberOrId + "/personal-details")
                .then()
                .extract()
                .response();
    }

    public Response deleteEmployeeRecord(String empNumberOrId) {
        int id;
        try {
            id = Integer.parseInt(empNumberOrId);
        } catch (Exception e) {
            id = 0;
        }
        return deleteEmployeeResponse(id);
    }
}
