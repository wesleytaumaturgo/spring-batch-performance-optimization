package com.wesleytaumaturgo.batch.scenario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class S2ChunkScenarioTest extends BatchScenarioBaseTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("s2ChunkJob")
    private Job job;

    @BeforeEach
    void setup() {
        insertPendingTransactions(100);
    }

    @Test
    void should_complete_with_chunk_500() throws Exception {
        // REQ-7.EARS-1
        var exec = jobLauncher.run(job, uniqueParams());
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void should_process_all_100_records() throws Exception {
        // REQ-7.EARS-2
        jobLauncher.run(job, uniqueParams());
        assertThat(countProcessed()).isEqualTo(100);
    }

    @Test
    void should_leave_zero_pending_after_completion() throws Exception {
        jobLauncher.run(job, uniqueParams());
        assertThat(countPending()).isZero();
    }

    private JobParameters uniqueParams() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
