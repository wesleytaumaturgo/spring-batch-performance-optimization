package com.wesleytaumaturgo.batch.processor;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Processor compartilhado entre todos os cenários.
 * Retorna null para itens que não estão PENDING (Spring Batch os ignora).
 * Isso evita o problema de "page shift" no JpaPagingItemReader — o reader
 * lê TODOS os registros, e o processor decide quais processar.
 */
@Component
public class TransactionItemProcessor implements ItemProcessor<Transaction, Transaction> {

    private static final Logger log = LoggerFactory.getLogger(TransactionItemProcessor.class);

    @Override
    public Transaction process(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            return null; // filtra registros já processados (não-PENDING)
        }
        transaction.markProcessed(LocalDateTime.now());
        log.debug("Processando transação {}", transaction.getId());
        return transaction;
    }
}
