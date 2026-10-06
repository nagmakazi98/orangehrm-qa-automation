package com.orangehrm.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for an employee item in the /web/index.php/api/v2/pim/employees list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeSummaryDto {

    private int empNumber;
    private String lastName;
    private String firstName;
    private String middleName;
    private String employeeId;
    private Object terminationId;

    public int getEmpNumber() {
        return empNumber;
    }

    public void setEmpNumber(int empNumber) {
        this.empNumber = empNumber;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public Object getTerminationId() {
        return terminationId;
    }

    public void setTerminationId(Object terminationId) {
        this.terminationId = terminationId;
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    @Override
    public String toString() {
        return "EmployeeSummaryDto{" +
                "empNumber=" + empNumber +
                ", employeeId='" + employeeId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
