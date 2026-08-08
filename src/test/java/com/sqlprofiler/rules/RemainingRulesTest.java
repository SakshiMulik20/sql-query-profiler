package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemainingRulesTest {

    @Test
    void leadingWildcardRuleDetectsLeadingPercent() {
        LeadingWildcardRule rule = new LeadingWildcardRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_name LIKE '%john'",
            "Seq Scan on demo_orders"
        );

        assertTrue(result.isPresent());
        assertEquals("LEADING_WILDCARD", result.get().getRuleName());
        assertEquals("MEDIUM", result.get().getSeverity());
    }

    @Test
    void leadingWildcardRuleIgnoresPrefixSearch() {
        LeadingWildcardRule rule = new LeadingWildcardRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_name LIKE 'john%'",
            "Index Scan using idx_demo_orders_customer_name"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void orderByNoIndexRuleDetectsSortStep() {
        OrderByNoIndexRule rule = new OrderByNoIndexRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id, amount FROM demo_orders " +
            "ORDER BY amount DESC LIMIT 20",
            "Sort " +
            "(cost=100.00..5000.00 rows=50000 width=32)"
        );

        assertTrue(result.isPresent());
        assertEquals("ORDER_BY_NO_INDEX", result.get().getRuleName());
        assertEquals("MEDIUM", result.get().getSeverity());
    }

    @Test
    void orderByNoIndexRuleIgnoresQueryWithoutOrderBy() {
        OrderByNoIndexRule rule = new OrderByNoIndexRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id, amount FROM demo_orders LIMIT 20",
            "Seq Scan on demo_orders (rows=50000)"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void correlatedSubqueryRuleDetectsOuterReference() {
        CorrelatedSubqueryRule rule = new CorrelatedSubqueryRule();

        Optional<Finding> result = rule.analyze(
            "SELECT o.id FROM demo_orders o " +
            "WHERE o.created_at IN (" +
            "SELECT MAX(x.created_at) FROM demo_orders x " +
            "WHERE x.customer_id = o.customer_id)",
            "Nested Loop (rows=50000)"
        );

        assertTrue(result.isPresent());
        assertEquals("CORRELATED_SUBQUERY", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
        assertTrue(result.get().getEvidence().contains("o.customer_id"));
    }

    @Test
    void correlatedSubqueryRuleIgnoresIndependentSubquery() {
        CorrelatedSubqueryRule rule = new CorrelatedSubqueryRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_id IN (" +
            "SELECT customer_id FROM preferred_customers)",
            "Hash Semi Join (rows=100)"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void notInSubqueryRuleDetectsNotIn() {
        NotInSubqueryRule rule = new NotInSubqueryRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_id NOT IN (" +
            "SELECT customer_id FROM blocked_customers)",
            "Hash Anti Join (rows=100)"
        );

        assertTrue(result.isPresent());
        assertEquals("NOT_IN_SUBQUERY", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
    }

    @Test
    void notInSubqueryRuleIgnoresNotExists() {
        NotInSubqueryRule rule = new NotInSubqueryRule();

        Optional<Finding> result = rule.analyze(
            "SELECT o.id FROM demo_orders o " +
            "WHERE NOT EXISTS (" +
            "SELECT 1 FROM blocked_customers b " +
            "WHERE b.customer_id = o.customer_id)",
            "Hash Anti Join (rows=100)"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void unparameterizedQueryRuleDetectsLiteralValue() {
        UnparameterizedQueryRule rule = new UnparameterizedQueryRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_id = 42",
            "Index Scan using idx_demo_orders_customer_id"
        );

        assertTrue(result.isPresent());
        assertEquals("UNPARAMETERIZED_QUERY", result.get().getRuleName());
        assertEquals("MEDIUM", result.get().getSeverity());
        assertTrue(result.get().getEvidence().contains("42"));
    }

    @Test
    void unparameterizedQueryRuleIgnoresQueryWithoutWhereClause() {
        UnparameterizedQueryRule rule = new UnparameterizedQueryRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders LIMIT 20",
            "Seq Scan on demo_orders (rows=20)"
        );

        assertTrue(result.isEmpty());
    }
}