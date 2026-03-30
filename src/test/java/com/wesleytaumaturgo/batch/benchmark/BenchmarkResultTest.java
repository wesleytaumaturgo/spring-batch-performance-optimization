package com.wesleytaumaturgo.batch.benchmark;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkResultTest {

    @Test
    void should_calculate_throughput_per_second() {
        // REQ-3.EARS-1
        BenchmarkResult r = new BenchmarkResult("S1", 1000, 10_000, 64, true);
        assertThat(r.throughputPerSecond()).isEqualTo(100.0);
    }

    @Test
    void should_calculate_speedup_vs_baseline() {
        // REQ-3.EARS-2
        BenchmarkResult baseline  = new BenchmarkResult("S1", 1000, 100_000, 64, true);
        BenchmarkResult optimized = new BenchmarkResult("S7", 1000,  10_000, 64, true);

        assertThat(optimized.speedupVs(baseline)).isEqualTo(10.0);
    }

    @Test
    void should_return_speedup_one_when_compared_to_itself() {
        BenchmarkResult r = new BenchmarkResult("S1", 1000, 5000, 64, true);
        assertThat(r.speedupVs(r)).isEqualTo(1.0);
    }

    @Test
    void should_expose_elapsed_duration() {
        // REQ-3.EARS-3
        BenchmarkResult r = new BenchmarkResult("S1", 0, 3_000, 0, true);
        assertThat(r.getElapsed().toSeconds()).isEqualTo(3L);
    }

    @Test
    void should_return_success_flag() {
        assertThat(new BenchmarkResult("S1", 0, 0, 0, true).isSuccess()).isTrue();
        assertThat(new BenchmarkResult("S1", 0, 0, 0, false).isSuccess()).isFalse();
    }

    @Test
    void should_expose_heap_used_mb() {
        BenchmarkResult r = new BenchmarkResult("S1", 0, 0, 128, true);
        assertThat(r.getHeapUsedMb()).isEqualTo(128L);
    }
}
