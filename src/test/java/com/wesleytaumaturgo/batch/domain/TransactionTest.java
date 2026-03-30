package com.wesleytaumaturgo.batch.domain;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import com.wesleytaumaturgo.batch.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionTest {

    // ── Status transitions ───────────────────────────────────────────────────

    @Test
    void should_start_with_pending_status_when_created() {
        // REQ-1.EARS-1
        Transaction t = buildPending();
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void should_set_status_to_processed_when_markProcessed_is_called() {
        // REQ-1.EARS-2
        Transaction t = buildPending();
        LocalDateTime now = LocalDateTime.now();

        t.markProcessed(now);

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.PROCESSED);
        assertThat(t.getProcessedAt()).isEqualTo(now);
    }

    @Test
    void should_set_status_to_failed_when_markFailed_is_called() {
        // REQ-1.EARS-3
        Transaction t = buildPending();

        t.markFailed();

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(t.getProcessedAt()).isNull();
    }

    @Test
    void should_reset_to_pending_when_resetToPending_is_called() {
        // REQ-1.EARS-4
        Transaction t = buildPending();
        t.markProcessed(LocalDateTime.now());

        t.resetToPending();

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(t.getProcessedAt()).isNull();
    }

    // ── equals / hashCode ────────────────────────────────────────────────────

    @Test
    void should_be_equal_when_ids_are_the_same() {
        UUID id = UUID.randomUUID();
        Transaction a = buildWith(id, "ACC-1", "ACC-2");
        Transaction b = buildWith(id, "ACC-3", "ACC-4");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void should_not_be_equal_when_ids_differ() {
        Transaction a = buildPending();
        Transaction b = buildPending();

        assertThat(a).isNotEqualTo(b);
    }

    // ── Field access ─────────────────────────────────────────────────────────

    @Test
    void should_preserve_all_fields_when_constructed() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Transaction t = new Transaction(id, "0000000001", "0000000002",
                BigDecimal.valueOf(999.99), "BRL", TransactionType.PAYMENT,
                TransactionStatus.PENDING, now);

        assertThat(t.getId()).isEqualTo(id);
        assertThat(t.getSourceAccount()).isEqualTo("0000000001");
        assertThat(t.getTargetAccount()).isEqualTo("0000000002");
        assertThat(t.getAmount()).isEqualByComparingTo("999.99");
        assertThat(t.getCurrency()).isEqualTo("BRL");
        assertThat(t.getType()).isEqualTo(TransactionType.PAYMENT);
        assertThat(t.getCreatedAt()).isEqualTo(now);
    }

    // ── equals edge cases ────────────────────────────────────────────────────

    @Test
    void should_not_be_equal_to_null() {
        assertThat(buildPending()).isNotEqualTo(null);
    }

    @Test
    void should_not_be_equal_to_different_type() {
        assertThat(buildPending()).isNotEqualTo("string");
    }

    @Test
    void should_be_equal_to_itself() {
        Transaction t = buildPending();
        assertThat(t).isEqualTo(t);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Transaction buildPending() {
        return buildWith(UUID.randomUUID(), "0000000001", "0000000002");
    }

    private Transaction buildWith(UUID id, String source, String target) {
        return new Transaction(id, source, target,
                BigDecimal.valueOf(100.00), "BRL",
                TransactionType.PAYMENT, TransactionStatus.PENDING,
                LocalDateTime.now());
    }
}
