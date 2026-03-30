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
class EdgeCaseScenarioTest extends BatchScenarioBaseTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired @Qualifier("s1NaiveJob") private Job s1Job;
    @Autowired @Qualifier("s2ChunkJob") private Job s2Job;
    @Autowired @Qualifier("s7AllCombinedJob") private Job s7Job;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void should_complete_when_there_are_zero_pending_records_s1() throws Exception {
        // REQ-13.EARS-1 — sem dados
        var exec = jobLauncher.run(s1Job, uniqueParams());
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(countProcessed()).isZero();
    }

    @Test
    void should_complete_when_there_are_zero_pending_records_s2() throws Exception {
        // REQ-13.EARS-2
        var exec = jobLauncher.run(s2Job, uniqueParams());
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void should_process_exactly_one_record() throws Exception {
        // REQ-13.EARS-3
        insertPendingTransactions(1);

        var exec = jobLauncher.run(s2Job, uniqueParams());

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(countProcessed()).isEqualTo(1);
    }

    @Test
    void should_process_one_record_with_combined_scenario() throws Exception {
        // REQ-13.EARS-4
        insertPendingTransactions(1);

        var exec = jobLauncher.run(s7Job, uniqueParams());

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(repository.count()).isEqualTo(1);
    }

    private JobParameters uniqueParams() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
