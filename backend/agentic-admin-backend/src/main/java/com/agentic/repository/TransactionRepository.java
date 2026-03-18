package com.agentic.repository;

import com.agentic.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.fromAccount.id = :accountId AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByFromAccountAndStatus(@Param("accountId") Long accountId, 
                                                  @Param("status") Transaction.TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.status = :status ORDER BY t.createdAt ASC")
    List<Transaction> findPendingTransactions(@Param("status") Transaction.TransactionStatus status);
    
    long countByStatus(Transaction.TransactionStatus status);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAccount.id = :accountId AND t.createdAt BETWEEN :startDate AND :endDate")
    long countDailyTransactionsByAccount(@Param("accountId") Long accountId, 
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}
