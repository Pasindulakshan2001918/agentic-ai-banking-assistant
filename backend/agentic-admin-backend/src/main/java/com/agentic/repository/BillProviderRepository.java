package com.agentic.repository;

import com.agentic.entity.BillProvider;
import com.agentic.entity.BillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for bill provider data.
 * Provides access to utility bills and charges.
 */
@Repository
public interface BillProviderRepository extends JpaRepository<BillProvider, Long> {
    
    /**
     * Find all unpaid bills for a user
     */
    List<BillProvider> findByUserIdAndStatus(Long userId, BillProvider.BillStatus status);

    /**
     * Find a specific bill type for a user
     */
    Optional<BillProvider> findByUserIdAndBillTypeAndStatus(
        Long userId, BillType billType, BillProvider.BillStatus status
    );

    /**
     * Find by provider account reference (for validation)
     */
    Optional<BillProvider> findByAccountReference(String accountReference);
}
