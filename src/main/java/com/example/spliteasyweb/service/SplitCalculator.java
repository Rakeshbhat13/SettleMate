package com.example.spliteasyweb.service;

import com.example.spliteasyweb.model.SplitType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SplitCalculator {
    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public Map<String, BigDecimal> calculate(
            BigDecimal total,
            List<String> members,
            SplitType splitType,
            Map<String, BigDecimal> allocations) {
        requirePositive(total);
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }
        if (splitType == null) {
            throw new IllegalArgumentException("A split type is required");
        }

        return switch (splitType) {
            case EQUAL -> equal(total, members);
            case PERCENTAGE -> percentage(total, members, allocations);
            case EXACT -> exact(total, members, allocations);
        };
    }

    private Map<String, BigDecimal> equal(BigDecimal total, List<String> members) {
        long totalCents = total.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                .movePointRight(MONEY_SCALE).longValueExact();
        long baseCents = totalCents / members.size();
        long remainderCents = totalCents % members.size();

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int index = 0; index < members.size(); index++) {
            long cents = baseCents + (index < remainderCents ? 1 : 0);
            BigDecimal share = BigDecimal.valueOf(cents, MONEY_SCALE);
            result.put(members.get(index), share);
        }
        return result;
    }

    private Map<String, BigDecimal> percentage(
            BigDecimal total, List<String> members, Map<String, BigDecimal> percentages) {
        requireAllocations(members, percentages);
        BigDecimal sum = percentages.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(ONE_HUNDRED) != 0) {
            throw new IllegalArgumentException("Percentages must total exactly 100%");
        }

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String member : members) {
            BigDecimal percentage = nonNegative(percentages.get(member), member);
            result.put(member, total.multiply(percentage).movePointLeft(2)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }
        return adjustRounding(total, result);
    }

    private Map<String, BigDecimal> exact(
            BigDecimal total, List<String> members, Map<String, BigDecimal> amounts) {
        requireAllocations(members, amounts);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String member : members) {
            result.put(member, nonNegative(amounts.get(member), member)
                    .setScale(MONEY_SCALE, RoundingMode.UNNECESSARY));
        }
        BigDecimal sum = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(total.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)) != 0) {
            throw new IllegalArgumentException("Exact split amounts must equal the expense total");
        }
        return result;
    }

    private Map<String, BigDecimal> adjustRounding(BigDecimal total, Map<String, BigDecimal> result) {
        BigDecimal expected = total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal actual = result.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = expected.subtract(actual);
        if (difference.signum() != 0) {
            String last = null;
            for (String member : result.keySet()) {
                last = member;
            }
            result.put(last, result.get(last).add(difference));
        }
        return result;
    }

    private void requireAllocations(List<String> members, Map<String, BigDecimal> allocations) {
        if (allocations == null || allocations.size() != members.size()
                || !allocations.keySet().containsAll(members)) {
            throw new IllegalArgumentException("Every participant needs exactly one allocation");
        }
    }

    private BigDecimal nonNegative(BigDecimal value, String member) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Allocation for " + member + " must be non-negative");
        }
        return value;
    }

    private void requirePositive(BigDecimal total) {
        if (total == null || total.signum() <= 0) {
            throw new IllegalArgumentException("Expense amount must be positive");
        }
    }
}
