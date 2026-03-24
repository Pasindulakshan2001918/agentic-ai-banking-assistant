# Controller Layer Refactoring Guide

## Executive Summary

The controller layer has critical security and concurrency issues that must be addressed:
- **CRITICAL**: Hardcoded user ID (returns `1L`) bypasses all authorization
- **CRITICAL**: Map-based request parsing lacks validation, type safety
- **HIGH**: OTP exposed in API responses (sent in plain text)
- **HIGH**: Exception handling bypasses GlobalExceptionHandler  
- **MEDIUM**: No pagination support (memory/performance risk)
- **MEDIUM**: Timestamp handling inconsistent (should use UTC)

**Impact**: All authorization checks are bypassed; users can act as others; OTP codes exposed; large datasets in memory.

---

## Priority 1: Critical Security Fixes

### 1.1 Fix Hardcoded User ID (MOST CRITICAL)

**Current State** (BROKEN):
```java
private Long getLoggedInUserId(Authentication auth) {
    return 1L;  // ⚠️ ALL USERS SEEN AS USER ID 1
}
```

**Fixed Implementation**:
```java
private Long getLoggedInUserId(Authentication auth) {
    if (auth == null || !auth.isAuthenticated()) {
        throw new UnauthorizedException("User not authenticated");
    }
    
    try {
        // Extract JWT from Spring Security Authentication
        Jwt jwt = (Jwt) auth.getPrincipal();
        
        // Get "sub" claim (Keycloak standard user ID claim)
        String userIdStr = jwt.getClaimAsString("sub");
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new UnauthorizedException("Missing 'sub' claim in JWT token");
        }
        
        // Convert UUID string to numeric ID for database queries
        // Production: Consider using UUID directly in database
        try {
            // Extract first 10 digits from UUID
            return Long.parseLong(userIdStr.replaceAll("[^0-9]", "").substring(0, 10));
        } catch (Exception e) {
            // Fallback: Use hash of user ID (stable across sessions)
            return Math.abs((long) userIdStr.hashCode());
        }
    } catch (ClassCastException e) {
        throw new UnauthorizedException("Invalid token format: expected JWT");
    }
}
```

**Imports Needed**:
```java
import org.springframework.security.oauth2.jwt.Jwt;
import com.agentic.exception.UnauthorizedException;
import java.time.ZoneOffset;
```

**Impact**: Every authorization check will now properly validate user ownership of accounts.

---

### 1.2 Remove OTP from API Response

**Current State** (INSECURE):
```java
String otp = otpService.generateOtp(userId, transferReference);
Map<String, Object> response = new HashMap<>();
response.put("otp", otp);  // ⚠️ OTP exposed in plain text!
```

**Fixed Implementation**:
```java
// Do NOT store OTP in response - only in backend
otpService.generateOtp(userId, transferReference);  // OTP sent via email/SMS only

Map<String, Object> response = new HashMap<>();
response.put("transferReference", transferReference);
response.put("status", "OTP_SENT");
response.put("message", "OTP sent to your registered email/phone");
response.put("expiryMinutes", 5);
response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
// NOTE: OTP NOT included in response
```

**Impact**: OTP codes are no longer exposed in API logs, network traffic, or browser history.

---

## Priority 2: Input Validation & Type Safety

### 2.1 Replace Map-Based Requests with DTOs

**Created DTOs** (already created in repo):
1. `InstantTransferBeneficiaryRequest`  
2. `RequestTransferOtpByBeneficiaryRequest`
3. `VerifyOtpAndTransferByBeneficiaryRequest`

