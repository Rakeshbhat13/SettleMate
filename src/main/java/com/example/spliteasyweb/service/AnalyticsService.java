package com.example.spliteasyweb.service;

import com.example.spliteasyweb.model.ExpenseCategory;
import com.example.spliteasyweb.model.ExpenseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {
    public record Summary(
            BigDecimal totalSpent,
            BigDecimal highestExpense,
            BigDecimal averageExpense,
            int expenseCount,
            int memberCount,
            Map<ExpenseCategory, BigDecimal> spendingByCategory,
            BigDecimal budgetRemaining,
            BigDecimal budgetPercent) {
    }

    public Summary summarize(List<ExpenseEntity> expenses, int memberCount, BigDecimal budget) {
        BigDecimal total = expenses.stream()
                .map(ExpenseEntity::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal highest = expenses.stream()
                .map(ExpenseEntity::getAmount)
                .filter(amount -> amount != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal average = expenses.isEmpty() ? BigDecimal.ZERO : total
                .divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);

        Map<ExpenseCategory, BigDecimal> byCategory = new LinkedHashMap<>();
        for (ExpenseEntity expense : expenses) {
            ExpenseCategory category = expense.getCategory() == null ? ExpenseCategory.OTHER : expense.getCategory();
            byCategory.merge(category, expense.getAmount() == null ? BigDecimal.ZERO : expense.getAmount(), BigDecimal::add);
        }
        byCategory.replaceAll((category, amount) -> amount.setScale(2, RoundingMode.HALF_UP));

        BigDecimal remaining = budget == null ? null : budget.subtract(total).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percent = budget == null || budget.signum() == 0 ? BigDecimal.ZERO
                : total.multiply(BigDecimal.valueOf(100)).divide(budget, 1, RoundingMode.HALF_UP);

        return new Summary(total, highest, average, expenses.size(), memberCount,
                byCategory, remaining, percent);
    }
}
