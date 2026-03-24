package com.agentic.service;

import com.agentic.entity.Account;
import com.agentic.entity.User;
import com.agentic.exception.EntityNotFoundException;
import com.agentic.exception.UnauthorizedException;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * UNIT TESTS: SecurityService
 * Tests authorization and validation logic
 */
@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private UserRepository userRepository;
    
    private SecurityService securityService;
    private User user;
    private Account account;
    
    @BeforeEach
    void setUp() {
        securityService = new SecurityService(accountRepository, userRepository);
        
        // Setup test data
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setStatus(User.UserStatus.ACTIVE);
        
        account = new Account();
        account.setId(1L);
        account.setUser(user);
        account.setStatus(Account.AccountStatus.ACTIVE);
    }
    
    /**
     * Test: Account ownership validation - Success
     */
    @Test
    void testValidateAccountOwnership_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        
        assertDoesNotThrow(() -> 
            securityService.validateAccountOwnership(1L, 1L)
        );
    }
    
    /**
     * Test: Account ownership validation - Account not found
     */
    @Test
    void testValidateAccountOwnership_AccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());
        
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
            securityService.validateAccountOwnership(99L, 1L)
        );
        
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }
    
    /**
     * Test: Account ownership validation - Unauthorized access
     */
    @Test
    void testValidateAccountOwnership_Unauthorized() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            securityService.validateAccountOwnership(1L, 99L)
        );
        
        assertEquals("UNAUTHORIZED", exception.getErrorCode());
    }
    
    /**
     * Test: User validation - Success
     */
    @Test
    void testValidateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        User result = securityService.validateUser(1L);
        
        assertEquals(user.getId(), result.getId());
        assertEquals(User.UserStatus.ACTIVE, result.getStatus());
    }
    
    /**
     * Test: User validation - User not found
     */
    @Test
    void testValidateUser_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
            securityService.validateUser(99L)
        );
        
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }
    
    /**
     * Test: User validation - Inactive user
     */
    @Test
    void testValidateUser_InactiveUser() {
        user.setStatus(User.UserStatus.SUSPENDED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            securityService.validateUser(1L)
        );
        
        assertEquals("UNAUTHORIZED", exception.getErrorCode());
    }
    
    /**
     * Test: Get account if owner - Success
     */
    @Test
    void testGetAccountIfOwner_Success() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        
        Account result = securityService.getAccountIfOwner(1L, 1L);
        
        assertEquals(account.getId(), result.getId());
    }
    
    /**
     * Test: Get account if owner - Unauthorized
     */
    @Test
    void testGetAccountIfOwner_Unauthorized() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            securityService.getAccountIfOwner(1L, 99L)
        );
        
        assertEquals("UNAUTHORIZED", exception.getErrorCode());
    }
    
    /**
     * Test: Can access account - True
     */
    @Test
    void testCanAccessAccount_True() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        
        boolean result = securityService.canAccessAccount(1L, 1L);
        
        assertTrue(result);
    }
    
    /**
     * Test: Can access account - False (wrong user)
     */
    @Test
    void testCanAccessAccount_False_WrongUser() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        
        boolean result = securityService.canAccessAccount(1L, 99L);
        
        assertFalse(result);
    }
    
    /**
     * Test: Can access account - False (account not found)
     */
    @Test
    void testCanAccessAccount_False_AccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());
        
        boolean result = securityService.canAccessAccount(99L, 1L);
        
        assertFalse(result);
    }
    
    /**
     * Test: Is account active - True
     */
    @Test
    void testIsAccountActive_True() {
        account.setStatus(Account.AccountStatus.ACTIVE);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        
        boolean result = securityService.isAccountActive(1L);
        
        assertTrue(result);
    }
    
    /**
     * Test: Is account active - False
     */
    @Test
    void testIsAccountActive_False() {
        account.setStatus(Account.AccountStatus.CLOSED);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        
        boolean result = securityService.isAccountActive(1L);
        
        assertFalse(result);
    }
    
    /**
     * Test: Is user active - True
     */
    @Test
    void testIsUserActive_True() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        boolean result = securityService.isUserActive(1L);
        
        assertTrue(result);
    }
    
    /**
     * Test: Is user active - False
     */
    @Test
    void testIsUserActive_False() {
        user.setStatus(User.UserStatus.SUSPENDED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        boolean result = securityService.isUserActive(1L);
        
        assertFalse(result);
    }
}
