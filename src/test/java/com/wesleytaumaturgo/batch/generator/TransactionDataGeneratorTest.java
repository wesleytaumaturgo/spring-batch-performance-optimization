package com.wesleytaumaturgo.batch.generator;

import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import com.wesleytaumaturgo.batch.domain.repository.TransactionRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("T3")
@DataJpaTest
@ActiveProfiles("test")
@Import(TransactionDataGenerator.class)
class TransactionDataGeneratorTest {

    @Autowired
    private TransactionDataGenerator generator;

    @Autowired
    private TransactionRepository repository;

    @Test
    void should_generate_requested_number_of_transactions() {
        // REQ-5.EARS-1
        generator.generate(100);

        assertThat(generator.count()).isEqualTo(100);
    }

    @Test
    void should_generate_all_transactions_as_pending() {
        // REQ-5.EARS-2
        generator.generate(50);

        assertThat(repository.countByStatus(TransactionStatus.PENDING)).isEqualTo(50);
        assertThat(repository.countByStatus(TransactionStatus.PROCESSED)).isZero();
    }

    @Test
    void should_clear_existing_data_before_generating() {
        generator.generate(20);
        generator.generate(30);

        assertThat(generator.count()).isEqualTo(30);
    }
}
