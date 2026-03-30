package com.wesleytaumaturgo.batch.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkReportTest {

    private final BenchmarkReport report = new BenchmarkReport();

    @Test
    void should_generate_markdown_with_header_and_rows() {
        // REQ-4.EARS-1
        List<BenchmarkResult> results = List.of(
                new BenchmarkResult("S1 — Naive", 1000, 60_000, 64, true),
                new BenchmarkResult("S7 — All combined", 1000, 3_000, 128, true)
        );

        String md = report.buildMarkdown(results);

        assertThat(md).contains("## Resultados do Benchmark");
        assertThat(md).contains("S1 — Naive");
        assertThat(md).contains("S7 — All combined");
        assertThat(md).contains("Speedup");
    }

    @Test
    void should_generate_csv_with_header_and_data() {
        // REQ-4.EARS-2
        List<BenchmarkResult> results = List.of(
                new BenchmarkResult("S1 — Naive", 1000, 60_000, 64, true),
                new BenchmarkResult("S7 — All combined", 1000, 3_000, 128, true)
        );

        String csv = report.buildCsv(results);

        assertThat(csv).startsWith("scenario,records,duration_ms");
        assertThat(csv).contains("S1 — Naive");
        assertThat(csv).contains("1000");
    }

    @Test
    void should_return_empty_string_for_empty_results() {
        assertThat(report.buildMarkdown(List.of())).isEmpty();
        assertThat(report.buildCsv(List.of())).isEmpty();
    }

    @Test
    void should_escape_csv_field_with_comma() {
        List<BenchmarkResult> results = List.of(
                new BenchmarkResult("S1, Naive", 100, 1000, 64, true)
        );

        String csv = report.buildCsv(results);

        assertThat(csv).contains("\"S1, Naive\"");
    }

    @Test
    void should_escape_csv_field_with_double_quote() {
        List<BenchmarkResult> results = List.of(
                new BenchmarkResult("S1 \"Naive\"", 100, 1000, 64, true)
        );

        String csv = report.buildCsv(results);

        assertThat(csv).contains("\"S1 \"\"Naive\"\"\"");
    }

    @Test
    void should_not_wrap_plain_field_in_quotes() {
        List<BenchmarkResult> results = List.of(
                new BenchmarkResult("S1Naive", 100, 1000, 64, true)
        );

        String csv = report.buildCsv(results);

        assertThat(csv).contains("S1Naive,");
        assertThat(csv).doesNotContain("\"S1Naive\"");
    }

    @Test
    void should_return_empty_string_for_null_results() {
        assertThat(report.buildCsv(null)).isEmpty();
        assertThat(report.generate(null)).isEmpty();
    }
}
