package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnparameterizedQueryRule implements DetectionRule {

    private static final Pattern LITERAL_COMPARISON_PATTERN = Pattern.compile(
        "\\b([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?)" +
            "(?:\\s*::\\s*[A-Za-z_][A-Za-z0-9_]*)?" +
            "\\s*(=|<>|!=|<=|>=|<|>|LIKE|ILIKE)\\s*" +
            "((?:'[^']*(?:''[^']*)*')|(?:\\b\\d+(?:\\.\\d+)?\\b))",
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
        Matcher matcher = LITERAL_COMPARISON_PATTERN.matcher(whereClause);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String columnName = matcher.group(1);
        String operator = matcher.group(2);
        String literal = matcher.group(3);

        return Optional.of(new Finding(
            "UNPARAMETERIZED_QUERY",
            "MEDIUM",
            "HIGH",
            "The WHERE clause compares '" + columnName +
                "' directly with the literal value " + literal +
                ". Building SQL with changing values directly inside the query " +
                "can reduce plan reuse and may expose the application to SQL injection " +
                "if the value comes from user input.",
            "Literal comparison detected: " + columnName + " " +
                operator + " " + literal,
            "Use a prepared statement or named parameter instead, for example: " +
                "WHERE " + columnName + " " + operator + " $1"
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