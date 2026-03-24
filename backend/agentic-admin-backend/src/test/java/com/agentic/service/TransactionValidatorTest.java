package com.agentic.service;

import com.agentic.config.TransferConfig;
import com.agentic.entity.Account;
import com.agentic.entity.Transaction;
import com.agentic.entity.User;
import com.agentic.exception.*;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS: TransactionValidator
 * Tests business rule enforcement
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransactionValidatorTest {
    
    @Mock
    private TransferConfig transferConfig;
    
    private TransactionValidator validator;
    private Account fromAccount;
    private Account toAccount;
    private User user;
    
    @BeforeEach
    void setUp() {
        setupTransferConfigMocks();
        validator = new TransactionValidator(transferConfig);
        
        // Setup test data
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setStatus(User.UserStatus.ACTIVE);
        
        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUser(user);
        fromAccount.setBalance(new BigDecimal("10000"));
        fromAccount.setStatus(Account.AccountStatus.ACTIVE);
        
        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUser(user);
        toAccount.setBalance(new BigDecimal("5000"));
        toAccount.setStatus(Account.AccountStatus.ACTIVE);
    }
    
    private void setupTransferConfigMocks() {
        TransferConfig.Instant instantConfig = mock(TransferConfig.Instant.class);
        TransferConfig.Daily dailyConfig = mock(TransferConfig.Daily.class);
        
        when(instantConfig.getLimit()).thenReturn(new BigDecimal("250000"));
        when(dailyConfig.getLimit()).thenReturn(new BigDecimal("5000000"));
        when(transferConfig.getInstant()).thenReturn(instantConfig);
        when(transferConfig.getDaily()).thenReturn(dailyConfig);
        when(transferConfig.getDailyTransactionCountLimit()).thenReturn(100);
    }
    
    /**
     * Test: Successful transfer validation
     */
    @Test
    void testValidateTransfer_Success() {
        // Should not throw any exception
        assertDoesNotThrow(() -> 
            validator.validateTransfer(fromAccount, toAccount, new BigDecimal("1000"), new BigDecimal("0"))
        );
    }
    
    /**
     * Test: Insufficient funds
     */
    @Test
    void testValidateTransfer_InsufficientFunds() {
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class, () ->
            validator.validateTransfer(fromAccount, toAccount, new BigDecimal("20000"), new BigDecimal("0"))
        );
        
        assertEquals("INSUFFICIENT_FUNDS", exception.getErrorCode());
    }
    
    /**
     * Test: Negative amount
     */
    @Test
    void testValidateTransfer_NegativeAmount() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validator.validateTransfer(fromAccount, toAccount, new BigDecimal("-1000"), new BigDecimal("0"))
        );
        
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
    }
    
    /**
     * Test: Daily limit exceeded
     */
    @Test
    void testValidateTransfer_DailyLimitExceeded() {
        // Increase balance to pass the balance check
        fromAccount.setBalance(new BigDecimal("1000000"));
        
        // Sum of today's transfers already at 4.9M, trying to add 200K exceeds 5M limit
        BigDecimal existingDailyTotal = new BigDecimal("4900000");
        
        TransactionLimitExceededException exception = assertThrows(
            TransactionLimitExceededException.class, () ->
            validator.validateTransfer(fromAccount, toAccount, new BigDecimal("200000"), existingDailyTotal)
        );
        
        assertEquals("LIMIT_EXCEEDED", exception.getErrorCode());
    }
    
    /**
     * Test: Same account transfer
     */
    @Test
    void testValidateTransfer_SameAccount() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validator.validateTransfer(fromAccount, fromAccount, new BigDecimal("1000"), new BigDecimal("0"))
        );
        
        assertEquals("VALIDATION_ERROR", exception.getErrorCode());
    }
    
    /**
     * Test: Transfer type determination - Instant
     */
    @Test
    void testDetermineTransferType_Instant() {
        TransactionValidator.TransferType type = validator.determineTransferType(
            new BigDecimal("100000")
        );
        
        assertEquals(TransactionValidator.TransferType.INSTANT, type);
    }
    
    /**
     * Test: Transfer type determination - Maker-Checker
     */
    @Test
    void testDetermineTransferType_MakerChecker() {
        TransactionValidator.TransferType type = validator.determineTransferType(
            new BigDecimal("300000")
        );
        
        assertEquals(TransactionValidator.TransferType.MAKER_CHECKER, type);
    }
    
    /**
     * Test: Inactive from account
     */
    @Test
    void testValidateTransfer_InactiveFromAccount() {
        fromAccount.setStatus(Account.AccountStatus.INACTIVE);
        
        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class, () ->
            validator.validateTransfer(fromAccount, toAccount, new BigDecimal("1000"), new BigDecimal("0"))
        );
        
        assertEquals("INVALID_TRANSACTION", exception.getErrorCode());
    }
    
    /**
     * Test: Inactive to account
     */
    @Test
    void testValidateTransfer_InactiveToAccount() {
        toAccount.setStatus(Account.AccountStatus.CLOSED);
        
        InvalidTransactionException exception = assertThrows(InvalidTransactionException.class, () ->
            validator.validateTransfer(fromAccount, toAccount, new BigDecimal("1000"), new BigDecimal("0"))
        );
        
        assertEquals("INVALID_TRANSACTION", exception.getErrorCode());
    }
}
