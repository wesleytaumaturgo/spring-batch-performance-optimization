package com.wesleytaumaturgo.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Parâmetros configuráveis via application.yml / variáveis de ambiente.
 */
@Component
@ConfigurationProperties(prefix = "benchmark")
public class BatchProperties {

    /** Número total de registros a gerar/processar. */
    private long recordCount = 100_000;

    /** Tamanho do chunk para os cenários otimizados. */
    private int chunkSize = 500;

    /** Número de threads para o cenário multi-thread. */
    private int threadCount = 8;

    /** Número de partições para o cenário de partitioning. */
    private int gridSize = 8;

    public long getRecordCount() { return recordCount; }
    public void setRecordCount(long recordCount) { this.recordCount = recordCount; }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public int getGridSize() { return gridSize; }
    public void setGridSize(int gridSize) { this.gridSize = gridSize; }
}
