package com.wesleytaumaturgo.batch.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_status", columnList = "status"),
        @Index(name = "idx_transactions_created_at", columnList = "created_at")
})
public class Transaction {

    @Id
    @NotNull
    private UUID id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "source_account", nullable = false, length = 20)
    private String sourceAccount;

    @NotBlank
    @Size(max = 20)
    @Column(name = "target_account", nullable = false, length = 20)
    private String targetAccount;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(nullable = false, length = 3)
    private String currency;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionStatus status;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    protected Transaction() {}

    public Transaction(UUID id,
                       String sourceAccount,
                       String targetAccount,
                       BigDecimal amount,
                       String currency,
                       TransactionType type,
                       TransactionStatus status,
                       LocalDateTime createdAt) {
        this.id = id;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ── State transitions ────────────────────────────────────────────────────

    public void markProcessed(LocalDateTime processedAt) {
        this.status = TransactionStatus.PROCESSED;
        this.processedAt = processedAt;
    }

    public void markFailed() {
        this.status = TransactionStatus.FAILED;
    }

    public void resetToPending() {
        this.status = TransactionStatus.PENDING;
        this.processedAt = null;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public String getSourceAccount() { return sourceAccount; }
    public String getTargetAccount() { return targetAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }

    // ── equals / hashCode (por ID) ───────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Transaction{id=" + id + ", status=" + status + ", amount=" + amount + "}";
    }
}
