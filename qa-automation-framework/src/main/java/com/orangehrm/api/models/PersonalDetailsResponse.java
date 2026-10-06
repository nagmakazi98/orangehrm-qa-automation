package com.orangehrm.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Top-level response wrapper for GET /web/index.php/api/v2/pim/employees/{empNumber}/personal-details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalDetailsResponse {

    private PersonalDetailsDto data;

    public PersonalDetailsDto getData() {
        return data;
    }

    public void setData(PersonalDetailsDto data) {
        this.data = data;
    }
}
