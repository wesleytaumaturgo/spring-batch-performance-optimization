package com.wesleytaumaturgo.batch.benchmark;

import java.time.Duration;

/**
 * Resultado imutável de um cenário de benchmark.
 * Armazena métricas de desempenho para comparação entre cenários.
 */
public class BenchmarkResult {

    private final String scenarioName;
    private final long totalRecords;
    private final long durationMs;
    private final long heapUsedMb;
    private final boolean success;

    public BenchmarkResult(String scenarioName,
                           long totalRecords,
                           long durationMs,
                           long heapUsedMb,
                           boolean success) {
        this.scenarioName = scenarioName;
        this.totalRecords = totalRecords;
        this.durationMs = durationMs;
        this.heapUsedMb = heapUsedMb;
        this.success = success;
    }

    // ── Métricas derivadas ───────────────────────────────────────────────────

    public double throughputPerSecond() {
        if (durationMs == 0) return (double) totalRecords;
        return totalRecords / (durationMs / 1000.0);
    }

    public double speedupVs(BenchmarkResult baseline) {
        if (this.durationMs == 0) return 0;
        return (double) baseline.durationMs / this.durationMs;
    }

    public Duration getElapsed() {
        return Duration.ofMillis(durationMs);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getScenarioName() { return scenarioName; }
    public long getTotalRecords() { return totalRecords; }
    public long getDurationMs() { return durationMs; }
    public long getHeapUsedMb() { return heapUsedMb; }
    public boolean isSuccess() { return success; }

    @Override
    public String toString() {
        return String.format("%-42s | %,8d reg | %5d ms | %6.0f reg/s | heap=%dMB | ok=%s",
                scenarioName, totalRecords, durationMs,
                throughputPerSecond(), heapUsedMb, success);
    }
}
