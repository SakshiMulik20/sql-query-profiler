package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;
import java.util.Optional;

public class NoLimitRule implements DetectionRule {

    @Override
    public Optional<Finding> analyze(String sql, String explainOutput) {

        String upper = sql.toUpperCase().trim();

        if (!upper.startsWith("SELECT")) {
            return Optional.empty();
        }

        // Skip if LIMIT or FETCH is already present
        if (upper.contains("LIMIT") || upper.contains("FETCH")) {
            return Optional.empty();
        }

        // Skip if it's an aggregate query — COUNT, SUM, AVG don't need LIMIT
        if (upper.contains("COUNT(") || upper.contains("SUM(") || 
            upper.contains("AVG(") || upper.contains("MAX(") || 
            upper.contains("MIN(")) {
            return Optional.empty();
        }

        // Skip system queries
        if (upper.contains("PG_") || upper.contains("INFORMATION_SCHEMA")) {
            return Optional.empty();
        }

        // Only flag if a large row count is visible in EXPLAIN
        if (!explainOutput.contains("rows=")) {
            return Optional.empty();
        }

        String tableName = extractTableName(sql);

        return Optional.of(new Finding(
            "NO_LIMIT",
            "MEDIUM",
            "MEDIUM",
            "Query has no LIMIT clause. If '" + tableName + "' grows to 1 million rows, " +
                "this query returns all 1 million to your application on every call. " +
                "APIs and UI pages rarely need more than a few hundred rows at a time. " +
                "Without LIMIT, one slow user request can consume all available database memory.",
            "No LIMIT clause found in query against " + tableName,
            "Add LIMIT to cap results. Example: SELECT ... FROM " + tableName + 
                " WHERE ... LIMIT 100;\n" +
                "For pagination, use: LIMIT 20 OFFSET 0"
        ));
    }

    private String extractTableName(String sql) {
        String[] words = sql.trim().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].toUpperCase().equals("FROM")) {
                return words[i + 1].replaceAll("[^a-zA-Z0-9_]", "");
            }
        }
        return "the table";
    }
}