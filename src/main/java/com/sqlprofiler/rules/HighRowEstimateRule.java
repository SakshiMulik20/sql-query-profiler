package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HighRowEstimateRule implements DetectionRule {

    private static final long MIN_ROWS_TO_FLAG = 10000L;

    private static final Pattern ROWS_PATTERN = Pattern.compile(
        "\\brows=(\\d+)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public Optional<Finding> analyze(String sql, String explainOutput) {
        if (sql == null || sql.isBlank()) {
            return Optional.empty();
        }

        if (explainOutput == null || explainOutput.isBlank()) {
            return Optional.empty();
        }

        String upperSql = sql.trim().toUpperCase();

        if (!upperSql.startsWith("SELECT")) {
            return Optional.empty();
        }

        if (upperSql.contains("PG_")
                || upperSql.contains("INFORMATION_SCHEMA")) {
            return Optional.empty();
        }

        Matcher matcher = ROWS_PATTERN.matcher(explainOutput);

        long highestRowEstimate = 0L;

        while (matcher.find()) {
            try {
                long currentEstimate = Long.parseLong(matcher.group(1));

                if (currentEstimate > highestRowEstimate) {
                    highestRowEstimate = currentEstimate;
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed plan values and continue checking the plan.
            }
        }

        if (highestRowEstimate < MIN_ROWS_TO_FLAG) {
            return Optional.empty();
        }

        String tableName = extractTableName(sql);

        return Optional.of(new Finding(
            "HIGH_ROW_ESTIMATE",
            "HIGH",
            "MEDIUM",
            "The execution plan estimates that this query will process " +
                highestRowEstimate + " rows for '" + tableName + "'. " +
                "Processing a large number of rows can increase CPU usage, " +
                "memory consumption, sorting cost, and response time.",
            "Highest rows= estimate found in EXPLAIN ANALYZE: " +
                highestRowEstimate,
            "Reduce the number of rows earlier in the plan by adding a selective " +
                "WHERE condition, adding a useful index, improving the join " +
                "condition, or paginating the result."
        ));
    }

    private String extractTableName(String sql) {
        String[] words = sql.trim().split("\\s+");

        for (int i = 0; i < words.length - 1; i++) {
            if ("FROM".equalsIgnoreCase(words[i])) {
                return words[i + 1].replaceAll("[^a-zA-Z0-9_]", "");
            }
        }

        return "the_table";
    }
}