package com.wesleytaumaturgo.batch.benchmark;

import com.wesleytaumaturgo.batch.domain.repository.TransactionRepository;
import com.wesleytaumaturgo.batch.generator.TransactionDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquestra a execução sequencial dos 7 cenários.
 * Ativo apenas com o profile "benchmark".
 */
@Component
@Profile("benchmark")
public class BenchmarkRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkRunner.class);

    private final JobLauncher jobLauncher;
    private final TransactionDataGenerator dataGenerator;
    private final TransactionRepository repository;
    private final BenchmarkReport report;

    private final Job s1NaiveJob;
    private final Job s2ChunkJob;
    private final Job s3MultiThreadJob;
    private final Job s4PartitionJob;
    private final Job s5SqlBatchJob;
    private final Job s6PoolTuningJob;
    private final Job s7AllCombinedJob;

    @Value("${benchmark.record-count:100000}")
    private long recordCount;

    public BenchmarkRunner(JobLauncher jobLauncher,
                           TransactionDataGenerator dataGenerator,
                           TransactionRepository repository,
                           BenchmarkReport report,
                           @Qualifier("s1NaiveJob") Job s1NaiveJob,
                           @Qualifier("s2ChunkJob") Job s2ChunkJob,
                           @Qualifier("s3MultiThreadJob") Job s3MultiThreadJob,
                           @Qualifier("s4PartitionJob") Job s4PartitionJob,
                           @Qualifier("s5SqlBatchJob") Job s5SqlBatchJob,
                           @Qualifier("s6PoolTuningJob") Job s6PoolTuningJob,
                           @Qualifier("s7AllCombinedJob") Job s7AllCombinedJob) {
        this.jobLauncher = jobLauncher;
        this.dataGenerator = dataGenerator;
        this.repository = repository;
        this.report = report;
        this.s1NaiveJob = s1NaiveJob;
        this.s2ChunkJob = s2ChunkJob;
        this.s3MultiThreadJob = s3MultiThreadJob;
        this.s4PartitionJob = s4PartitionJob;
        this.s5SqlBatchJob = s5SqlBatchJob;
        this.s6PoolTuningJob = s6PoolTuningJob;
        this.s7AllCombinedJob = s7AllCombinedJob;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("=".repeat(70));
        log.info("BENCHMARK — {} registros", recordCount);
        log.info("=".repeat(70));

        dataGenerator.generate(recordCount);

        List<BenchmarkResult> results = new ArrayList<>();

        results.add(runScenario("S1 — Naive (chunk=1, single-thread)", s1NaiveJob));
        results.add(runScenario("S2 — Chunk otimizado (chunk=500)", s2ChunkJob));
        results.add(runScenario("S3 — Multi-thread (8 threads)", s3MultiThreadJob));
        results.add(runScenario("S4 — Partitioning (8 partições)", s4PartitionJob));
        results.add(runScenario("S5 — SQL batch inserts (JDBC)", s5SqlBatchJob));
        results.add(runScenario("S6 — Connection pool tuning (HikariCP)", s6PoolTuningJob));
        results.add(runScenario("S7 — All combined", s7AllCombinedJob));

        String markdown = report.generate(results);

        try {
            report.saveCsv(results, Path.of("benchmark-results.csv"));
        } catch (Exception e) {
            log.warn("Não foi possível salvar CSV: {}", e.getMessage());
        }
    }

    private BenchmarkResult runScenario(String name, Job job) throws Exception {
        log.info("Iniciando: {}", name);
        repository.resetAllToPending();

        long heapBefore = usedHeapMb();
        long start = System.currentTimeMillis();

        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        boolean success = true;
        try {
            var execution = jobLauncher.run(job, params);
            success = execution.getStatus().isUnsuccessful() == false;
        } catch (Exception e) {
            log.error("Cenário {} falhou: {}", name, e.getMessage());
            success = false;
        }

        long durationMs = System.currentTimeMillis() - start;
        long heapUsed = Math.abs(usedHeapMb() - heapBefore);
        long processed = repository.countByStatus(
                com.wesleytaumaturgo.batch.domain.model.TransactionStatus.PROCESSED);

        BenchmarkResult result = new BenchmarkResult(name, processed, durationMs, heapUsed, success);
        log.info(result.toString());
        return result;
    }

    private long usedHeapMb() {
        MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
        return bean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }
}
