package com.orangehrm.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDetailsResponse {

    private JobDetailsDto data;

    public JobDetailsDto getData() {
        return data;
    }

    public void setData(JobDetailsDto data) {
        this.data = data;
    }
}
