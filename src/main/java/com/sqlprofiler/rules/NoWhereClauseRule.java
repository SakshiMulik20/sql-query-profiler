package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;
import java.util.Optional;

public class NoWhereClauseRule implements DetectionRule {

    @Override
    public Optional<Finding> analyze(String sql, String explainOutput) {

        String upper = sql.toUpperCase().trim();

        // Only applies to SELECT queries
        if (!upper.startsWith("SELECT")) {
            return Optional.empty();
        }

        // Skip if it has a WHERE clause
        if (upper.contains("WHERE")) {
            return Optional.empty();
        }

        // Skip system/metadata queries — they're usually small and safe
        if (upper.contains("PG_") || upper.contains("INFORMATION_SCHEMA")) {
            return Optional.empty();
        }

        // Extract table name for a specific message
        String tableName = extractTableName(sql);

        return Optional.of(new Finding(
            "NO_WHERE_CLAUSE",
            "HIGH",
            "HIGH",
            "Query fetches every single row from '" + tableName + "' with no filter. " +
                "If this table has 1 million rows, your app receives 1 million rows — " +
                "consuming memory, network bandwidth, and database CPU all at once. " +
                "This is one of the most common causes of application slowdowns during peak traffic.",
            "No WHERE clause found in: " + sql.trim(),
            "Add a WHERE clause to filter rows. Example: WHERE created_at > now() - interval '7 days'"
        ));
    }

    private String extractTableName(String sql) {
        String upper = sql.toUpperCase();
        String[] words = sql.trim().split("\\s+");
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].toUpperCase().equals("FROM")) {
                return words[i + 1].replaceAll("[^a-zA-Z0-9_]", "");
            }
        }
        return "the table";
    }
}