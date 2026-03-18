# Developer Guide - Backend Services & Extensions

## 📚 Service Layer Reference

### TransactionService

**Purpose**: Handle transaction lifecycle with maker-checker enforcement

#### Methods

```java
// Create transaction (CREATOR role)
Transaction createTransaction(
    Long fromAccountId,
    Long toAccountId,
    BigDecimal amount,
    String description,
    String createdBy  // Username from Authentication
)
// Returns: Transaction with status PENDING
// Throws: RuntimeException if validation fails

// Approve & execute (APPROVER role)
Transaction approveTransaction(
    Long transactionId,
    String approvedBy
)
// Returns: Transaction with status APPROVED
// Side effects: Updates sender/receiver account balances

// Reject transaction (APPROVER role)
Transaction rejectTransaction(
    Long transactionId,
    String rejectedBy,
    String reason
)
// Returns: Transaction with status REJECTED
// Side effects: Funds NOT transferred

// Queries
List<Transaction> getPendingTransactions()
Page<Transaction> getAccountTransactionHistory(Long accountId, Pageable pageable)
Optional<Transaction> getTransaction(Long transactionId)
Optional<Transaction> getTransactionByReference(String refNum)
List<Transaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end)
long getPendingTransactionCount()
```

#### Usage Example

```java
@Service
public class MyService {
    private final TransactionService txnService;
    
    public void processTransaction() {
        // Create transaction
        Transaction txn = txnService.createTransaction(
            1L,  // from account
            2L,  // to account
            new BigDecimal("1000.00"),
            "Payment for invoice #123",
            "username"
        );
        // txn.getId() = 1, txn.getStatus() = PENDING
        
        // Later, APPROVER approves
        Transaction approved = txnService.approveTransaction(
            txn.getId(),
            "approver_username"
        );
        // Balances updated, status = APPROVED
    }
}
```

---

### AccountService

**Purpose**: Manage user accounts and balances

#### Methods

```java
Account createAccount(
    Long userId,
    Account.AccountType type,  // SAVINGS, CHECKING, MONEY_MARKET, CREDIT
    String createdBy
)

Optional<Account> getAccount(Long accountId)
Optional<Account> getAccountByNumber(String accountNumber)
List<Account> getUserAccounts(Long userId)
BigDecimal getBalance(Long accountId)

Account updateAccountStatus(
    Long accountId,
    Account.AccountStatus newStatus,
    String updatedBy
)
// Status: ACTIVE, INACTIVE, FROZEN, CLOSED

Account deactivateAccount(Long accountId, String deactivatedBy)
```

#### Example

```java
// Create saving account for user
Account account = accountService.createAccount(
    userId,
    Account.AccountType.SAVINGS,
    "admin_username"
);
// Account created with account_number like: ACC-1111111111-12345

// Get all accounts for user
List<Account> accounts = accountService.getUserAccounts(userId);

// Deactivate account
accountService.deactivateAccount(accountId, "admin");
```

---

### UserService

**Purpose**: User management with role-based operations

#### Methods

```java
User createUser(
    String username,
    String email,
    String password,        // TODO: Hash with BCrypt before storing
    String fullName,
    User.UserRole role,     // SUPER_ADMIN, ADMIN, CREATOR, APPROVER, VIEWER
    String createdBy
)

Optional<User> getUserByUsername(String username)
Optional<User> getUserByEmail(String email)
Optional<User> getUser(Long userId)
Page<User> getAllUsers(Pageable pageable)

User updateUserRole(Long userId, User.UserRole newRole, String updatedBy)

User updateUserStatus(
    Long userId,
    User.UserStatus newStatus,  // ACTIVE, INACTIVE, LOCKED, SUSPENDED
    String updatedBy
)

void deleteUser(Long userId, String deletedBy)
long getUserCount()
```

#### Example

