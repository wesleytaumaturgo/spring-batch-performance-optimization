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
class S1NaiveScenarioTest extends BatchScenarioBaseTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("s1NaiveJob")
    private Job job;

    @BeforeEach
    void setup() {
        insertPendingTransactions(20);
    }

    @Test
    void should_complete_successfully_with_status_completed() throws Exception {
        // REQ-6.EARS-1
        var exec = jobLauncher.run(job, uniqueParams());
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void should_process_all_pending_transactions() throws Exception {
        // REQ-6.EARS-2
        jobLauncher.run(job, uniqueParams());
        assertThat(countProcessed()).isEqualTo(20);
        assertThat(countPending()).isZero();
    }

    private JobParameters uniqueParams() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
