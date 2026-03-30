package com.wesleytaumaturgo.batch.scenario;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ScenarioNameTest {

    @Autowired @Qualifier("s1NaiveJob")        private Job s1;
    @Autowired @Qualifier("s2ChunkJob")         private Job s2;
    @Autowired @Qualifier("s3MultiThreadJob")   private Job s3;
    @Autowired @Qualifier("s4PartitionJob")     private Job s4;
    @Autowired @Qualifier("s5SqlBatchJob")      private Job s5;
    @Autowired @Qualifier("s6PoolTuningJob")    private Job s6;
    @Autowired @Qualifier("s7AllCombinedJob")   private Job s7;

    @Test
    void should_load_all_7_scenario_jobs() {
        assertThat(s1.getName()).isEqualTo("s1NaiveJob");
        assertThat(s2.getName()).isEqualTo("s2ChunkJob");
        assertThat(s3.getName()).isEqualTo("s3MultiThreadJob");
        assertThat(s4.getName()).isEqualTo("s4PartitionJob");
        assertThat(s5.getName()).isEqualTo("s5SqlBatchJob");
        assertThat(s6.getName()).isEqualTo("s6PoolTuningJob");
        assertThat(s7.getName()).isEqualTo("s7AllCombinedJob");
    }
}
