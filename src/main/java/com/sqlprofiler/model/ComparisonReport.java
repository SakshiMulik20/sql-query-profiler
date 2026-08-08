package com.sqlprofiler.model;

public class ComparisonReport {

    private QueryHistory before;
    private QueryReport after;

    private double beforeExecutionTime;
    private double afterExecutionTime;
    private double executionTimeImprovementMs;
    private double improvementPercentage;

    private String performanceStatus;
    private boolean statusImproved;

    public ComparisonReport() {
    }

    public ComparisonReport(
        QueryHistory before,
        QueryReport after,
        double beforeExecutionTime,
        double afterExecutionTime,
        double executionTimeImprovementMs,
        double improvementPercentage,
        String performanceStatus,
        boolean statusImproved
    ) {
        this.before = before;
        this.after = after;
        this.beforeExecutionTime = beforeExecutionTime;
        this.afterExecutionTime = afterExecutionTime;
        this.executionTimeImprovementMs = executionTimeImprovementMs;
        this.improvementPercentage = improvementPercentage;
        this.performanceStatus = performanceStatus;
        this.statusImproved = statusImproved;
    }

    public QueryHistory getBefore() {
        return before;
    }

    public void setBefore(QueryHistory before) {
        this.before = before;
    }

    public QueryReport getAfter() {
        return after;
    }

    public void setAfter(QueryReport after) {
        this.after = after;
    }

    public double getBeforeExecutionTime() {
        return beforeExecutionTime;
    }

    public void setBeforeExecutionTime(double beforeExecutionTime) {
        this.beforeExecutionTime = beforeExecutionTime;
    }

    public double getAfterExecutionTime() {
        return afterExecutionTime;
    }

    public void setAfterExecutionTime(double afterExecutionTime) {
        this.afterExecutionTime = afterExecutionTime;
    }

    public double getExecutionTimeImprovementMs() {
        return executionTimeImprovementMs;
    }

    public void setExecutionTimeImprovementMs(
        double executionTimeImprovementMs
    ) {
        this.executionTimeImprovementMs = executionTimeImprovementMs;
    }

    public double getImprovementPercentage() {
        return improvementPercentage;
    }

    public void setImprovementPercentage(double improvementPercentage) {
        this.improvementPercentage = improvementPercentage;
    }

    public String getPerformanceStatus() {
        return performanceStatus;
    }

    public void setPerformanceStatus(String performanceStatus) {
        this.performanceStatus = performanceStatus;
    }

    public boolean isStatusImproved() {
        return statusImproved;
    }

    public void setStatusImproved(boolean statusImproved) {
        this.statusImproved = statusImproved;
    }
}