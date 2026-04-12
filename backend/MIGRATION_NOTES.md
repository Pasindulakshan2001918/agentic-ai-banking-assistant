# Backend Restructuring - Migration Notes

## What changed

### Deleted (not included in this clean version)
- `HelloController.java` — test scaffold, no business value
- `AiIntegrationController.java` — merged into `AiController.java`
- `AiServiceController.java` — merged into `AiController.java`
- `CustomerBankingService.java` (com.agentic.customer) — logic duplicated AccountService
- `TransactionOrchestrator.java` (com.agentic.customer.usecase) — used domain.entity, not entity
- `TransactionUseCase.java` (com.agentic.customer.usecase) — duplicate of ai.usecase version
- `ConfirmTransferDTO.java`, `TransferCompletedDTO.java`, `TransferInitiatedDTO.java`, `TransferRequestDTO.java` — from customer.dto, unused by kept code
- `Money.java`, `TransactionStatus.java` (com.agentic.domain.value) — DDD experiment, unused
- `Account.java`, `Transaction.java` (com.agentic.domain.entity) — DDD experiment, unused
- `AccountDomainService.java` (com.agentic.domain.service) — DDD experiment, unused
- `TransactionDomainService.java` (com.agentic.domain.service) — DDD experiment, unused
- `AccountEntity.java`, `TransactionEntity.java`, `AccountMapper.java`, `TransactionMapper.java` (infrastructure.persistence) — hexagonal experiment, unused
- `AccountRepositoryPort.java`, `AccountRepositoryAdapter.java`, `TransactionRepositoryPort.java`, `TransactionRepositoryAdapter.java`, `SpringDataAccountRepository.java`, `SpringDataTransactionRepository.java`, `SpringDataOneTimePasswordRepository.java`, `SpringDataPendingTransferRepository.java` (infrastructure.repository) — hexagonal experiment
- `OtpService.java` (com.agentic.infrastructure.config) — duplicate, kept the one in service
- `SecurityUtils.java` (com.agentic.infrastructure.security) — duplicate, kept the one in security
- `OneTimePasswordEntity.java`, `PendingTransferEntity.java` (infrastructure.persistence) — duplicates of entity versions

### Moved (package changed)
- `AccountDomainService.java`: service.account → service
- `AccountValidator.java`: service.account → service
- `TransactionDomainService.java`: service.transaction → service
- `TransactionOrchestratorService.java`: service.transaction → service
- `PendingTransferStore.java`: infrastructure.config → service
- `ScheduledTransactionExecutor.java`: infrastructure.config → service
- `TransactionCompletedEvent.java`: infrastructure.config → service
- `TransactionEventListener.java`: infrastructure.config → service
- `SecurityUtils.java`: security (kept, infrastructure.security deleted)
- Test files: src/main → src/test

### Merged
- `AiController.java` = AiServiceController + AiIntegrationController
  - `/api/ai/**` endpoints — Map-based, simple responses for AI microservice
  - `/api/ai/v1/**` endpoints — Typed DTO responses for structured clients

### Fixed
- Currency symbol `₹` → `LKR` throughout (AiResponseBuilder, AiController, AiServiceController)
- `CustomerBankingController` — removed orphan import of `customer.CustomerBankingService`
- Test classes moved to `src/test/java` where they belong

## Final package structure
```
com.agentic
├── AgenticAdminBackendApplication.java
├── controller/      AuthController, BankingController, CustomerBankingController,
│                    CardController, ScheduledTransferController, UserController,
│                    AiController (merged), GlobalExceptionHandler
├── service/         AccountService, AccountDomainService, AccountValidator,
│                    TransactionService, TransactionDomainService, TransactionValidator,
│                    TransactionOrchestratorService, OtpService, OtpTransactionService,
│                    BeneficiaryService, BeneficiaryTransferService, BillService,
│                    CardService, ScheduledTransferService, UserService, SecurityService,
│                    AuditService, StandardizedAuditLogger, AiInsightsService,
│                    AiAlertsService, SpendingInsightService, MerchantCategoryService,
│                    PendingTransferStore, ScheduledTransactionExecutor,
│                    TransactionCompletedEvent, TransactionEventListener
├── repository/      AccountRepository, TransactionRepository, UserRepository,
│                    BeneficiaryRepository, BillProviderRepository, CardRepository,
│                    OneTimePasswordRepository, ScheduledTransferRepository,
│                    AuditLogRepository, IdempotencyRepository
├── entity/          Account, Transaction, User, Beneficiary, Card, BillProvider,
│                    BillType, OneTimePassword, ScheduledTransfer, Conversation,
│                    AuditLog, IdempotencyRecord, SpendingCategory
├── dto/             All request/response DTOs
│   └── ai/          AI-specific DTOs (AiBaseResponse, AiTransferRequest, etc.)
├── exception/       All custom exceptions
├── config/          Spring config beans (SecurityConfig, CorsConfig, AsyncConfiguration,
│                    JwtAuthConverter, TransferConfig, RateLimitingService,
│                    CorrelationIdHolder, ObservabilityFilter, BillDataInitializer,
│                    CardDataInitializer, ScheduledTransferScheduler)
├── security/        SecurityUtils
└── ai/
    ├── service/     AiResponseBuilder, ConversationManager, ConversationRepository,
    │                IntentRouter
    ├── usecase/     BalanceUseCase, CardUseCase, InsightsUseCase, PaymentUseCase,
    │                TransactionUseCase, TransferUseCase
    └── usecase/impl CardUseCaseImpl, InsightsUseCaseImpl, PaymentUseCaseImpl,
                     TransactionUseCaseImpl, TransferUseCaseImpl
```

## TODO before next development sprint
1. Replace `getLoggedInUserId()` placeholder (returns 1L) with real JWT extraction
2. Wire up `BalanceUseCaseImpl` — interface exists but no impl was in source
3. `ScheduledTransactionExecutor` is a stub — implement transaction processing logic
4. `PendingTransferStore` references `PendingTransferRepository` — ensure that repository is wired
5. Replace any remaining `USD` currency strings (search for "USD" in codebase)
