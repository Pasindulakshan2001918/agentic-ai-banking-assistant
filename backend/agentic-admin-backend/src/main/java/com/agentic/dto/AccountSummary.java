package com.agentic.dto;

import com.agentic.entity.Account;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ACCOUNT SUMMARY RESPONSE DTO
 * 
 * Shows user's total balance across all accounts
 * and breakdown of individual accounts.
 * 
 * Used by AI to answer: "How much money do I have?"
 */
public class AccountSummary {
    
    private BigDecimal totalBalance;
    private List<AccountInfo> accounts;
    
    public AccountSummary() {}
    
    public AccountSummary(BigDecimal totalBalance, List<AccountInfo> accounts) {
        this.totalBalance = totalBalance;
        this.accounts = accounts;
    }
    
    /**
     * Convert list of Account entities to summary
     */
    public static AccountSummary fromEntities(List<Account> accounts) {
        BigDecimal total = accounts.stream()
            .map(Account::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<AccountInfo> accountInfos = accounts.stream()
            .map(AccountInfo::fromEntity)
            .collect(Collectors.toList());
        
        return new AccountSummary(total, accountInfos);
    }
    
    // ========== GETTERS & SETTERS ==========
    
    public BigDecimal getTotalBalance() {
        return totalBalance;
    }
    
    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }
    
    public List<AccountInfo> getAccounts() {
        return accounts;
    }
    
    public void setAccounts(List<AccountInfo> accounts) {
        this.accounts = accounts;
    }
    
    // ========== INNER CLASS: AccountInfo ==========
    
    /**
     * Individual account information within summary
     */
    public static class AccountInfo {
        
        private Long id;
        private String accountNumber;
        private String type;
        private String status;
        private BigDecimal balance;
        
        public AccountInfo() {}
        
        public AccountInfo(Long id, String accountNumber, String type, String status, BigDecimal balance) {
            this.id = id;
            this.accountNumber = accountNumber;
            this.type = type;
            this.status = status;
            this.balance = balance;
        }
        
        /**
         * Convert Account entity to AccountInfo
         */
        public static AccountInfo fromEntity(Account account) {
            return new AccountInfo(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType().toString(),
                account.getStatus().toString(),
                account.getBalance()
            );
        }
        
        // Getters & Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getAccountNumber() {
            return accountNumber;
        }
        
        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public BigDecimal getBalance() {
            return balance;
        }
        
        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }
}
