package com.example.spliteasyweb.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "groups")
public class GroupEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false, unique=true)
    private String slug;

    // dueño anónimo por sesión (para la home)
    @Column(name = "owner_session")
    private String ownerSession;

    @Column(length = 500)
    private String description;

    @Column(precision = 14, scale = 2)
    private BigDecimal budget;

    @Column(nullable = false)
    private LocalDate createdDate = LocalDate.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getOwnerSession() { return ownerSession; }
    public void setOwnerSession(String ownerSession) { this.ownerSession = ownerSession; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
}
