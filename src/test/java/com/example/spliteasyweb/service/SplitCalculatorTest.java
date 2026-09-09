package com.example.spliteasyweb.service;

import com.example.spliteasyweb.model.SplitType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SplitCalculatorTest {
    private final SplitCalculator calculator = new SplitCalculator();
    private final List<String> members = List.of("Ana", "Ben", "Cara", "Dev");

    @Test
    void splitsEquallyAndPreservesEveryCent() {
        var result = calculator.calculate(new BigDecimal("1000.00"), members, SplitType.EQUAL, null);

        assertEquals(new BigDecimal("250.00"), result.get("Ana"));
        assertEquals(new BigDecimal("1000.00"), result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void distributesRemainderCentsDeterministically() {
        var result = calculator.calculate(new BigDecimal("10.00"), List.of("Ana", "Ben", "Cara"), SplitType.EQUAL, null);

        assertEquals(new BigDecimal("3.34"), result.get("Ana"));
        assertEquals(new BigDecimal("3.33"), result.get("Ben"));
        assertEquals(new BigDecimal("3.33"), result.get("Cara"));
    }

    @Test
    void calculatesPercentageSplit() {
        var result = calculator.calculate(new BigDecimal("1000.00"), members, SplitType.PERCENTAGE,
                Map.of("Ana", new BigDecimal("40"), "Ben", new BigDecimal("30"),
                        "Cara", new BigDecimal("20"), "Dev", new BigDecimal("10")));

        assertEquals(new BigDecimal("400.00"), result.get("Ana"));
        assertEquals(new BigDecimal("1000.00"), result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void rejectsPercentagesThatDoNotTotalOneHundred() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                new BigDecimal("100.00"), members, SplitType.PERCENTAGE,
                Map.of("Ana", new BigDecimal("25"), "Ben", new BigDecimal("25"),
                        "Cara", new BigDecimal("25"), "Dev", new BigDecimal("24"))));
    }

    @Test
    void validatesExactSplitTotal() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(
                new BigDecimal("100.00"), members, SplitType.EXACT,
                Map.of("Ana", new BigDecimal("30.00"), "Ben", new BigDecimal("30.00"),
                        "Cara", new BigDecimal("30.00"), "Dev", new BigDecimal("9.00"))));
    }
}
