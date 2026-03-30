package com.wesleytaumaturgo.batch.scenario;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.processor.TransactionItemProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Cenário 5 — SQL batch inserts: JdbcBatchItemWriter com UPDATE em lote.
 * Usa JDBC nativo para executar batch de UPDATEs em vez de JPA individual.
 */
@Configuration
public class S5SqlBatchConfig {

    private static final String UPDATE_SQL =
            "UPDATE transactions SET status = :status, processed_at = :processedAt WHERE id = :id";

    @Value("${benchmark.chunk-size:500}")
    private int chunkSize;

    @Bean
    public Job s5SqlBatchJob(JobRepository jobRepository, Step s5SqlBatchStep) {
        return new JobBuilder("s5SqlBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(s5SqlBatchStep)
                .build();
    }

    @Bean
    public Step s5SqlBatchStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               EntityManagerFactory emf,
                               DataSource dataSource,
                               TransactionItemProcessor processor) {
        return new StepBuilder("s5SqlBatchStep", jobRepository)
                .<Transaction, Transaction>chunk(chunkSize, transactionManager)
                .reader(s5SqlBatchReader(emf))
                .processor(processor)
                .writer(s5SqlBatchWriter(dataSource))
                .build();
    }

    @Bean
    public JpaPagingItemReader<Transaction> s5SqlBatchReader(EntityManagerFactory emf) {
        return new JpaPagingItemReaderBuilder<Transaction>()
                .name("s5SqlBatchReader")
                .entityManagerFactory(emf)
                .queryString("SELECT t FROM Transaction t ORDER BY t.createdAt")
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaction> s5SqlBatchWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction>()
                .dataSource(dataSource)
                .sql(UPDATE_SQL)
                .itemSqlParameterSourceProvider(t -> {
                    MapSqlParameterSource p = new MapSqlParameterSource();
                    p.addValue("id", t.getId());
                    p.addValue("status", t.getStatus().name());
                    p.addValue("processedAt", t.getProcessedAt());
                    return p;
                })
                .build();
    }
}
