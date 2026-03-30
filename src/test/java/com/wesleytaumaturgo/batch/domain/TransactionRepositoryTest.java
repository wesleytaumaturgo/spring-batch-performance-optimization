package com.wesleytaumaturgo.batch.domain;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import com.wesleytaumaturgo.batch.domain.model.TransactionType;
import com.wesleytaumaturgo.batch.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void should_count_transactions_by_status() {
        // REQ-1.EARS-5
        save(TransactionStatus.PENDING);
        save(TransactionStatus.PENDING);
        save(TransactionStatus.PROCESSED);

        assertThat(repository.countByStatus(TransactionStatus.PENDING)).isEqualTo(2);
        assertThat(repository.countByStatus(TransactionStatus.PROCESSED)).isEqualTo(1);
    }

    @Test
    void should_return_pending_transactions_ordered_by_created_at() {
        // REQ-1.EARS-6
        LocalDateTime older = LocalDateTime.now().minusDays(2);
        LocalDateTime newer = LocalDateTime.now().minusDays(1);

        save(TransactionStatus.PENDING, newer);
        save(TransactionStatus.PENDING, older);
        save(TransactionStatus.PROCESSED, older);

        List<Transaction> result = repository.findByStatusOrderByCreatedAt(TransactionStatus.PENDING);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCreatedAt()).isEqualTo(older);
        assertThat(result.get(1).getCreatedAt()).isEqualTo(newer);
    }

    @Test
    void should_reset_all_transactions_to_pending() {
        // REQ-1.EARS-7
        Transaction t = save(TransactionStatus.PROCESSED);
        t.markProcessed(LocalDateTime.now());
        repository.save(t);

        int updated = repository.resetAllToPending();

        assertThat(updated).isGreaterThanOrEqualTo(1);
        assertThat(repository.countByStatus(TransactionStatus.PENDING)).isEqualTo(1);
    }

    @Test
    void should_persist_and_find_transaction_by_id() {
        // REQ-1.EARS-8
        Transaction t = save(TransactionStatus.PENDING);

        assertThat(repository.findById(t.getId())).isPresent();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Transaction save(TransactionStatus status) {
        return save(status, LocalDateTime.now());
    }

    private Transaction save(TransactionStatus status, LocalDateTime createdAt) {
        return repository.save(new Transaction(
                UUID.randomUUID(), "0000000001", "0000000002",
                BigDecimal.valueOf(100.00), "BRL",
                TransactionType.PAYMENT, status, createdAt));
    }
}
