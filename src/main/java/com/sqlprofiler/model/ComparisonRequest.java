package com.sqlprofiler.model;

public class ComparisonRequest {

    private Long beforeHistoryId;
    private String afterQuery;

    public ComparisonRequest() {
    }

    public Long getBeforeHistoryId() {
        return beforeHistoryId;
    }

    public void setBeforeHistoryId(Long beforeHistoryId) {
        this.beforeHistoryId = beforeHistoryId;
    }

    public String getAfterQuery() {
        return afterQuery;
    }

    public void setAfterQuery(String afterQuery) {
        this.afterQuery = afterQuery;
    }
}