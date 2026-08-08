package com.sqlprofiler.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "query_history")
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "original_query",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String originalQuery;

    @Column(
        name = "normalized_query",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String normalizedQuery;

    @Column(
        name = "execution_time",
        nullable = false
    )
    private double executionTime;

    @Column(
        name = "rows_scanned",
        nullable = false
    )
    private long rowsScanned;

    @Column(
        name = "findings_json",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String findingsJson;

    @Column(
        name = "overall_status",
        nullable = false,
        length = 30
    )
    private String overallStatus;

    @Column(
        name = "analyzed_at",
        nullable = false
    )
    private LocalDateTime analyzedAt;

    public QueryHistory() {
    }

    public Long getId() {
        return id;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public void setNormalizedQuery(String normalizedQuery) {
        this.normalizedQuery = normalizedQuery;
    }

    public double getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }

    public long getRowsScanned() {
        return rowsScanned;
    }

    public void setRowsScanned(long rowsScanned) {
        this.rowsScanned = rowsScanned;
    }

    public String getFindingsJson() {
        return findingsJson;
    }

    public void setFindingsJson(String findingsJson) {
        this.findingsJson = findingsJson;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}