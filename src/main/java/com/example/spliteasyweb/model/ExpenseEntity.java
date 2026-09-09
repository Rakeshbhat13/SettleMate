package com.example.spliteasyweb.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class ExpenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private GroupEntity group;

    @Column(nullable = false)
    private String title;

    // Usamos DECIMAL para dinero
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    // Participantes (nombres visibles) separados por coma
    @Column(name = "participants_csv", nullable = false, length = 1000)
    private String participantsCsv;

    // NUEVO: pagadores (pueden ser varios), separados por coma. Nullable para no romper DB vieja
    @Column(name = "payers_csv", nullable = true, length = 1000)
    private String payersCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExpenseCategory category = ExpenseCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 32)
    private SplitType splitType = SplitType.EQUAL;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GroupEntity getGroup() { return group; }
    public void setGroup(GroupEntity group) { this.group = group; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getParticipantsCsv() { return participantsCsv; }
    public void setParticipantsCsv(String participantsCsv) { this.participantsCsv = participantsCsv; }

    public String getPayersCsv() { return payersCsv; }
    public void setPayersCsv(String payersCsv) { this.payersCsv = payersCsv; }

    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category == null ? ExpenseCategory.OTHER : category; }

    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType == null ? SplitType.EQUAL : splitType; }
}
