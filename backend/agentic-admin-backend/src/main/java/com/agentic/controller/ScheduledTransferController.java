package com.agentic.controller;

import com.agentic.dto.ScheduledTransferRequest;
import com.agentic.dto.ScheduledTransferResponse;
import com.agentic.security.SecurityUtils;
import com.agentic.service.ScheduledTransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SCHEDULED TRANSFER CONTROLLER
 * 
 * REST endpoints for scheduled transfer operations:
 * - POST /api/scheduled-transfers — Schedule a future transfer
 * - GET /api/scheduled-transfers — List all scheduled transfers
 * - GET /api/scheduled-transfers/pending — List pending transfers
 * - DELETE /api/scheduled-transfers/{id} — Cancel scheduled transfer
 */
@RestController
@RequestMapping("/api/scheduled-transfers")
@PreAuthorize("isAuthenticated()")
public class ScheduledTransferController {
    
    private final ScheduledTransferService scheduledTransferService;
    private final SecurityUtils securityUtils;
    
    public ScheduledTransferController(ScheduledTransferService scheduledTransferService,
                                      SecurityUtils securityUtils) {
        this.scheduledTransferService = scheduledTransferService;
        this.securityUtils = securityUtils;
    }
    
    /**
     * POST /api/scheduled-transfers
     * Schedule a future transfer
     * 
     * @param request Contains from/to accounts, amount, and execution datetime
     * @param auth Spring Security Authentication (JWT)
     * @return Created scheduled transfer
     */
    @PostMapping
    public ResponseEntity<ScheduledTransferResponse> scheduleTransfer(
            @Valid @RequestBody ScheduledTransferRequest request,
            Authentication auth) {
        String userId = securityUtils.getUserId(auth);
        ScheduledTransferResponse response = scheduledTransferService.scheduleTransfer(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * GET /api/scheduled-transfers
     * Get all scheduled transfers for authenticated user
     */
    @GetMapping
    public ResponseEntity<List<ScheduledTransferResponse>> getScheduledTransfers(
            Authentication auth) {
        String userId = securityUtils.getUserId(auth);
        List<ScheduledTransferResponse> transfers = scheduledTransferService.getUserScheduledTransfers(userId);
        return ResponseEntity.ok(transfers);
    }
    
    /**
     * GET /api/scheduled-transfers/pending
     * Get pending (not yet executed) scheduled transfers
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ScheduledTransferResponse>> getPendingScheduledTransfers(
            Authentication auth) {
        String userId = securityUtils.getUserId(auth);
        List<ScheduledTransferResponse> transfers = scheduledTransferService.getPendingScheduledTransfers(userId);
        return ResponseEntity.ok(transfers);
    }
    
    /**
     * DELETE /api/scheduled-transfers/{id}
     * Cancel a scheduled transfer before it executes
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelScheduledTransfer(
            @PathVariable Long id,
            Authentication auth) {
        String userId = securityUtils.getUserId(auth);
        scheduledTransferService.cancelScheduledTransfer(id, userId);
        return ResponseEntity.noContent().build();
    }
}
