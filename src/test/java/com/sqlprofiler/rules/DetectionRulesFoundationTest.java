package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionRulesFoundationTest {

    private static final String LARGE_SEQ_SCAN_PLAN =
        "Seq Scan on demo_orders " +
        "(cost=0.00..1000.00 rows=50000 width=32)";

    @Test
    void seqScanRuleDetectsLargeUnfilteredScan() {
        SeqScanRule rule = new SeqScanRule();

        Optional<Finding> result = rule.analyze(
            "SELECT * FROM demo_orders",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isPresent());
        assertEquals("SEQ_SCAN", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
    }

    @Test
    void seqScanRuleDetectsRowsRemovedByFilter() {
        SeqScanRule rule = new SeqScanRule();

        Optional<Finding> result = rule.analyze(
            "SELECT * FROM demo_orders WHERE customer_id = 42",
            "Seq Scan on demo_orders " +
            "(cost=0.00..1200.00 rows=48 width=32)\n" +
            "Rows Removed by Filter: 49952"
        );

        assertTrue(result.isPresent());
        assertEquals("SEQ_SCAN", result.get().getRuleName());
        assertEquals("CRITICAL", result.get().getSeverity());
        assertTrue(result.get().getFixSql().contains("customer_id"));
    }

    @Test
    void seqScanRuleIgnoresSmallScan() {
        SeqScanRule rule = new SeqScanRule();

        Optional<Finding> result = rule.analyze(
            "SELECT * FROM demo_orders WHERE customer_id = 42",
            "Seq Scan on demo_orders " +
            "(cost=0.00..12.00 rows=2 width=32)\n" +
            "Rows Removed by Filter: 20"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void noWhereClauseRuleDetectsMissingFilter() {
        NoWhereClauseRule rule = new NoWhereClauseRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id, customer_id FROM demo_orders",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isPresent());
        assertEquals("NO_WHERE_CLAUSE", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
    }

    @Test
    void noWhereClauseRuleIgnoresFilteredQuery() {
        NoWhereClauseRule rule = new NoWhereClauseRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id, customer_id FROM demo_orders WHERE customer_id = 42",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void selectStarRuleDetectsSelectStar() {
        SelectStarRule rule = new SelectStarRule();

        Optional<Finding> result = rule.analyze(
            "SELECT * FROM demo_orders",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isPresent());
        assertEquals("SELECT_STAR", result.get().getRuleName());
        assertEquals("MEDIUM", result.get().getSeverity());
    }

    @Test
    void selectStarRuleIgnoresExplicitColumns() {
        SelectStarRule rule = new SelectStarRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id, customer_id, amount FROM demo_orders",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void noLimitRuleDetectsUnboundedQuery() {
        NoLimitRule rule = new NoLimitRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id, amount FROM demo_orders WHERE customer_id = 42",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isPresent());
        assertEquals("NO_LIMIT", result.get().getRuleName());
        assertEquals("MEDIUM", result.get().getSeverity());
    }

    @Test
    void noLimitRuleIgnoresLimitedQuery() {
        NoLimitRule rule = new NoLimitRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id, amount FROM demo_orders " +
            "WHERE customer_id = 42 LIMIT 20",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void noLimitRuleIgnoresAggregateQuery() {
        NoLimitRule rule = new NoLimitRule();

        Optional<Finding> result = rule.analyze(
            "SELECT COUNT(*) FROM demo_orders",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void functionOnColumnRuleDetectsDateFunction() {
        FunctionOnColumnRule rule = new FunctionOnColumnRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE DATE(created_at) = CURRENT_DATE",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isPresent());
        assertEquals("FUNCTION_ON_COLUMN", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
        assertTrue(result.get().getEvidence().contains("DATE"));
    }

    @Test
    void functionOnColumnRuleIgnoresNormalColumnComparison() {
        FunctionOnColumnRule rule = new FunctionOnColumnRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE created_at = CURRENT_DATE",
            LARGE_SEQ_SCAN_PLAN
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void functionOnColumnRuleIgnoresQueryWithoutWhereClause() {
        FunctionOnColumnRule rule = new FunctionOnColumnRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders",
            LARGE_SEQ_SCAN_PLAN
        );

        assertFalse(result.isPresent());
    }
}