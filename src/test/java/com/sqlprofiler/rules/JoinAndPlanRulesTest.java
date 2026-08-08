package com.sqlprofiler.rules;

import com.sqlprofiler.model.Finding;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinAndPlanRulesTest {

    @Test
    void cartesianJoinRuleDetectsCrossJoin() {
        CartesianJoinRule rule = new CartesianJoinRule();

        Optional<Finding> result = rule.analyze(
            "SELECT o.id, c.id " +
            "FROM demo_orders o CROSS JOIN demo_customers c",
            "Nested Loop (rows=500000)"
        );

        assertTrue(result.isPresent());
        assertEquals("CARTESIAN_JOIN", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
    }

    @Test
    void cartesianJoinRuleIgnoresExplicitJoinCondition() {
        CartesianJoinRule rule = new CartesianJoinRule();

        Optional<Finding> result = rule.analyze(
            "SELECT o.id, c.id " +
            "FROM demo_orders o " +
            "JOIN demo_customers c ON o.customer_id = c.id",
            "Hash Join (rows=100)"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void implicitTypeCastRuleDetectsPostgresCast() {
        ImplicitTypeCastRule rule = new ImplicitTypeCastRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_id::text = '42'",
            "Seq Scan on demo_orders"
        );

        assertTrue(result.isPresent());
        assertEquals("IMPLICIT_TYPE_CAST", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
        assertTrue(result.get().getEvidence().contains("customer_id"));
    }

    @Test
    void implicitTypeCastRuleIgnoresNativeTypeComparison() {
        ImplicitTypeCastRule rule = new ImplicitTypeCastRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_id = 42",
            "Index Scan using idx_demo_orders_customer_id"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void nestedLoopRuleDetectsNestedLoopPlan() {
        NestedLoopRule rule = new NestedLoopRule();

        Optional<Finding> result = rule.analyze(
            "SELECT o.id, c.id " +
            "FROM demo_orders o " +
            "JOIN demo_customers c ON o.customer_id = c.id",
            "Nested Loop " +
            "(cost=0.00..5000.00 rows=25000 width=64)"
        );

        assertTrue(result.isPresent());
        assertEquals("NESTED_LOOP", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
    }

    @Test
    void nestedLoopRuleIgnoresHashJoinPlan() {
        NestedLoopRule rule = new NestedLoopRule();

        Optional<Finding> result = rule.analyze(
            "SELECT o.id, c.id " +
            "FROM demo_orders o " +
            "JOIN demo_customers c ON o.customer_id = c.id",
            "Hash Join " +
            "(cost=0.00..1000.00 rows=500 width=64)"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void highRowEstimateRuleDetectsLargeEstimate() {
        HighRowEstimateRule rule = new HighRowEstimateRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders",
            "Seq Scan on demo_orders " +
            "(cost=0.00..1000.00 rows=50000 width=32)"
        );

        assertTrue(result.isPresent());
        assertEquals("HIGH_ROW_ESTIMATE", result.get().getRuleName());
        assertEquals("HIGH", result.get().getSeverity());
        assertTrue(result.get().getEvidence().contains("50000"));
    }

    @Test
    void highRowEstimateRuleIgnoresSmallEstimate() {
        HighRowEstimateRule rule = new HighRowEstimateRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders WHERE customer_id = 42",
            "Index Scan using idx_demo_orders_customer_id " +
            "(cost=0.00..20.00 rows=48 width=32)"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void orOnIndexedColumnRuleDetectsRepeatedOrColumn() {
        OrOnIndexedColumnRule rule = new OrOnIndexedColumnRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_id = 42 OR customer_id = 43",
            "Bitmap Heap Scan on demo_orders"
        );

        assertTrue(result.isPresent());
        assertEquals("OR_ON_INDEXED_COLUMN", result.get().getRuleName());
        assertEquals("MEDIUM", result.get().getSeverity());
        assertTrue(result.get().getEvidence().contains("customer_id"));
    }

    @Test
    void orOnIndexedColumnRuleIgnoresSingleCondition() {
        OrOnIndexedColumnRule rule = new OrOnIndexedColumnRule();

        Optional<Finding> result = rule.analyze(
            "SELECT id FROM demo_orders " +
            "WHERE customer_id = 42 AND amount > 100",
            "Index Scan using idx_demo_orders_customer_id"
        );

        assertTrue(result.isEmpty());
    }
}