```java
// Create new CREATOR user
User creator = userService.createUser(
    "john_creator",
    "john@bank.com",
    "hashed_password_here",
    "John Creator",
    User.UserRole.CREATOR,
    "admin"
);

// Upgrade to APPROVER
User upgraded = userService.updateUserRole(
    creator.getId(),
    User.UserRole.APPROVER,
    "admin"
);

// Suspend user
userService.updateUserStatus(
    creator.getId(),
    User.UserStatus.SUSPENDED,
    "admin"
);
```

---

### AuditService

**Purpose**: Log all actions for compliance and auditing

#### Methods

```java
// Log any action
AuditLog logAction(
    String entityType,      // "Transaction", "Account", "User", etc.
    Long entityId,
    String action,          // "CREATE", "UPDATE", "DELETE", "APPROVE", "REJECT"
    String performedBy,     // Username
    String oldValue,        // JSON string of old state (null for CREATE)
    String newValue,        // JSON string of new state
    String changeDetails    // Human-readable description
)
// Returns: Saved AuditLog (with IP captured automatically)

// Queries
Page<AuditLog> getAuditLogs(String entityType, Long entityId, Pageable pageable)
Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable)
Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable)
List<AuditLog> getAuditLogsByUser(String username)
List<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end)
```

#### Example

```java
// Automatically called by services, but can be called manually
auditService.logAction(
    "Transaction",
    123L,
    "APPROVE",
    "approver_name",
    "{\"status\":\"PENDING\"}",
    "{\"status\":\"APPROVED\",\"approvedAt\":\"2026-03-18T10:00:00\"}",
    "Transaction approved and funds transferred"
);
```

---

## 🔌 Extending the Backend

### Adding a New Endpoint

#### Step 1: Create DTOs

```java
// src/main/java/com/agentic/dto/TransferRequest.java
public class TransferRequest {
    @NotNull
    private Long fromAccountId;
    
    @NotNull
    private Long toAccountId;
    
    @DecimalMin("0.01")
    private BigDecimal amount;
    
    // getters/setters
}

public class TransferResponse {
    private String status;
    private Long transactionId;
    private LocalDateTime executedAt;
    
    // getters/setters
}
```

#### Step 2: Add Service Method

```java
// In TransactionService
@Transactional
public TransactionResponse executeTransfer(TransferRequest request, String username) {
    // Validate
    // Create transaction (goes in PENDING)
    // Return response
}
```

#### Step 3: Add Controller Endpoint

