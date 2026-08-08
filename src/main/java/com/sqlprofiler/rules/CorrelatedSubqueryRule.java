package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorrelatedSubqueryRule implements DetectionRule {

    private static final Pattern OUTER_ALIAS_PATTERN = Pattern.compile(
        "\\b(?:FROM|JOIN)\\s+" +
            "[A-Za-z_][A-Za-z0-9_]*" +
            "\\s+(?:AS\\s+)?([A-Za-z_][A-Za-z0-9_]*)",
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

        int subqueryIndex = findNestedSelect(upperSql);

        if (subqueryIndex < 0) {
            return Optional.empty();
        }

        String outerQuery = trimmedSql.substring(0, subqueryIndex);
        String subqueryBody = trimmedSql.substring(subqueryIndex);

        List<String> outerAliases = extractOuterAliases(outerQuery);

        for (String alias : outerAliases) {
            Pattern outerReferencePattern = Pattern.compile(
                "\\b" + Pattern.quote(alias) +
                    "\\.[A-Za-z_][A-Za-z0-9_]*\\b",
                Pattern.CASE_INSENSITIVE
            );

            Matcher referenceMatcher =
                outerReferencePattern.matcher(subqueryBody);

            if (referenceMatcher.find()) {
                String reference = referenceMatcher.group();

                return Optional.of(new Finding(
                    "CORRELATED_SUBQUERY",
                    "HIGH",
                    "HIGH",
                    "The subquery references a column from the outer query. " +
                        "PostgreSQL may need to evaluate the subquery repeatedly " +
                        "for rows produced by the outer query, creating an " +
                        "N+1-style performance problem.",
                    "Outer query reference found inside subquery: " + reference,
                    "Rewrite the correlated subquery as a JOIN, a pre-aggregated " +
                        "derived table, or a window function where appropriate. " +
                        "Then compare both versions with EXPLAIN ANALYZE."
                ));
            }
        }

        return Optional.empty();
    }

    private int findNestedSelect(String upperSql) {
        int firstSelect = upperSql.indexOf("SELECT");
        int secondSelect = upperSql.indexOf("SELECT", firstSelect + 6);

        return secondSelect;
    }

    private List<String> extractOuterAliases(String outerQuery) {
        List<String> aliases = new ArrayList<>();
        Matcher matcher = OUTER_ALIAS_PATTERN.matcher(outerQuery);

        while (matcher.find()) {
            String alias = matcher.group(1);
            String upperAlias = alias.toUpperCase(Locale.ROOT);

            if (!isSqlKeyword(upperAlias)) {
                aliases.add(alias);
            }
        }

        return aliases;
    }

    private boolean isSqlKeyword(String value) {
        return value.equals("WHERE")
            || value.equals("JOIN")
            || value.equals("ON")
            || value.equals("GROUP")
            || value.equals("ORDER")
            || value.equals("LIMIT")
            || value.equals("OFFSET")
            || value.equals("AND")
            || value.equals("OR")
            || value.equals("IN");
    }
}