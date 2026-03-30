package com.wesleytaumaturgo.batch.scenario;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.repository.TransactionRepository;
import com.wesleytaumaturgo.batch.processor.TransactionItemProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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
 * Cenário 3 — Multi-thread: chunk=500, TaskExecutor com 8 threads.
 * O reader é protegido por SynchronizedItemStreamReader para thread-safety.
 */
@Configuration
public class S3MultiThreadConfig {

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${benchmark.chunk-size:500}")
    private int chunkSize;

    @Value("${benchmark.thread-count:8}")
    private int threadCount;

    @Bean
    public Job s3MultiThreadJob(JobRepository jobRepository, Step s3MultiThreadStep) {
        return new JobBuilder("s3MultiThreadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(s3MultiThreadStep)
                .build();
    }

    @Bean
    public Step s3MultiThreadStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  EntityManagerFactory emf,
                                  TransactionItemProcessor processor) {
        return new StepBuilder("s3MultiThreadStep", jobRepository)
                .<Transaction, Transaction>chunk(chunkSize, transactionManager)
                .reader(s3SyncReader(emf))
                .processor(processor)
                .writer(s3MultiThreadWriter())
                .taskExecutor(s3TaskExecutor())
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<Transaction> s3SyncReader(EntityManagerFactory emf) {
        JpaPagingItemReader<Transaction> reader = new JpaPagingItemReaderBuilder<Transaction>()
                .name("s3JpaReader")
                .entityManagerFactory(emf)
                .queryString("SELECT t FROM Transaction t ORDER BY t.createdAt")
                .pageSize(chunkSize)
                .build();

        return new SynchronizedItemStreamReaderBuilder<Transaction>()
                .delegate(reader)
                .build();
    }

    @Bean
    public RepositoryItemWriter<Transaction> s3MultiThreadWriter() {
        return new RepositoryItemWriterBuilder<Transaction>()
                .repository(transactionRepository)
                .methodName("save")
                .build();
    }

    @Bean
    public TaskExecutor s3TaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("s3-thread-");
        executor.setConcurrencyLimit(threadCount);
        return executor;
    }
}
