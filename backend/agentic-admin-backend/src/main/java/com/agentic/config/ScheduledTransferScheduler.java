package com.agentic.config;

import com.agentic.entity.ScheduledTransfer;
import com.agentic.service.ScheduledTransferService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SCHEDULED TRANSFER SCHEDULER
 * 
 * Cron job that runs every 60 seconds to execute scheduled transfers.
 * 
 * Flow:
 * 1. Find all PENDING transfers where executeAt <= now
 * 2. For each transfer:
 *    a. Execute transfer (debit + credit)
 *    b. Mark COMPLETED or FAILED
 *    c. Log audit event
 * 3. Continue
 * 
 * ✅ IDEMPOTENT:
 * - If system crashes mid-transfer, status is already updated
 * - Won't re-process same transfer on next run
 * 
 * ⚠️ IMPORTANT:
 * - @EnableScheduling must be on main application class
 * - Database must have index on (status, executeAt)
 * - All operations wrapped in @Transactional
 */
@Component
public class ScheduledTransferScheduler {
    
    private final ScheduledTransferService scheduledTransferService;
    
    public ScheduledTransferScheduler(ScheduledTransferService scheduledTransferService) {
        this.scheduledTransferService = scheduledTransferService;
    }
    
    /**
     * Execute scheduled transfers every 60 seconds (1 minute)
     * Adjust fixedRate if needed (value in milliseconds)
     */
    @Scheduled(fixedRate = 60000)
    public void processScheduledTransfers() {
        try {
            // Find all due transfers
            List<ScheduledTransfer> dueTransfers = scheduledTransferService.findDueTransfers();
            
            if (dueTransfers.isEmpty()) {
                return; // No transfers to process
            }
            
            System.out.println("🕐 Scheduled Transfer Scheduler: Processing " + dueTransfers.size() + " due transfer(s)...");
            
            // Execute each transfer
            for (ScheduledTransfer st : dueTransfers) {
                try {
                    System.out.println("  ⏳ Executing scheduled transfer ID " + st.getId() + 
                                       ": " + st.getAmount() + " from account " + 
                                       st.getFromAccount().getId() + " to " + 
                                       st.getToAccount().getId());
                    
                    scheduledTransferService.executeScheduledTransfer(st);
                    
                    System.out.println("  ✅ Successfully executed scheduled transfer ID " + st.getId());
                    
                } catch (Exception e) {
                    System.out.println("  ❌ Failed to execute scheduled transfer ID " + st.getId() + 
                                       ": " + e.getMessage());
                    // Continue with next transfer even if one fails
                }
            }
            
            System.out.println("✅ Scheduled Transfer Scheduler: Finished processing");
            
        } catch (Exception e) {
            System.out.println("❌ Scheduled Transfer Scheduler FAILED: " + e.getMessage());
            e.printStackTrace();
            // Don't rethrow - scheduler should continue running
        }
    }
}
