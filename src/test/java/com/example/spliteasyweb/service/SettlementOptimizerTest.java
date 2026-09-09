package com.example.spliteasyweb.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementOptimizerTest {
    private final SettlementOptimizer optimizer = new SettlementOptimizer();

    @Test
    void matchesMultipleDebtorsToOneCreditor() {
        var balances = new LinkedHashMap<String, BigDecimal>();
        balances.put("A", new BigDecimal("500.00"));
        balances.put("B", new BigDecimal("-300.00"));
        balances.put("C", new BigDecimal("-200.00"));

        assertEquals(List.of(
                new SettlementOptimizer.Transfer("B", "A", new BigDecimal("300.00")),
                new SettlementOptimizer.Transfer("C", "A", new BigDecimal("200.00"))
        ), optimizer.optimize(balances));
    }

    @Test
    void ignoresBalancesBelowOneCent() {
        var balances = Map.of("A", new BigDecimal("0.004"), "B", new BigDecimal("-0.004"));

        assertEquals(List.of(), optimizer.optimize(balances));
    }

    @Test
    void settlesAcrossSeveralCreditors() {
        var balances = new LinkedHashMap<String, BigDecimal>();
        balances.put("A", new BigDecimal("-75.00"));
        balances.put("B", new BigDecimal("50.00"));
        balances.put("C", new BigDecimal("25.00"));

        assertEquals(List.of(
                new SettlementOptimizer.Transfer("A", "B", new BigDecimal("50.00")),
                new SettlementOptimizer.Transfer("A", "C", new BigDecimal("25.00"))
        ), optimizer.optimize(balances));
    }
}
