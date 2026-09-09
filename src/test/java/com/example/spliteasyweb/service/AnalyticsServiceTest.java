package com.example.spliteasyweb.service;

import com.example.spliteasyweb.model.ExpenseCategory;
import com.example.spliteasyweb.model.ExpenseEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsServiceTest {
    private final AnalyticsService analytics = new AnalyticsService();

    @Test
    void summarizesStoredExpensesByCategoryAndBudget() {
        ExpenseEntity dinner = expense("Dinner", "120.00", ExpenseCategory.FOOD);
        ExpenseEntity train = expense("Train", "80.00", ExpenseCategory.TRAVEL);

        var summary = analytics.summarize(List.of(dinner, train), 4, new BigDecimal("500.00"));

        assertEquals(new BigDecimal("200.00"), summary.totalSpent());
        assertEquals(new BigDecimal("100.00"), summary.averageExpense());
        assertEquals(new BigDecimal("300.00"), summary.budgetRemaining());
        assertEquals(new BigDecimal("40.0"), summary.budgetPercent());
        assertEquals(new BigDecimal("120.00"), summary.spendingByCategory().get(ExpenseCategory.FOOD));
    }

    private ExpenseEntity expense(String title, String amount, ExpenseCategory category) {
        ExpenseEntity expense = new ExpenseEntity();
        expense.setTitle(title);
        expense.setAmount(new BigDecimal(amount));
        expense.setCategory(category);
        return expense;
    }
}
