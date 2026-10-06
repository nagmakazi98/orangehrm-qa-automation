package com.orangehrm.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Top-level response wrapper for GET /web/index.php/api/v2/pim/employees.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeListResponse {

    private List<EmployeeSummaryDto> data = new ArrayList<>();
    private Map<String, Object> meta;

    public List<EmployeeSummaryDto> getData() {
        return data;
    }

    public void setData(List<EmployeeSummaryDto> data) {
        this.data = data;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }
}
