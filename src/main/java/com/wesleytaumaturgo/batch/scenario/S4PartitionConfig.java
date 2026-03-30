package com.wesleytaumaturgo.batch.scenario;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.repository.TransactionRepository;
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
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * Cenário 4 — Partitioning: GridPartitioner com 8 partições em paralelo.
 * Cada worker compete pelo pool de registros PENDING via SynchronizedItemStreamReader.
 */
@Configuration
public class S4PartitionConfig {

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${benchmark.chunk-size:500}")
    private int chunkSize;

    @Value("${benchmark.grid-size:8}")
    private int gridSize;

    @Bean
    public Job s4PartitionJob(JobRepository jobRepository, Step s4MasterStep) {
        return new JobBuilder("s4PartitionJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(s4MasterStep)
                .build();
    }

    @Bean
    public Step s4MasterStep(JobRepository jobRepository,
                             Step s4SlaveStep,
                             TransactionRangePartitioner s4Partitioner) {
        return new StepBuilder("s4MasterStep", jobRepository)
                .partitioner("s4SlaveStep", s4Partitioner)
                .step(s4SlaveStep)
                .taskExecutor(s4TaskExecutor())
                .gridSize(gridSize)
                .build();
    }

    @Bean
    public Step s4SlaveStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            EntityManagerFactory emf,
                            TransactionItemProcessor processor) {
        return new StepBuilder("s4SlaveStep", jobRepository)
                .<Transaction, Transaction>chunk(chunkSize, transactionManager)
                .reader(s4SyncReader(emf))
                .processor(processor)
                .writer(s4SlaveWriter())
                .build();
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<Transaction> s4SyncReader(EntityManagerFactory emf) {
        JpaPagingItemReader<Transaction> reader = new JpaPagingItemReaderBuilder<Transaction>()
                .name("s4JpaReader")
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
    public RepositoryItemWriter<Transaction> s4SlaveWriter() {
        return new RepositoryItemWriterBuilder<Transaction>()
                .repository(transactionRepository)
                .methodName("save")
                .build();
    }

    @Bean
    public TransactionRangePartitioner s4Partitioner() {
        return new TransactionRangePartitioner();
    }

    @Bean
    public TaskExecutor s4TaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("s4-partition-");
        executor.setConcurrencyLimit(gridSize);
        return executor;
    }
}
