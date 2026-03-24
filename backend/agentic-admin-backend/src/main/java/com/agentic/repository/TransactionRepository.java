package com.agentic.repository;

import com.agentic.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    
    /**
     * 🔒 DB-SIDE DAILY TOTAL CALCULATION
     * Prevents concurrency issues: always fetches latest from DB
     * Only sums APPROVED transactions for the given date
     * Returns 0 if no transactions exist
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.fromAccount.id = :accountId " +
           "AND t.status = 'APPROVED' " +
           "AND CAST(t.createdAt AS DATE) = :date")
    BigDecimal sumDailyTransfers(@Param("accountId") Long accountId, @Param("date") LocalDate date);
    
    /**
     * Check idempotency: transaction with this key already exists
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.idempotencyKey = :key")
    boolean existsByIdempotencyKey(@Param("key") String key);

    /**
     * All transactions for an account in a date range (for aggregation)
     * Used for spending insights and analysis
     */
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.fromAccount.id = :accountId
        AND t.createdAt BETWEEN :start AND :end
        AND t.status = 'COMPLETED'
        ORDER BY t.amount DESC
    """)
    List<Transaction> findSpendingByAccountAndPeriod(
        @Param("accountId") Long accountId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * Sum per category for a given month
     * Returns Object[] with [category, sum] pairs
     */
    @Query("""
        SELECT t.category, SUM(t.amount)
        FROM Transaction t
        WHERE t.fromAccount.id = :accountId
        AND t.createdAt BETWEEN :start AND :end
        AND t.status = 'COMPLETED'
        AND t.category IS NOT NULL
        GROUP BY t.category
    """)
    List<Object[]> sumByCategory(
        @Param("accountId") Long accountId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}
