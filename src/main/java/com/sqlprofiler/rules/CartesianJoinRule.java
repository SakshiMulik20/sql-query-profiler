package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CartesianJoinRule implements DetectionRule {

    private static final Pattern COMMA_JOIN_PATTERN = Pattern.compile(
        "\\bFROM\\s+" +
        "([A-Za-z_][A-Za-z0-9_]*(?:\\s+(?:AS\\s+)?[A-Za-z_][A-Za-z0-9_]*)?)" +
        "\\s*,\\s*" +
        "([A-Za-z_][A-Za-z0-9_]*(?:\\s+(?:AS\\s+)?[A-Za-z_][A-Za-z0-9_]*)?)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TABLE_TO_TABLE_CONDITION_PATTERN =
        Pattern.compile(
            "\\b[A-Za-z_][A-Za-z0-9_]*\\.[A-Za-z_][A-Za-z0-9_]*" +
            "\\s*=\\s*" +
            "[A-Za-z_][A-Za-z0-9_]*\\.[A-Za-z_][A-Za-z0-9_]*\\b",
            Pattern.CASE_INSENSITIVE
        );

    @Override
    public Optional<Finding> analyze(String sql, String explainOutput) {
        if (sql == null || sql.isBlank()) {
            return Optional.empty();
        }

        String trimmedSql = sql.trim();
        String upperSql = trimmedSql.toUpperCase(Locale.ROOT);

        if (!upperSql.startsWith("SELECT")) {
            return Optional.empty();
        }

        if (upperSql.contains("PG_")
                || upperSql.contains("INFORMATION_SCHEMA")) {
            return Optional.empty();
        }

        if (upperSql.matches("(?s).*\\bCROSS\\s+JOIN\\b.*")) {
            return Optional.of(createFinding(
                "CROSS JOIN detected in the query",
                "The query explicitly creates a Cartesian product."
            ));
        }

        Matcher commaJoinMatcher = COMMA_JOIN_PATTERN.matcher(trimmedSql);

        if (!commaJoinMatcher.find()) {
            return Optional.empty();
        }

        boolean hasTableToTableCondition =
            TABLE_TO_TABLE_CONDITION_PATTERN
                .matcher(trimmedSql)
                .find();

        if (hasTableToTableCondition) {
            return Optional.empty();
        }

        String firstTable = extractFirstWord(commaJoinMatcher.group(1));
        String secondTable = extractFirstWord(commaJoinMatcher.group(2));

        return Optional.of(new Finding(
            "CARTESIAN_JOIN",
            "HIGH",
            "HIGH",
            "The query reads from '" + firstTable + "' and '" +
                secondTable + "' without a table-to-table join condition. " +
                "This can combine every row from one table with every row " +
                "from the other table, causing a huge increase in rows and CPU usage.",
            "Multiple table sources detected without a join condition: " +
                firstTable + ", " + secondTable,
            "Add an explicit JOIN condition, for example: " +
                "FROM " + firstTable + " a JOIN " + secondTable +
                " b ON a.id = b.related_id"
        ));
    }

    private Finding createFinding(String evidence, String explanation) {
        return new Finding(
            "CARTESIAN_JOIN",
            "HIGH",
            "MEDIUM",
            explanation + " A Cartesian product can produce " +
                "far more rows than either source table.",
            evidence,
            "Replace the Cartesian join with an explicit JOIN and provide " +
                "the correct ON condition between the tables."
        );
    }

    private String extractFirstWord(String value) {
        return value.trim().split("\\s+")[0];
    }
}