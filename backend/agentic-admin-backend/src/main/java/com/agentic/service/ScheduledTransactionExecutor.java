package com.agentic.service;

import com.agentic.entity.ScheduledTransfer;
import com.agentic.repository.ScheduledTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScheduledTransactionExecutor {

    private static final Logger logger =
        LoggerFactory.getLogger(ScheduledTransactionExecutor.class);

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final ScheduledTransferService scheduledTransferService;

    public ScheduledTransactionExecutor(
            ScheduledTransferRepository scheduledTransferRepository,
            ScheduledTransferService scheduledTransferService) {
        this.scheduledTransferRepository = scheduledTransferRepository;
        this.scheduledTransferService = scheduledTransferService;
    }

    /**
     * Runs every 60 seconds.
     * Finds all PENDING scheduled transfers where executeAt <= now
     * and executes each one individually.
     * Idempotent: each transfer is marked PROCESSING before execution
     * so a crash cannot cause double-execution.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processDueTransfers() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Scheduled transfer executor running at {}", now);

        List<ScheduledTransfer> dueTransfers;
        try {
            dueTransfers = scheduledTransferRepository.findDueTransfers(now);
        } catch (Exception e) {
            logger.error("Failed to query due transfers: {}", e.getMessage());
            return;
        }

        if (dueTransfers.isEmpty()) {
            logger.debug("No scheduled transfers due at {}", now);
            return;
        }

        logger.info("Found {} scheduled transfer(s) to execute", dueTransfers.size());

        for (ScheduledTransfer transfer : dueTransfers) {
            executeOne(transfer);
        }
    }

    private void executeOne(ScheduledTransfer transfer) {
        logger.info("Executing scheduled transfer id={} amount={} from={} to={}",
            transfer.getId(),
            transfer.getAmount(),
            transfer.getFromAccount().getId(),
            transfer.getToAccount().getId());
        try {
            scheduledTransferService.executeScheduledTransfer(transfer);
            logger.info("Scheduled transfer id={} completed successfully", transfer.getId());
        } catch (Exception e) {
            logger.error("Scheduled transfer id={} failed: {}", transfer.getId(), e.getMessage());
        }
    }
}
