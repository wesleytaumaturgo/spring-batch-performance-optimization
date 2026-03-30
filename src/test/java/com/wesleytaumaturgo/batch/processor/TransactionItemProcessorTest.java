package com.wesleytaumaturgo.batch.processor;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import com.wesleytaumaturgo.batch.domain.model.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionItemProcessorTest {

    private final TransactionItemProcessor processor = new TransactionItemProcessor();

    @Test
    void should_mark_pending_transaction_as_processed() throws Exception {
        // REQ-2.EARS-1
        Transaction t = buildWith(TransactionStatus.PENDING);

        Transaction result = processor.process(t);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PROCESSED);
    }

    @Test
    void should_set_processed_at_for_pending_transaction() throws Exception {
        // REQ-2.EARS-2
        Transaction t = buildWith(TransactionStatus.PENDING);
        assertThat(t.getProcessedAt()).isNull();

        processor.process(t);

        assertThat(t.getProcessedAt()).isNotNull();
    }

    @Test
    void should_return_same_instance_after_processing() throws Exception {
        // REQ-2.EARS-3
        Transaction t = buildWith(TransactionStatus.PENDING);

        Transaction result = processor.process(t);

        assertThat(result).isSameAs(t);
    }

    @Test
    void should_return_null_for_already_processed_transaction() throws Exception {
        // REQ-2.EARS-4 — evita page-shift problem no JpaPagingItemReader
        Transaction t = buildWith(TransactionStatus.PROCESSED);

        Transaction result = processor.process(t);

        assertThat(result).isNull();
    }

    @Test
    void should_return_null_for_failed_transaction() throws Exception {
        // REQ-2.EARS-5
        Transaction t = buildWith(TransactionStatus.FAILED);

        Transaction result = processor.process(t);

        assertThat(result).isNull();
    }

    private Transaction buildWith(TransactionStatus status) {
        return new Transaction(UUID.randomUUID(), "0000000001", "0000000002",
                BigDecimal.valueOf(250.00), "BRL", TransactionType.TRANSFER,
                status, LocalDateTime.now());
    }
}
