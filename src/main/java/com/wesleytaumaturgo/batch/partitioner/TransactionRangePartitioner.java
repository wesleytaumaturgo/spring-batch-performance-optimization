package com.wesleytaumaturgo.batch.partitioner;

import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import com.wesleytaumaturgo.batch.domain.repository.TransactionRepository;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Divide o processamento em faixas de offset/limit para cada partição.
 * Consulta o total de registros PENDING diretamente no repositório.
 */
public class TransactionRangePartitioner implements Partitioner {

    @Autowired
    private TransactionRepository repository;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long totalCount = repository.countByStatus(TransactionStatus.PENDING);
        long pageSize = Math.max(1, (totalCount + gridSize - 1) / gridSize);

        Map<String, ExecutionContext> partitions = new HashMap<>();

        for (int i = 0; i < gridSize; i++) {
            long offset = (long) i * pageSize;
            long limit  = Math.min(pageSize, totalCount - offset);

            if (limit <= 0) break;

            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("offset", offset);
            ctx.putLong("limit", limit);

            partitions.put("partition-" + i, ctx);
        }

        return partitions;
    }
}
