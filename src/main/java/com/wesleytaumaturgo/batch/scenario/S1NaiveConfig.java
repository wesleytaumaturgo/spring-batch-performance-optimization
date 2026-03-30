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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * Cenário 1 — Naive: chunk=1, single-thread.
 * Linha de base para todos os demais cenários.
 */
@Configuration
public class S1NaiveConfig {

    @Autowired
    private TransactionRepository transactionRepository;

    @Bean
    public Job s1NaiveJob(JobRepository jobRepository, Step s1NaiveStep) {
        return new JobBuilder("s1NaiveJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(s1NaiveStep)
                .build();
    }

    @Bean
    public Step s1NaiveStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            EntityManagerFactory emf,
                            TransactionItemProcessor processor) {
        return new StepBuilder("s1NaiveStep", jobRepository)
                .<Transaction, Transaction>chunk(1, transactionManager)
                .reader(s1NaiveReader(emf))
                .processor(processor)
                .writer(s1NaiveWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Transaction> s1NaiveReader(EntityManagerFactory emf) {
        return new JpaPagingItemReaderBuilder<Transaction>()
                .name("s1NaiveReader")
                .entityManagerFactory(emf)
                .queryString("SELECT t FROM Transaction t ORDER BY t.createdAt")
                .pageSize(1)
                .build();
    }

    @Bean
    public RepositoryItemWriter<Transaction> s1NaiveWriter() {
        return new RepositoryItemWriterBuilder<Transaction>()
                .repository(transactionRepository)
                .methodName("save")
                .build();
    }
}
