package com.orangehrm.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobTitleDto {

    private Integer id;
    @com.fasterxml.jackson.annotation.JsonAlias({"title", "name", "jobTitleName", "jobTitle"})
    private String title;
    @com.fasterxml.jackson.annotation.JsonAlias({"name", "title"})
    private String name;
    private Boolean isDeleted;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        if (title != null && !title.isBlank()) return title;
        return name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }
}
