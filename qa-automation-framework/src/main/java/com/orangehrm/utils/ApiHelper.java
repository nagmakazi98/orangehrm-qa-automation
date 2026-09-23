package com.orangehrm.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/**
 * Thin REST client used to validate employee data at the API layer and
 * cross-check it against what is rendered in the UI.
 *
 * NOTE ON SCOPE:
 * The public OrangeHRM demo instance (opensource-demo.orangehrmlive.com) does
 * not expose an open/public REST API for employee CRUD without additional
 * OAuth setup that is out of scope for a demo/sandbox account. As permitted
 * by the assessment ("or simulate API with any public test API like ReqRes"),
 * this helper talks to https://reqres.in to demonstrate the API-validation
 * layer of the framework end-to-end (create/read/update/delete + response
 * assertions). The methods are written so that swapping api.base.url in
 * config.properties to a real OrangeHRM API host (e.g. an on-prem instance
 * with API access enabled) requires no test-code changes -- only the
 * request/response mapping would need to be adjusted to OrangeHRM's schema.
 */
public class ApiHelper {

    private final String baseUrl;

    public ApiHelper() {
        this.baseUrl = ConfigReader.get("api.base.url");
        RestAssured.baseURI = baseUrl;
    }

    /**
     * Simulates creating/registering the employee record at the API layer
     * so it can later be fetched and cross-checked against the UI record.
     */
    public Response createEmployeeRecord(EmployeeData employee) {
        String payload = "{"
                + "\"name\": \"" + employee.getFullName() + "\","
                + "\"job\": \"" + employee.getUpdatedJobTitle() + "\""
                + "}";

        return given()
                .contentType("application/json")
                .body(payload)
                .when()
                .post("/users")
                .then()
                .extract()
                .response();
    }

    /**
     * Fetches a record by id to validate it exists / matches expected values.
     */
    public Response getEmployeeRecord(String id) {
        return given()
                .when()
                .get("/users/" + id)
                .then()
                .extract()
                .response();
    }

    /**
     * Simulates updating job title / employment status at the API layer,
     * mirroring the UI "Edit Employee" step, so the two can be compared.
     */
    public Response updateEmployeeRecord(String id, EmployeeData employee) {
        String payload = "{"
                + "\"name\": \"" + employee.getFullName() + "\","
                + "\"job\": \"" + employee.getUpdatedJobTitle() + "\""
                + "}";

        return given()
                .contentType("application/json")
                .body(payload)
                .when()
                .put("/users/" + id)
                .then()
                .extract()
                .response();
    }

    /**
     * Simulates deleting the employee record at the API layer, mirroring the
     * UI delete step so deletion can be verified from both sides.
     */
    public Response deleteEmployeeRecord(String id) {
        return given()
                .when()
                .delete("/users/" + id)
                .then()
                .extract()
                .response();
    }
}
