package com.example.spliteasyweb.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
public class SettlementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "from_person", nullable = false, length = 120)
    private String fromPerson;

    @Column(name = "to_person", nullable = false, length = 120)
    private String toPerson;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime settledAt = LocalDateTime.now();

    @Column(length = 500)
    private String note;

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getFromPerson() { return fromPerson; }
    public void setFromPerson(String fromPerson) { this.fromPerson = fromPerson; }
    public String getToPerson() { return toPerson; }
    public void setToPerson(String toPerson) { this.toPerson = toPerson; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
