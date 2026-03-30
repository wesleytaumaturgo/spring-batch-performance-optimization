package com.wesleytaumaturgo.batch.generator;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import com.wesleytaumaturgo.batch.domain.model.TransactionType;
import com.wesleytaumaturgo.batch.domain.repository.TransactionRepository;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gera transações fictícias usando DataFaker para dados realistas.
 * Salva em lotes de 1.000 registros e loga o progresso.
 */
@Component
public class TransactionDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(TransactionDataGenerator.class);
    private static final int BATCH_SIZE = 1_000;
    private static final TransactionType[] TYPES = TransactionType.values();

    private final TransactionRepository repository;
    private final Faker faker;

    public TransactionDataGenerator(TransactionRepository repository) {
        this.repository = repository;
        this.faker = new Faker();
    }

    public void generate(long totalRecords) {
        log.info("Gerando {} transações...", totalRecords);
        repository.deleteAll();

        List<Transaction> batch = new ArrayList<>(BATCH_SIZE);
        long saved = 0;

        for (long i = 0; i < totalRecords; i++) {
            batch.add(buildRandom());

            if (batch.size() == BATCH_SIZE) {
                repository.saveAll(batch);
                saved += batch.size();
                batch.clear();
                if (saved % 10_000 == 0) {
                    log.info("Progresso: {}/{} ({} %)", saved, totalRecords,
                            Math.round(saved * 100.0 / totalRecords));
                }
            }
        }

        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            saved += batch.size();
        }

        log.info("Geração concluída: {} transações inseridas.", saved);
    }

    public long count() {
        return repository.count();
    }

    private Transaction buildRandom() {
        return new Transaction(
                UUID.randomUUID(),
                faker.numerify("##########"),
                faker.numerify("##########"),
                randomAmount(),
                "BRL",
                TYPES[faker.random().nextInt(TYPES.length)],
                TransactionStatus.PENDING,
                LocalDateTime.now().minusDays(faker.random().nextInt(30))
        );
    }

    private BigDecimal randomAmount() {
        double raw = 1.0 + faker.random().nextDouble() * 99_999.0;
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
    }
}
