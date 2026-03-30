package com.wesleytaumaturgo.batch.scenario;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.partitioner.TransactionRangePartitioner;
import com.wesleytaumaturgo.batch.processor.TransactionItemProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Cenário 7 — All combined: S2 (chunk) + S3 (multi-thread) + S4 (partitioning)
 *             + S5 (JdbcBatchItemWriter) + S6 (HikariCP via application.yml).
 */
@Configuration
public class S7AllCombinedConfig {

    private static final String UPDATE_SQL =
            "UPDATE transactions SET status = :status, processed_at = :processedAt WHERE id = :id";

    @Value("${benchmark.chunk-size:500}")
    private int chunkSize;

    @Value("${benchmark.grid-size:8}")
    private int gridSize;

    @Bean
    public Job s7AllCombinedJob(JobRepository jobRepository, Step s7MasterStep) {
        return new JobBuilder("s7AllCombinedJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(s7MasterStep)
                .build();
    }

    @Bean
    public Step s7MasterStep(JobRepository jobRepository,
                             Step s7SlaveStep,
                             TransactionRangePartitioner s7Partitioner) {
        return new StepBuilder("s7MasterStep", jobRepository)
                .partitioner("s7SlaveStep", s7Partitioner)
                .step(s7SlaveStep)
                .taskExecutor(s7TaskExecutor())
                .gridSize(gridSize)
                .build();
    }

    @Bean
    public Step s7SlaveStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            EntityManagerFactory emf,
                            DataSource dataSource,
                            TransactionItemProcessor processor) {
        return new StepBuilder("s7SlaveStep", jobRepository)
                .<Transaction, Transaction>chunk(chunkSize, transactionManager)
                .reader(s7SyncReader(emf))
                .processor(processor)
                .writer(s7SqlBatchWriter(dataSource))
                .build();
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<Transaction> s7SyncReader(EntityManagerFactory emf) {
        JpaPagingItemReader<Transaction> reader = new JpaPagingItemReaderBuilder<Transaction>()
                .name("s7JpaReader")
                .entityManagerFactory(emf)
                .queryString("SELECT t FROM Transaction t ORDER BY t.createdAt")
                .pageSize(chunkSize)
                .saveState(false)
                .build();

        return new SynchronizedItemStreamReaderBuilder<Transaction>()
                .delegate(reader)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Transaction> s7SqlBatchWriter(DataSource dataSource) {
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

    @Bean
    public TransactionRangePartitioner s7Partitioner() {
        return new TransactionRangePartitioner();
    }

    @Bean
    public TaskExecutor s7TaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("s7-thread-");
        executor.setConcurrencyLimit(gridSize);
        return executor;
    }
}
