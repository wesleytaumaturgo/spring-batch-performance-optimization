package com.wesleytaumaturgo.batch.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Gera relatório comparativo em formato Markdown e CSV.
 */
@Component
public class BenchmarkReport {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkReport.class);

    /**
     * Exibe o relatório no log e retorna a tabela Markdown.
     */
    public String generate(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) {
            log.warn("Nenhum resultado para exibir.");
            return "";
        }

        String markdown = buildMarkdown(results);
        log.info("\n{}", markdown);
        return markdown;
    }

    /**
     * Salva o relatório em formato CSV no caminho especificado.
     */
    public void saveCsv(List<BenchmarkResult> results, Path outputPath) throws IOException {
        String csv = buildCsv(results);
        Files.writeString(outputPath, csv);
        log.info("Relatório CSV salvo em: {}", outputPath);
    }

    // ── Formatação Markdown ──────────────────────────────────────────────────

    public String buildMarkdown(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) return "";
        BenchmarkResult baseline = results.get(0);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("## Resultados do Benchmark");
        pw.println();
        pw.printf("| # | Cenário | Tempo (ms) | Throughput (reg/s) | Speedup | Heap (MB) |%n");
        pw.printf("|:-:|---------|:----------:|:------------------:|:-------:|:---------:|%n");

        for (int i = 0; i < results.size(); i++) {
            BenchmarkResult r = results.get(i);
            pw.printf("| %d | %s | %,d | %.0f | %.1fx | %d |%n",
                    i + 1,
                    r.getScenarioName(),
                    r.getDurationMs(),
                    r.throughputPerSecond(),
                    r.speedupVs(baseline),
                    r.getHeapUsedMb());
        }

        return sw.toString();
    }

    // ── Formatação CSV ───────────────────────────────────────────────────────

    public String buildCsv(List<BenchmarkResult> results) {
        if (results == null || results.isEmpty()) return "";

        BenchmarkResult baseline = results.get(0);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("scenario,records,duration_ms,throughput_per_sec,speedup,heap_mb,success");

        for (BenchmarkResult r : results) {
            pw.printf("%s,%d,%d,%.2f,%.2f,%d,%s%n",
                    csvEscape(r.getScenarioName()),
                    r.getTotalRecords(),
                    r.getDurationMs(),
                    r.throughputPerSecond(),
                    r.speedupVs(baseline),
                    r.getHeapUsedMb(),
                    r.isSuccess());
        }

        return sw.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
