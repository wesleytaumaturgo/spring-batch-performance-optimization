package com.wesleytaumaturgo.batch.scenario;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import com.wesleytaumaturgo.batch.domain.model.TransactionType;
import com.wesleytaumaturgo.batch.domain.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base para testes de cenários de batch.
 * Provê helpers para popular e verificar o estado das transações.
 */
public abstract class BatchScenarioBaseTest {

    @Autowired
    protected TransactionRepository repository;

    protected void insertPendingTransactions(int count) {
        repository.deleteAll();
        List<Transaction> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new Transaction(
                    UUID.randomUUID(),
                    String.format("%010d", i),
                    String.format("%010d", i + 1),
                    BigDecimal.valueOf(100.00 + i),
                    "BRL",
                    TransactionType.PAYMENT,
                    TransactionStatus.PENDING,
                    LocalDateTime.now().minusMinutes(count - i)
            ));
        }
        repository.saveAll(list);
    }

    protected long countProcessed() {
        return repository.countByStatus(TransactionStatus.PROCESSED);
    }

    protected long countPending() {
        return repository.countByStatus(TransactionStatus.PENDING);
    }
}
