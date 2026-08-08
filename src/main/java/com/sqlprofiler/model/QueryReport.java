package com.sqlprofiler.model;

import java.util.ArrayList;
import java.util.List;

public class QueryReport {

    private String originalQuery;
    private double executionTime;
    private long rowsScanned;
    private String overallStatus;    // CRITICAL, ISSUES_FOUND, OPTIMIZED
    private List<Finding> findings;  // ALL problems found

    public QueryReport() {
        this.findings = new ArrayList<>();
        this.overallStatus = "OPTIMIZED"; // assume good until a rule fires
    }

    // Add a finding and update overall status
    public void addFinding(Finding finding) {
        this.findings.add(finding);

        // Escalate overall status based on worst finding
        if ("CRITICAL".equals(finding.getSeverity())) {
            this.overallStatus = "CRITICAL";
        } else if (this.overallStatus.equals("OPTIMIZED")) {
            this.overallStatus = "ISSUES_FOUND";
        }
    }

    public boolean hasFindings() {
        return !findings.isEmpty();
    }

    // Getters and Setters
    public String getOriginalQuery()           { return originalQuery; }
    public void setOriginalQuery(String q)     { this.originalQuery = q; }

    public double getExecutionTime()           { return executionTime; }
    public void setExecutionTime(double t)     { this.executionTime = t; }

    public long getRowsScanned()               { return rowsScanned; }
    public void setRowsScanned(long r)         { this.rowsScanned = r; }

    public String getOverallStatus()           { return overallStatus; }
    public void setOverallStatus(String s)     { this.overallStatus = s; }

    public List<Finding> getFindings()         { return findings; }
}