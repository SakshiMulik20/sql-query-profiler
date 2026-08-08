package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotInSubqueryRule implements DetectionRule {

    private static final Pattern NOT_IN_SUBQUERY_PATTERN = Pattern.compile(
        "\\b([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)" +
            "\\s+NOT\\s+IN\\s*\\(\\s*SELECT\\b",
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

        String whereClause = extractWhereClause(trimmedSql);
        Matcher matcher = NOT_IN_SUBQUERY_PATTERN.matcher(whereClause);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String columnName = matcher.group(1);

        return Optional.of(new Finding(
            "NOT_IN_SUBQUERY",
            "HIGH",
            "HIGH",
            "The query uses NOT IN with a subquery on '" + columnName +
                "'. If the subquery returns even one NULL value, SQL's three-valued " +
                "logic can cause rows to be excluded unexpectedly. This pattern can " +
                "also produce less predictable query plans than an anti-join.",
            "NOT IN subquery detected for column: " + columnName,
            "Prefer NOT EXISTS with an explicit equality condition and handle NULL " +
                "values deliberately. Example: WHERE NOT EXISTS (" +
                "SELECT 1 FROM related_table r WHERE r.id = outer_table.id " +
                "AND r.id IS NOT NULL)"
        ));
    }

    private String extractWhereClause(String sql) {
        String upperSql = sql.toUpperCase(Locale.ROOT);
        int whereIndex = upperSql.indexOf("WHERE");

        if (whereIndex < 0) {
            return "";
        }

        String clause = sql.substring(whereIndex + "WHERE".length());
        String upperClause = clause.toUpperCase(Locale.ROOT);

        String[] endingKeywords = {
            " GROUP BY",
            " ORDER BY",
            " LIMIT",
            " OFFSET",
            " FETCH",
            " FOR "
        };

        int endIndex = clause.length();

        for (String keyword : endingKeywords) {
            int keywordIndex = upperClause.indexOf(keyword);

            if (keywordIndex >= 0 && keywordIndex < endIndex) {
                endIndex = keywordIndex;
            }
        }

        return clause.substring(0, endIndex);
    }
}