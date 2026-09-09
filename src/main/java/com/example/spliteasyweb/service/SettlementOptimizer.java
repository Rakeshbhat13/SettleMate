package com.example.spliteasyweb.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class SettlementOptimizer {
    private static final BigDecimal CENT = new BigDecimal("0.01");

    public record Transfer(String from, String to, BigDecimal amount) {
    }

    private record Position(String person, BigDecimal amount) {
    }

    public List<Transfer> optimize(Map<String, BigDecimal> balances) {
        PriorityQueue<Position> debtors = new PriorityQueue<>(
            Comparator.<Position, BigDecimal>comparing(Position::amount).reversed()
                .thenComparing(Position::person));
        PriorityQueue<Position> creditors = new PriorityQueue<>(
                Comparator.<Position, BigDecimal>comparing(Position::amount).reversed()
                        .thenComparing(Position::person));

        balances.forEach((person, balance) -> {
            BigDecimal normalized = money(balance);
            if (normalized.compareTo(CENT.negate()) <= 0) {
                debtors.add(new Position(person, normalized.negate()));
            } else if (normalized.compareTo(CENT) >= 0) {
                creditors.add(new Position(person, normalized));
            }
        });

        List<Transfer> transfers = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Position debtor = debtors.poll();
            Position creditor = creditors.poll();
            BigDecimal amount = money(debtor.amount().min(creditor.amount()));
            transfers.add(new Transfer(debtor.person(), creditor.person(), amount));

            BigDecimal remainingDebt = money(debtor.amount().subtract(amount));
            BigDecimal remainingCredit = money(creditor.amount().subtract(amount));
            if (remainingDebt.compareTo(CENT) >= 0) {
                debtors.add(new Position(debtor.person(), remainingDebt));
            }
            if (remainingCredit.compareTo(CENT) >= 0) {
                creditors.add(new Position(creditor.person(), remainingCredit));
            }
        }
        return transfers;
    }

    private BigDecimal money(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2) : amount.setScale(2, RoundingMode.HALF_UP);
    }
}
