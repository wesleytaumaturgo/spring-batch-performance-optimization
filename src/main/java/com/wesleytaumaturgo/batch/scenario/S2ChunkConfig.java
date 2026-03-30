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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * Cenário 2 — Chunk otimizado: chunk=500, single-thread.
 */
@Configuration
public class S2ChunkConfig {

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${benchmark.chunk-size:500}")
    private int chunkSize;

    @Bean
    public Job s2ChunkJob(JobRepository jobRepository, Step s2ChunkStep) {
        return new JobBuilder("s2ChunkJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(s2ChunkStep)
                .build();
    }

    @Bean
    public Step s2ChunkStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            EntityManagerFactory emf,
                            TransactionItemProcessor processor) {
        return new StepBuilder("s2ChunkStep", jobRepository)
                .<Transaction, Transaction>chunk(chunkSize, transactionManager)
                .reader(s2ChunkReader(emf))
                .processor(processor)
                .writer(s2ChunkWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Transaction> s2ChunkReader(EntityManagerFactory emf) {
        return new JpaPagingItemReaderBuilder<Transaction>()
                .name("s2ChunkReader")
                .entityManagerFactory(emf)
                .queryString("SELECT t FROM Transaction t ORDER BY t.createdAt")
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    public RepositoryItemWriter<Transaction> s2ChunkWriter() {
        return new RepositoryItemWriterBuilder<Transaction>()
                .repository(transactionRepository)
                .methodName("save")
                .build();
    }
}