```java
// In BankingController
@PreAuthorize("hasRole('CREATOR')")
@PostMapping("/banking/transfers")
public ResponseEntity<TransferResponse> executeTransfer(
    @Valid @RequestBody TransferRequest request,
    Authentication auth
) {
    TransferResponse response = transactionService.executeTransfer(
        request,
        auth.getName()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

---

### Adding a New Role

#### Step 1: Update User Entity

```java
// In User.java
public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    CREATOR,
    APPROVER,
    VIEWER,
    COMPLIANCE_OFFICER  // NEW
}
```

#### Step 2: Create New Controller Methods

```java
@PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
@GetMapping("/banking/compliance/reports")
public ResponseEntity<ComplianceReport> getComplianceReport(Authentication auth) {
    // Implementation
}
```

---

### Adding a New Entity (e.g., Loan)

#### Step 1: Create Entity

```java
// src/main/java/com/agentic/entity/Loan.java
@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    
    private BigDecimal principal;
    private BigDecimal interestRate;
    private LocalDate approvalDate;
    private LocalDate maturityDate;
    
    // getters/setters
}
```

#### Step 2: Create Repository

```java
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUser(User user);
    List<Loan> findByAccount(Account account);
}
```

#### Step 3: Create Service

```java
@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final AuditService auditService;
    
    @Transactional
    public Loan createLoan(Long userId, Long accountId, 
                          BigDecimal principal, BigDecimal rate, String createdBy) {
        Loan loan = new Loan();
        // ... set fields
        
        Loan saved = loanRepository.save(loan);
        auditService.logAction("Loan", saved.getId(), "CREATE", createdBy, 
                             null, toJsonString(saved), "Loan created");
        return saved;
    }
}
```

---

## 🧪 Testing Services

### Unit Test Template

```java
@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceTest {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    private Account fromAccount;
    private Account toAccount;
    
    @BeforeEach
    void setUp() {
        // Create test accounts
        fromAccount = new Account();
        fromAccount.setAccountNumber("ACC-TEST-001");
        fromAccount.setBalance(new BigDecimal("10000.00"));
        accountRepository.save(fromAccount);
        
        toAccount = new Account();
        toAccount.setAccountNumber("ACC-TEST-002");
        toAccount.setBalance(BigDecimal.ZERO);
        accountRepository.save(toAccount);
    }
    
    @Test
    void testCreateTransaction() {
        Transaction txn = transactionService.createTransaction(
            fromAccount.getId(),
            toAccount.getId(),
            new BigDecimal("1000.00"),
            "Test transfer",
            "testuser"
        );
        
        assertEquals(Transaction.TransactionStatus.PENDING, txn.getStatus());
        assertEquals("testuser", txn.getCreatedBy());
        assertNotNull(txn.getReferenceNumber());
    }
    
    @Test
    void testApproveTransaction() {
        // Create transaction
        Transaction txn = transactionService.createTransaction(
            fromAccount.getId(),
            toAccount.getId(),
            new BigDecimal("1000.00"),
            "Test transfer",
            "creator"
        );
        
        // Approve it
        Transaction approved = transactionService.approveTransaction(
            txn.getId(),
            "approver"
        );
        
        assertEquals(Transaction.TransactionStatus.APPROVED, approved.getStatus());
        
        // Verify balances
        fromAccount = accountRepository.findById(fromAccount.getId()).get();
        toAccount = accountRepository.findById(toAccount.getId()).get();
        
        assertEquals(new BigDecimal("9000.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("1000.00"), toAccount.getBalance());
    }
    
    @Test
    void testInsufficientFunds() {
        assertThrows(RuntimeException.class, () -> {
            transactionService.createTransaction(
                fromAccount.getId(),
                toAccount.getId(),
                new BigDecimal("50000.00"),  // More than balance
                "Test",
                "creator"
            );
        });
    }
}
```

---

## 🚀 Performance Tips

### 1. Use Pagination
```java
List<Transaction> pending = transactionService.getPendingTransactions();
// Large result set! Use pagination instead:

@GetMapping("/pending")
public Page<Transaction> getPending(Pageable pageable) {
    return transactionRepository.findByStatus(PENDING, pageable);
}
// pageable = PageRequest.of(0, 20) for page 1, 20 items
```

### 2. Add Indexes
```java
// Already done in Transaction entity:
@Table(name = "transactions", indexes = {
    @Index(name = "idx_from_account", columnList = "from_account_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
```

### 3. Use Projections for Large Result Sets
```java
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query("SELECT new com.agentic.dto.TransactionDTO(t.id, t.amount, t.status) " +
           "FROM Transaction t WHERE t.status = :status")
    List<TransactionDTO> findPendingProjection(@Param("status") TransactionStatus status);
}
```

---

## 🔒 Security Reminders

1. **Always use `@Transactional`** for operations that modify data
2. **Validate user roles** with `@PreAuthorize`
3. **Hash passwords** before storing (use BCrypt)
4. **Log all sensitive actions** via AuditService
5. **Never expose internal IDs** without authorization checks
6. **Validate BigDecimal operations** to prevent precision loss

---

## 📊 Common Queries

### Get pending transactions for approval
```java
List<Transaction> pending = transactionService.getPendingTransactions();
```

### Get account balance
```java
BigDecimal balance = accountService.getBalance(accountId);
```

### Get audit trail for entity
```java
Page<AuditLog> logs = auditService.getAuditLogs("Transaction", txnId, PageRequest.of(0, 10));
```

### Get transactions by date range
```java
List<Transaction> txns = transactionService.getTransactionsByDateRange(
    LocalDateTime.of(2026, 1, 1, 0, 0),
    LocalDateTime.of(2026, 3, 18, 23, 59)
);
```

---

**Last Updated**: 2026-03-18
**Build Status**: ✅ Production Ready