**Before** (Type-Unsafe):
```java
@PostMapping("/transfer/by-beneficiary")
public ResponseEntity<?> instantTransferByBeneficiary(
        @Valid @RequestBody Map<String, Object> request,  // ⚠️ No type safety
        Authentication auth) {
    try {
        // Manual parsing with NumberFormatException risk
        Long fromAccountId = Long.parseLong(request.get("fromAccountId").toString());
        String beneficiaryNickname = request.get("beneficiaryNickname").toString();
        BigDecimal amount = new BigDecimal(request.get("amount").toString());  // Can throw
        // ... rest of logic
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

**After** (Type-Safe):
```java
@PostMapping("/transfer/by-beneficiary")
public ResponseEntity<?> instantTransferByBeneficiary(
        @Valid @RequestBody InstantTransferBeneficiaryRequest request,  // ✅ Validated DTO
        Authentication auth) {
    Long userId = getLoggedInUserId(auth);
    
    // Validation happens automatically
    // request.fromAccountId is guaranteed @NotNull
    // request.beneficiaryNickname is guaranteed @NotBlank  
    // request.amount is guaranteed @DecimalMin("0.01")
    
    if (!customerService.ownsAccount(request.getFromAccountId(), userId)) {
        throw new UnauthorizedException("You do not own the source account");
    }
    
    Account toAccount = beneficiaryTransferService.resolveBeneficiaryToAccount(
        userId, 
        request.getBeneficiaryNickname()
    );
    
    String referenceNumber = transactionService.instantTransfer(
        request.getFromAccountId(),
        toAccount.getId(),
        request.getAmount(),
        userId
    );
    
    Map<String, Object> response = new HashMap<>();
    response.put("message", "Transfer completed successfully");
    response.put("referenceNumber", referenceNumber);
    response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
    return ResponseEntity.ok(response);
}
```

**DTO Example**:
```java
public class InstantTransferBeneficiaryRequest {
    @NotNull private Long fromAccountId;
    @NotBlank private String beneficiaryNickname;
    @NotNull @DecimalMin("0.01") @Digits(integer=15, fraction=2) private BigDecimal amount;
    @Size(max=500) private String description;
    // ... getters/setters
}
```

**Impact**:
- ✅ Compile-time type safety
- ✅ Automatic Spring validation with @NotNull, @DecimalMin, etc.
- ✅ Better error messages
- ✅ Self-documenting API contract
- ✅ Eliminates NumberFormatException risks

---

## Priority 3: Exception Handling

### 3.1 Remove Try-Catch, Let Exceptions Propagate

**Before** (Blocks GlobalExceptionHandler):
```java
@GetMapping("/balance/{accountId}")
public ResponseEntity<?> getBalance(@PathVariable Long accountId, Authentication auth) {
    try {
        Long userId = getLoggedInUserId(auth);
        BalanceResponse balance = customerService.getBalance(accountId, userId);
        return ResponseEntity.ok(balance);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", e.getMessage()));  // ⚠️ Hides real error
    }
}
```

**After** (Centralized Handling):
```java
@GetMapping("/balance/{accountId}")
public ResponseEntity<?> getBalance(
        @PathVariable Long accountId,
        Authentication auth) {
    Long userId = getLoggedInUserId(auth);  // Throws UnauthorizedException if no auth
    BalanceResponse balance = customerService.getBalance(accountId, userId);  // Throws EntityNotFoundException
    return ResponseEntity.ok(balance);  // Exceptions propagate to GlobalExceptionHandler
}
```

**GlobalExceptionHandler will catch**:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleUnauthorized(UnauthorizedException ex) {
        return new ApiErrorResponse("UNAUTHORIZED", ex.getMessage());
    }
    
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(EntityNotFoundException ex) {
        return new ApiErrorResponse("NOT_FOUND", ex.getMessage());
    }
    
    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInsufficientFunds(InsufficientFundsException ex) {
        return new ApiErrorResponse("INSUFFICIENT_FUNDS", ex.getMessage());
    }
}
```

**Impact**:
- ✅ Consistent HTTP status codes
- ✅ Uniform error response format
- ✅ Audit logging in one place
- ✅ Cleaner controller code

---

## Priority 4: Timestamp Consistency

### 4.1 Use UTC Timestamps with ZoneOffset

**Before**:
```java
response.put("timestamp", LocalDateTime.now());  // System timezone (inconsistent)
```

**After**:
```java
import java.time.ZoneOffset;
response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));  // Always UTC
```

**Why**: Distributed systems need consistent timezone to avoid transaction timing confusion.

---

## Priority 5: Pagination Support

### 5.1 Add Pageable to Large Dataset Queries

**Before**:
```java
@GetMapping("/transactions/{accountId}")
public ResponseEntity<?> getTransactionHistory(
        @PathVariable Long accountId,
        @RequestParam(defaultValue = "0") int page,   // Manual pagination
        @RequestParam(defaultValue = "10") int size,
        Authentication auth) {
    // TODO: Implement transaction history retrieval
    // Returns empty for now, but would fetch ALL transactions in production
}
```

