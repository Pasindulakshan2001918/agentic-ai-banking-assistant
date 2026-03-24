package com.agentic.repository;

import com.agentic.entity.ScheduledTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SCHEDULED TRANSFER REPOSITORY
 * 
 * Manages database operations for scheduled transfers.
 */
@Repository
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {
    
    /**
     * Find all scheduled transfers for a user
     */
    List<ScheduledTransfer> findByUserId(String userId);
    
    /**
     * Find scheduled transfers ready to execute
     * Called by cron job every minute
     * 
     * Finds: PENDING transfers where executeAt <= now
     */
    @Query("SELECT st FROM ScheduledTransfer st WHERE st.status = 'PENDING' AND st.executeAt <= :now ORDER BY st.executeAt ASC")
    List<ScheduledTransfer> findDueTransfers(@Param("now") LocalDateTime now);
    
    /**
     * Find all pending transfers for a user
     */
    List<ScheduledTransfer> findByUserIdAndStatus(String userId, ScheduledTransfer.ScheduledTransferStatus status);
    
    /**
     * Find scheduled transfer by ID and validate ownership
     */
    @Query("SELECT st FROM ScheduledTransfer st WHERE st.id = :id AND st.userId = :userId")
    java.util.Optional<ScheduledTransfer> findByIdAndUserId(@Param("id") Long id, @Param("userId") String userId);
}
