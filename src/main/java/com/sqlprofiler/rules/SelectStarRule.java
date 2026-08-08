package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;
import java.util.Optional;

public class SelectStarRule implements DetectionRule {

    @Override
    public Optional<Finding> analyze(String sql, String explainOutput) {

        String upper = sql.toUpperCase().trim();

        if (!upper.startsWith("SELECT")) {
            return Optional.empty();
        }

        // Check for SELECT * or SELECT table.*
        boolean hasStar = upper.contains("SELECT *") || 
                          upper.matches(".*SELECT\\s+\\w+\\.\\*.*");

        if (!hasStar) {
            return Optional.empty();
        }

        // Skip system queries
        if (upper.contains("PG_") || upper.contains("INFORMATION_SCHEMA")) {
            return Optional.empty();
        }

        String tableName = extractTableName(sql);

        return Optional.of(new Finding(
            "SELECT_STAR",
            "MEDIUM",
            "HIGH",
            "SELECT * fetches every column from '" + tableName + "', " +
                "including columns your application never uses. " +
                "This wastes memory and network bandwidth on every query execution. " +
                "On large tables with many columns (images, JSON blobs, long text fields), " +
                "this can multiply data transfer by 10x or more. " +
                "It also breaks your application silently if the table schema changes.",
            "SELECT * found — all columns fetched from " + tableName,
            "Replace SELECT * with only the columns you need. " +
                "Example: SELECT id, customer_id, amount, created_at FROM " + tableName
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