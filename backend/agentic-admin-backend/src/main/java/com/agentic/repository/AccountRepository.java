package com.agentic.repository;

import com.agentic.entity.Account;
import com.agentic.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountNumber(String accountNumber);
    
    List<Account> findByUser(User user);
    
    List<Account> findByUserId(Long userId);
    
    /**
     * Get the primary (first created) account for a user
     * Used for spending insights and analysis
     */
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId ORDER BY a.createdAt ASC")
    Optional<Account> findPrimaryByUserId(@Param("userId") Long userId);
    
    long countByUserId(Long userId);
    
    /**
     * 🔒 PESSIMISTIC LOCKING: Acquires DB-level WRITE lock on account
     * Prevents concurrent modifications during transfer
     * CRITICAL for preventing race conditions
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
