package com.agentic.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class BankingController {

    // ==========================================
    // PUBLIC ENDPOINTS (No authentication)
    // ==========================================
    
    @GetMapping("/public/health")
    public Map<String, Object> publicHealth() {
        return Map.of(
            "status", "UP",
            "message", "Banking system is operational",
            "timestamp", LocalDateTime.now().toString()
        );
    }
    
    @GetMapping("/public/info")
    public Map<String, String> publicInfo() {
        return Map.of(
            "application", "Agentic Banking System",
            "version", "1.0.0",
            "environment", "development"
        );
    }

    // ==========================================
    // SUPER_ADMIN ONLY (Full system control)
    // ==========================================
    
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/superadmin/system-config")
    public Map<String, Object> getSystemConfig(Authentication auth) {
        return Map.of(
            "message", "System configuration retrieved successfully",
            "accessedBy", auth.getName(),
            "role", "SUPER_ADMIN",
            "config", Map.of(
                "maxTransactionLimit", 1000000,
                "dailyTransactionLimit", 5000000,
                "systemStatus", "Active"
            ),
            "permissions", List.of(
                "Manage all users",
                "Configure system settings",
                "Create/Delete roles",
                "Access all modules"
            )
        );
    }
    
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/superadmin/roles")
    public Map<String, String> createRole(@RequestBody Map<String, String> roleData, 
                                           Authentication auth) {
        return Map.of(
            "message", "Role created successfully",
            "roleName", roleData.getOrDefault("roleName", "NEW_ROLE"),
            "createdBy", auth.getName(),
            "timestamp", LocalDateTime.now().toString()
        );
    }
    
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/superadmin/users/{userId}")
    public Map<String, String> deleteUser(@PathVariable Long userId, 
                                           Authentication auth) {
        return Map.of(
            "message", "User deleted successfully",
            "userId", userId.toString(),
            "deletedBy", auth.getName()
        );
    }

    // ==========================================
    // ADMIN (User & settings management)
    // Can be accessed by ADMIN or SUPER_ADMIN
    // ==========================================
    
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/admin/users")
    public Map<String, Object> getAllUsers(Authentication auth) {
        return Map.of(
            "message", "User list retrieved successfully",
            "accessedBy", auth.getName(),
            "totalUsers", 25,
            "users", List.of(
                Map.of("id", 1, "username", "creator1", "email", "creator1@bank.com", "role", "CREATOR", "status", "Active"),
                Map.of("id", 2, "username", "approver1", "email", "approver1@bank.com", "role", "APPROVER", "status", "Active"),
                Map.of("id", 3, "username", "viewer1", "email", "viewer1@bank.com", "role", "VIEWER", "status", "Active")
            )
        );
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/admin/users")
    public Map<String, Object> createUser(@RequestBody Map<String, String> userData, 
                                           Authentication auth) {
        return Map.of(
            "message", "User created successfully",
            "createdBy", auth.getName(),
            "userData", Map.of(
                "username", userData.getOrDefault("username", "newuser"),
                "email", userData.getOrDefault("email", "newuser@bank.com"),
                "role", userData.getOrDefault("role", "VIEWER"),
                "status", "Active"
            ),
            "timestamp", LocalDateTime.now().toString()
        );
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PutMapping("/admin/users/{userId}")
    public Map<String, String> updateUser(@PathVariable Long userId,
                                           @RequestBody Map<String, String> userData,
                                           Authentication auth) {
        return Map.of(
            "message", "User updated successfully",
            "userId", userId.toString(),
            "updatedBy", auth.getName(),
            "newEmail", userData.getOrDefault("email", "updated@bank.com")
        );
    }
    
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/admin/settings")
    public Map<String, Object> getSettings(Authentication auth) {
        return Map.of(
            "message", "System settings retrieved",
            "accessedBy", auth.getName(),
            "settings", Map.of(
                "transactionApprovalRequired", true,
                "maxDailyTransactions", 100,
                "sessionTimeout", 30,
                "twoFactorEnabled", false
            )
        );
    }

    // ==========================================
    // CREATOR (Maker in Maker-Checker)
    // Can create transactions but cannot approve
    // ==========================================
    
    @PreAuthorize("hasRole('CREATOR')")
    @PostMapping("/creator/transaction")
    public Map<String, Object> createTransaction(@RequestBody Map<String, Object> txnData,
                                                   Authentication auth) {
        String txnId = "TXN" + System.currentTimeMillis();
        
        return Map.of(
            "message", "Transaction created successfully. Waiting for approval.",
            "transactionId", txnId,
            "status", "PENDING_APPROVAL",
            "createdBy", auth.getName(),
            "transactionDetails", Map.of(
                "amount", txnData.getOrDefault("amount", 0),
                "recipient", txnData.getOrDefault("recipient", "Unknown"),
                "description", txnData.getOrDefault("description", "Bank transfer"),
                "createdAt", LocalDateTime.now().toString()
            )
        );
    }
    
    @PreAuthorize("hasRole('CREATOR')")
    @GetMapping("/creator/my-transactions")
    public Map<String, Object> getMyTransactions(Authentication auth) {
        return Map.of(
            "message", "Your transactions retrieved",
            "username", auth.getName(),
            "totalTransactions", 8,
            "transactions", List.of(
                Map.of("id", "TXN001", "amount", 5000, "recipient", "ACC-123", "status", "PENDING", "createdAt", "2025-01-20 10:30"),
                Map.of("id", "TXN002", "amount", 3000, "recipient", "ACC-456", "status", "APPROVED", "createdAt", "2025-01-20 09:15"),
                Map.of("id", "TXN003", "amount", 7500, "recipient", "ACC-789", "status", "REJECTED", "createdAt", "2025-01-19 14:20")
            )
        );
    }
    
    @PreAuthorize("hasRole('CREATOR')")
    @GetMapping("/creator/statistics")
    public Map<String, Object> getCreatorStats(Authentication auth) {
        return Map.of(
            "username", auth.getName(),
            "statistics", Map.of(
                "totalCreated", 25,
                "pending", 5,
                "approved", 18,
                "rejected", 2,
                "totalAmount", 125000
            )
        );
    }

    // ==========================================
    // APPROVER (Checker in Maker-Checker)
    // Can approve/reject transactions (not own)
    // ==========================================
    
    @PreAuthorize("hasRole('APPROVER')")
    @PostMapping("/approver/transaction/{txnId}/approve")
    public Map<String, Object> approveTransaction(@PathVariable String txnId,
                                                    Authentication auth) {
        // Banking compliance: In real system, check if approver != creator
        // if (transaction.getCreatedBy().equals(auth.getName())) {
        //     throw new AccessDeniedException("Cannot approve your own transaction");
        // }
        
        return Map.of(
            "message", "Transaction approved successfully",
            "transactionId", txnId,
            "status", "APPROVED",
            "approvedBy", auth.getName(),
            "approvedAt", LocalDateTime.now().toString(),
            "nextStep", "Transaction will be processed within 24 hours"
        );
    }
    
    @PreAuthorize("hasRole('APPROVER')")
    @PostMapping("/approver/transaction/{txnId}/reject")
    public Map<String, Object> rejectTransaction(@PathVariable String txnId,
                                                   @RequestBody Map<String, String> rejectionData,
                                                   Authentication auth) {
        return Map.of(
            "message", "Transaction rejected",
            "transactionId", txnId,
            "status", "REJECTED",
            "rejectedBy", auth.getName(),
            "rejectedAt", LocalDateTime.now().toString(),
            "reason", rejectionData.getOrDefault("reason", "Not specified")
        );
    }
    
    @PreAuthorize("hasRole('APPROVER')")
    @GetMapping("/approver/pending-transactions")
    public Map<String, Object> getPendingTransactions(Authentication auth) {
        return Map.of(
            "message", "Pending transactions for approval",
            "accessedBy", auth.getName(),
            "pendingCount", 12,
            "transactions", List.of(
                Map.of("id", "TXN004", "amount", 10000, "creator", "creator1", "createdAt", "2025-01-20 11:00", "priority", "High"),
                Map.of("id", "TXN005", "amount", 7500, "creator", "creator1", "createdAt", "2025-01-20 10:45", "priority", "Medium"),
                Map.of("id", "TXN006", "amount", 5000, "creator", "creator2", "createdAt", "2025-01-20 10:30", "priority", "Low")
            )
        );
    }
    
    @PreAuthorize("hasRole('APPROVER')")
    @GetMapping("/approver/statistics")
    public Map<String, Object> getApproverStats(Authentication auth) {
        return Map.of(
            "username", auth.getName(),
            "statistics", Map.of(
                "totalReviewed", 45,
                "approved", 38,
                "rejected", 7,
                "pending", 12,
                "avgProcessingTime", "15 minutes"
            )
        );
    }

    // ==========================================
    // VIEWER (Auditor / Read-only access)
    // Can only view reports and audit logs
    // ==========================================
    
    @PreAuthorize("hasRole('VIEWER')")
    @GetMapping("/viewer/reports")
    public Map<String, Object> viewReports(Authentication auth) {
        return Map.of(
            "message", "Reports retrieved successfully",
            "accessedBy", auth.getName(),
            "role", "VIEWER (Read-only)",
            "reports", List.of(
                Map.of(
                    "id", 1,
                    "name", "Daily Transaction Report",
                    "date", "2025-01-20",
                    "totalTransactions", 156,
                    "totalAmount", 2500000
                ),
                Map.of(
                    "id", 2,
                    "name", "User Activity Report",
                    "date", "2025-01-20",
                    "activeUsers", 42,
                    "newRegistrations", 3
                ),
                Map.of(
                    "id", 3,
                    "name", "Approval Summary",
                    "date", "2025-01-20",
                    "pendingApprovals", 12,
                    "completedApprovals", 45
                )
            )
        );
    }
    
    @PreAuthorize("hasRole('VIEWER')")
    @GetMapping("/viewer/audit-logs")
    public Map<String, Object> viewAuditLogs(Authentication auth) {
        return Map.of(
            "message", "Audit logs retrieved",
            "accessedBy", auth.getName(),
            "totalLogs", 234,
            "logs", List.of(
                Map.of("timestamp", "2025-01-20 10:30:15", "user", "creator1", "action", "CREATE_TRANSACTION", "txnId", "TXN001", "status", "Success"),
                Map.of("timestamp", "2025-01-20 11:15:22", "user", "approver1", "action", "APPROVE_TRANSACTION", "txnId", "TXN002", "status", "Success"),
                Map.of("timestamp", "2025-01-20 12:05:33", "user", "admin1", "action", "CREATE_USER", "userId", "25", "status", "Success"),
                Map.of("timestamp", "2025-01-20 13:20:45", "user", "creator1", "action", "LOGIN", "ip", "192.168.1.100", "status", "Success")
            )
        );
    }
    
    @PreAuthorize("hasRole('VIEWER')")
    @GetMapping("/viewer/transactions")
    public Map<String, Object> viewAllTransactions(Authentication auth) {
        return Map.of(
            "message", "All transactions (read-only view)",
            "accessedBy", auth.getName(),
            "note", "You have read-only access. Cannot modify transactions.",
            "transactions", List.of(
                Map.of("id", "TXN001", "creator", "creator1", "amount", 5000, "status", "PENDING", "date", "2025-01-20"),
                Map.of("id", "TXN002", "creator", "creator1", "amount", 3000, "status", "APPROVED", "date", "2025-01-20"),
                Map.of("id", "TXN003", "creator", "creator2", "amount", 7500, "status", "REJECTED", "date", "2025-01-19")
            )
        );
    }

    // ==========================================
    // COMMON ENDPOINTS (Any authenticated user)
    // ==========================================
    
    @GetMapping("/common/dashboard")
    public Map<String, Object> getDashboard(Authentication auth) {
        return Map.of(
            "message", "Welcome to your dashboard",
            "username", auth.getName(),
            "lastLogin", "2025-01-20 09:00:00",
            "notifications", 5,
            "quickStats", Map.of(
                "todayTransactions", 12,
                "pendingActions", 3,
                "systemStatus", "Operational"
            )
        );
    }
    
    @GetMapping("/common/notifications")
    public Map<String, Object> getNotifications(Authentication auth) {
        return Map.of(
            "username", auth.getName(),
            "unreadCount", 5,
            "notifications", List.of(
                Map.of("id", 1, "message", "New transaction pending approval", "time", "10 minutes ago", "type", "info"),
                Map.of("id", 2, "message", "System maintenance scheduled", "time", "1 hour ago", "type", "warning"),
                Map.of("id", 3, "message", "Your transaction TXN002 was approved", "time", "2 hours ago", "type", "success")
            )
        );
    }
}