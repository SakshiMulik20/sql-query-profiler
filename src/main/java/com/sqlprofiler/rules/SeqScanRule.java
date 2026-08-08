package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeqScanRule implements DetectionRule {

    private static final int MIN_ROWS_REMOVED_TO_FLAG = 500;

    @Override
    public Optional<Finding> analyze(String sql, String explainOutput) {

        // Must have a Seq Scan to proceed
        if (!explainOutput.contains("Seq Scan")) {
            return Optional.empty();
        }

        // Extract table name
        String tableName = "table";
        Pattern tablePattern = Pattern.compile("Seq Scan on (\\w+)");
        Matcher tableMatcher = tablePattern.matcher(explainOutput);
        if (tableMatcher.find()) {
            tableName = tableMatcher.group(1);
        }

        // "Rows Removed by Filter" tells us how many rows were scanned and thrown away
        // This is the real cost of a missing index
        if (explainOutput.contains("Rows Removed by Filter:")) {
            Pattern removedPattern = Pattern.compile("Rows Removed by Filter: (\\d+)");
            Matcher removedMatcher = removedPattern.matcher(explainOutput);

            if (removedMatcher.find()) {
                int rowsRemoved = Integer.parseInt(removedMatcher.group(1));

                // False-positive guard — small tables are fine
                if (rowsRemoved < MIN_ROWS_REMOVED_TO_FLAG) {
                    return Optional.empty();
                }

                String columnHint = extractWhereColumn(sql);

                return Optional.of(new Finding(
                    "SEQ_SCAN",
                    "CRITICAL",
                    "HIGH",
                    "Table '" + tableName + "' was scanned row by row. " +
                        "PostgreSQL read " + rowsRemoved + " rows and threw them away " +
                        "just to find the matching ones. " +
                        "This is because there is no index on the column in your WHERE clause. " +
                        "Every time this query runs, it scans the entire table — " +
                        "and it gets slower as the table grows.",
                    "Seq Scan on " + tableName +
                        " — Rows Removed by Filter: " + rowsRemoved,
                    "CREATE INDEX idx_" + tableName + "_" + columnHint +
                        " ON " + tableName + "(" + columnHint + ");"
                ));
            }
        }

        // Seq Scan with no filter — fetching entire table (no WHERE clause)
        // This is handled by NoWhereClauseRule, but still flag lightly here
        Pattern tableRowsPattern = Pattern.compile("Seq Scan on (\\w+).*?cost=\\d+\\.\\d+\\.\\.\\d+\\.\\d+ rows=(\\d+)");
        Matcher tableRowsMatcher = tableRowsPattern.matcher(explainOutput);
        if (tableRowsMatcher.find()) {
            int estimatedTableRows = Integer.parseInt(tableRowsMatcher.group(2));
            if (estimatedTableRows > MIN_ROWS_REMOVED_TO_FLAG) {
                return Optional.of(new Finding(
                    "SEQ_SCAN",
                    "HIGH",
                    "MEDIUM",
                    "Table '" + tableName + "' is being fully scanned (" +
                        estimatedTableRows + " estimated rows). " +
                        "Consider adding a WHERE clause or an index.",
                    "Seq Scan on " + tableName + " (rows=" + estimatedTableRows + ")",
                    "Add a WHERE clause to filter rows, or create an index on the relevant column."
                ));
            }
        }

        return Optional.empty();
    }

    private String extractWhereColumn(String sql) {
        String upper = sql.toUpperCase();

        if (upper.contains("WHERE")) {
            String afterWhere = sql.substring(upper.indexOf("WHERE") + 5).trim();

            String firstToken = afterWhere.split("[\\s=<>!:()]+")[0];

            String cleanedToken = firstToken
                .toLowerCase()
                .replaceAll("[^a-zA-Z0-9_.]", "");

            int dotIndex = cleanedToken.lastIndexOf('.');

            if (dotIndex >= 0) {
                cleanedToken = cleanedToken.substring(dotIndex + 1);
            }

            return cleanedToken.replaceAll("[^a-zA-Z0-9_]", "");
        }

        return "column";
    }
}