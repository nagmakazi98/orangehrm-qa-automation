package com.orangehrm.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDetailsDto {

    private int empNumber;
    private String joinedDate;
    private JobTitleDto jobTitle;
    private EmploymentStatusDto empStatus;

    public int getEmpNumber() {
        return empNumber;
    }

    public void setEmpNumber(int empNumber) {
        this.empNumber = empNumber;
    }

    public String getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(String joinedDate) {
        this.joinedDate = joinedDate;
    }

    public JobTitleDto getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(JobTitleDto jobTitle) {
        this.jobTitle = jobTitle;
    }

    public EmploymentStatusDto getEmpStatus() {
        return empStatus;
    }

    public void setEmpStatus(EmploymentStatusDto empStatus) {
        this.empStatus = empStatus;
    }

    public String getJobTitleName() {
        return jobTitle != null ? jobTitle.getTitle() : null;
    }

    public String getEmploymentStatusName() {
        return empStatus != null ? empStatus.getName() : null;
    }
}
