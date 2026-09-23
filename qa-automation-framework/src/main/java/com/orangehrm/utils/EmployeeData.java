package com.orangehrm.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Plain data object mapped from src/test/resources/testdata/employee.json.
 * Represents the data-driven input for the Employee Lifecycle scenario.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeData {

    private String firstName;
    private String lastName;
    private String employeeId;
    private String profilePicture;
    private String updatedJobTitle;
    private String updatedEmploymentStatus;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getUpdatedJobTitle() {
        return updatedJobTitle;
    }

    public void setUpdatedJobTitle(String updatedJobTitle) {
        this.updatedJobTitle = updatedJobTitle;
    }

    public String getUpdatedEmploymentStatus() {
        return updatedEmploymentStatus;
    }

    public void setUpdatedEmploymentStatus(String updatedEmploymentStatus) {
        this.updatedEmploymentStatus = updatedEmploymentStatus;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Override
    public String toString() {
        return "EmployeeData{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", updatedJobTitle='" + updatedJobTitle + '\'' +
                ", updatedEmploymentStatus='" + updatedEmploymentStatus + '\'' +
                '}';
    }
}
