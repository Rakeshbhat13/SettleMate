package com.example.spliteasyweb.model;

public enum ExpenseCategory {
    FOOD("Food"),
    TRAVEL("Travel"),
    ACCOMMODATION("Accommodation"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    ENTERTAINMENT("Entertainment"),
    BILLS("Bills"),
    OTHER("Other");

    private final String label;

    ExpenseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
