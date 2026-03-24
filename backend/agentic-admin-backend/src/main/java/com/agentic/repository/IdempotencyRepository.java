package com.agentic.repository;

import com.agentic.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * IDEMPOTENCY REPOSITORY
 * 
 * Manages idempotency records to prevent duplicate request processing.
 */
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
    
    /**
     * Find existing idempotency record by key and user ID
     * Used to detect duplicate requests
     */
    Optional<IdempotencyRecord> findByIdempotencyKeyAndUserId(String idempotencyKey, String userId);
    
    /**
     * Delete records older than specified timestamp
     * Called by scheduled cleanup job (24-hour cleanup)
     */
    @Modifying
    @Query("DELETE FROM IdempotencyRecord WHERE createdAt < :before")
    void deleteOldRecords(@Param("before") LocalDateTime before);
}
