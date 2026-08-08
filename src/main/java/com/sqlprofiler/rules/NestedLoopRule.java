package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NestedLoopRule implements DetectionRule {

    private static final Pattern NESTED_LOOP_ROWS_PATTERN = Pattern.compile(
        "Nested\\s+Loop.*?rows=(\\d+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
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

        if (!explainOutput.toUpperCase().contains("NESTED LOOP")) {
            return Optional.empty();
        }

        Matcher rowsMatcher = NESTED_LOOP_ROWS_PATTERN.matcher(explainOutput);

        String rowEstimate = "unknown";

        if (rowsMatcher.find()) {
            rowEstimate = rowsMatcher.group(1);
        }

        return Optional.of(new Finding(
            "NESTED_LOOP",
            "HIGH",
            "HIGH",
            "PostgreSQL chose a Nested Loop join strategy. " +
                "Nested loops can become very expensive when the outer side " +
                "returns many rows because PostgreSQL repeatedly searches the " +
                "inner table for each outer row.",
            "Nested Loop found in the EXPLAIN ANALYZE plan. " +
                "Estimated rows at the nested loop: " + rowEstimate,
            "Check that both join columns have useful indexes. " +
                "For large joins, compare the plan with a Hash Join or Merge Join " +
                "and verify that the join condition is selective."
        ));
    }
}