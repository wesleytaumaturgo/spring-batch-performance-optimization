package com.wesleytaumaturgo.batch.domain.repository;

import com.wesleytaumaturgo.batch.domain.model.Transaction;
import com.wesleytaumaturgo.batch.domain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    long countByStatus(TransactionStatus status);

    List<Transaction> findByStatusOrderByCreatedAt(TransactionStatus status);

    /**
     * Reseta todas as transações para PENDING entre execuções do benchmark.
     */
    @Modifying
    @Transactional
    @Query("UPDATE Transaction t SET t.status = 'PENDING', t.processedAt = null")
    int resetAllToPending();
}
