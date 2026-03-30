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
 * Cenário 6 — Connection pool tuning: HikariCP com configurações otimizadas.
 * O pool é configurado via application.yml (spring.datasource.hikari.*).
 * Este cenário executa o mesmo pipeline do S2 com pool ajustado.
 */
@Configuration
public class S6PoolTuningConfig {

    @Autowired
    private TransactionRepository transactionRepository;

    @Value("${benchmark.chunk-size:500}")
    private int chunkSize;

    @Bean
    public Job s6PoolTuningJob(JobRepository jobRepository, Step s6PoolTuningStep) {
        return new JobBuilder("s6PoolTuningJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(s6PoolTuningStep)
                .build();
    }

    @Bean
    public Step s6PoolTuningStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 EntityManagerFactory emf,
                                 TransactionItemProcessor processor) {
        return new StepBuilder("s6PoolTuningStep", jobRepository)
                .<Transaction, Transaction>chunk(chunkSize, transactionManager)
                .reader(s6PoolTuningReader(emf))
                .processor(processor)
                .writer(s6PoolTuningWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Transaction> s6PoolTuningReader(EntityManagerFactory emf) {
        return new JpaPagingItemReaderBuilder<Transaction>()
                .name("s6PoolTuningReader")
                .entityManagerFactory(emf)
                .queryString("SELECT t FROM Transaction t ORDER BY t.createdAt")
                .pageSize(chunkSize)
                .build();
    }

    @Bean
    public RepositoryItemWriter<Transaction> s6PoolTuningWriter() {
        return new RepositoryItemWriterBuilder<Transaction>()
                .repository(transactionRepository)
                .methodName("save")
                .build();
    }
}