**After**:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@GetMapping("/transactions/{accountId}")
public ResponseEntity<?> getTransactionHistory(
        @PathVariable Long accountId,
        Pageable pageable,  // Spring automatically validates and creates PageRequest
        Authentication auth) {
    Long userId = getLoggedInUserId(auth);
    if (!customerService.ownsAccount(accountId, userId)) {
        throw new UnauthorizedException("You do not own this account");
    }
    
    // Repository returns Page<Transaction> with automatic pagination
    Page<TransactionResponse> transactions = transactionService.getTransactionHistory(
        accountId, 
        pageable
    );
    
    return ResponseEntity.ok(transactions);
}
```

**Repository Method**:
```java
Page<Transaction> findByFromAccountIdOrToAccountId(Long fromId, Long toId, Pageable pageable);
```

**Client Usage**:
```
GET /api/customer/transactions/123?page=0&size=10&sort=timestamp,desc
```

---

## Implementation Roadmap

### Phase 1: Critical Security (Do First)
- [ ] Fix getLoggedInUserId() JWT extraction
- [ ] Add UnauthorizedException import
- [ ] Remove OTP from responses in:
  - `/transfer/request-otp`
  - `/transfer/by-beneficiary/request-otp`
- [ ] Add Jwt import and ZoneOffset import

### Phase 2: DTO Validation  
- [ ] Create InstantTransferBeneficiaryRequest DTO ✅ (already created)
- [ ] Create RequestTransferOtpByBeneficiaryRequest DTO ✅ (already created)
- [ ] Create VerifyOtpAndTransferByBeneficiaryRequest DTO ✅ (already created)
- [ ] Update endpoints to use @Valid @RequestBody DTOs instead of Map

### Phase 3: Exception Handling
- [ ] Remove all try-catch blocks from controller methods
- [ ] Verify GlobalExceptionHandler handles all custom exceptions
- [ ] Test exception propagation

### Phase 4: Pagination (Lower priority)
- [ ] Add Pageable parameter to list endpoints
- [ ] Update repository queries with Spring Data's Page support
- [ ] Return Page<Response> instead of List

---

## Code Review Checklist

After implementing changes:

- [ ] **Security**:
  - [ ] getLoggedInUserId() extracts from JWT
  - [ ] OTP not in responses
  - [ ] Authorization checks on all account access
  - [ ] UserID always from auth, never hardcoded

- [ ] **Input Validation**:
  - [ ] All request bodies use @Valid @RequestBody DTOs
  - [ ] No Map<String, Object> in production endpoints
  - [ ] @NotNull, @NotBlank, @Positive on amounts
  - [ ] @Email, @Size validations where appropriate

- [ ] **Error Handling**:
  - [ ] No try-catch blocks in controller methods
  - [ ] All custom exceptions mapped in GlobalExceptionHandler
  - [ ] Correct HTTP status codes (401, 403, 404, 409, 422, 500)

- [ ] **Consistency**:
  - [ ] All timestamps use ZoneOffset.UTC
  - [ ] All responses include timestamp field
  - [ ] Response structure consistent across endpoints

- [ ] **Documentation**:
  - [ ] Endpoint comments explain security checks
  - [ ] DTO javadoc explains validation rules
  - [ ] API documentation updated with examples

---

## Testing Strategy

### Unit Tests
```java
@Test
void testGetLoggedInUserId_WithValidJwt_ReturnsUserId() {
    // Mock JWT with "sub" claim
    Jwt jwt = mock(Jwt.class);
    when(jwt.getClaimAsString("sub")).thenReturn("user-uuid-1234");
    
    JwtAuthenticationToken auth = mock(JwtAuthenticationToken.class);
    when(auth.isAuthenticated()).thenReturn(true);
    when(auth.getPrincipal()).thenReturn(jwt);
    
    Long userId = controller.getLoggedInUserId(auth);
    
    assertNotNull(userId);
    assertTrue(userId > 0);  // Should generate proper ID
}

@Test
void testGetLoggedInUserId_WithNoAuth_ThrowsException() {
    assertThrows(UnauthorizedException.class, () -> {
        controller.getLoggedInUserId(null);
    });
}

@Test
void testInstantTransferByBeneficiary_WithValidRequest_ReturnsSuccess() {
    InstantTransferBeneficiaryRequest request = new InstantTransferBeneficiaryRequest(
        1L, "Alice", new BigDecimal("100.00")
    );
    // ... test implementation
}
```

### Integration Tests
```java
@Test
void testOtpNotReturnedInResponse() {
    // Request OTP transfer
    ResponseEntity<?> response = controller.requestTransferOtp(
        new OtpRequestRequest(...), auth
    );
    
    Map<String,Object> body = (Map) response.getBody();
    assertNull(body.get("otp"));  // OTP should NOT be in response
    assertEquals("OTP_SENT", body.get("status"));
}
```

---

## Production Deployment Checklist

- [ ] Feature flag: JWT extraction working with your Keycloak setup
- [ ] All endpoints tested with actual Keycloak tokens
- [ ] OTP delivery verified (email/SMS working)
- [ ] Exception handling produces correct HTTP status codes
- [ ] Pagination tested with large datasets (1M+ transactions)
- [ ] Load test concurrent transfers to verify SERIALIZABLE isolation + pessimistic locks
- [ ] Audit logging captures all user actions
- [ ] Rate limiting configured for sensitive endpoints
- [ ] CORS configured correctly for frontend
- [ ] API documentation updated with new DTO schemas

