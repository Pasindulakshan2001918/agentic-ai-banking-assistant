# Banking Backend - Quick Reference Guide

## 🚀 REST API Endpoints

### User Management (`/api/users`)

#### Create User (Public)
```bash
POST /api/users
Content-Type: application/json

{
  "username": "john.doe",
  "email": "john@example.com",
  "password": "SecurePass123",
  "fullName": "John Doe",
  "role": "USER"
}

Response: 201 Created
{
  "user": {
    "id": 1,
    "username": "john.doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "role": "USER",
    "status": "ACTIVE",
    "createdAt": 1711012000
  },
  "message": "User created successfully",
  "timestamp": "2026-03-21T10:20:30Z"
}
```

#### Get User by ID (Requires Auth)
```bash
GET /api/users/{id}
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "user": { ... },
  "timestamp": "2026-03-21T10:20:30Z"
}
```

#### List All Users (Admin Only)
```bash
GET /api/users?page=0&size=20
Authorization: Bearer {ADMIN_JWT_TOKEN}

Response: 200 OK
{
  "users": [ ... ],
  "totalElements": 50,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20,
  "timestamp": "2026-03-21T10:20:30Z"
}
```

#### Update User (Admin Only)
```bash
PUT /api/users/{id}
Authorization: Bearer {ADMIN_JWT_TOKEN}
Content-Type: application/json

{
  "role": "ADMIN"
}

Response: 200 OK
{
  "user": { ... },
  "message": "User updated successfully",
  "timestamp": "2026-03-21T10:20:30Z"
}
```

#### Delete User (Admin Only)
```bash
DELETE /api/users/{id}
Authorization: Bearer {ADMIN_JWT_TOKEN}

Response: 200 OK
{
  "message": "User deleted successfully",
  "userId": 1,
  "timestamp": "2026-03-21T10:20:30Z"
}
```

#### Get Current User
```bash
GET /api/users/me
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "user": { ... },
  "timestamp": "2026-03-21T10:20:30Z"
}
```

---

### Banking Operations (`/api/banking`, `/api/customer`)

#### Create Transaction (CREATOR Role)
```bash
POST /api/banking/transactions
Authorization: Bearer {CREATOR_JWT_TOKEN}
Content-Type: application/json

{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 5000,
  "description": "Payment to vendor"
}

Response: 201 Created
{
  "message": "Transaction created successfully. Awaiting approval.",
  "transaction": { ... },
  "status": "PENDING",
  "createdBy": "creator@example.com",
  "timestamp": "2026-03-21T10:20:30Z"
}
```

#### Approve Transaction (APPROVER Role)
```bash
POST /api/banking/transactions/{transactionId}/approve
Authorization: Bearer {APPROVER_JWT_TOKEN}
Content-Type: application/json

{
  "remarks": "Approved"
}

Response: 200 OK
```

#### Get Account Balance
```bash
GET /api/customer/balance/{accountId}
Authorization: Bearer {JWT_TOKEN}

Response: 200 OK
{
  "accountId": 1,
  "accountNumber": "ACC001",
  "balance": 50000.00,
  "currency": "USD",
  "status": "ACTIVE",
  "timestamp": "2026-03-21T10:20:30Z"
}
```

---

## 🔐 Security Features

### Authentication
- **Method:** OAuth2 JWT (Keycloak)
- **Header:** `Authorization: Bearer {TOKEN}`
- **Token Claim:** "sub" (user ID)

### Authorization
- **Method:** Spring Security `@PreAuthorize` annotations
- **Roles:** ADMIN, CREATOR, APPROVER, VIEWER, SUPER_ADMIN

### Input Validation
- **Annotations:** `@Valid`, `@NotBlank`, `@Email`, `@Size`, `@Positive`
- **Failure:** Returns 400 Bad Request with error details

### Data Protection
- **Passwords:** Never in API responses (hashed with BCrypt)
- **DTOs:** Used for all responses (entities hidden)
- **Timestamps:** UTC format only

---

## ⚙️ Configuration

### Transfer Limits (application.yml)
```yaml
transfer:
  instant:
    limit: 250000      # Max instant transfer
  daily:
    limit: 5000000     # Max daily transfer
  daily-transaction-count-limit: 100
```

### Database Isolation
```yaml
jpa:
  hibernate:
    jdbc:
      batch_size: 20
      fetch_size: 50
    order_inserts: true
    order_updates: true
```

---

## ✅ Validation Rules

### User Creation
- Username: 3-50 characters
- Email: Valid email format (RFC 5322)
- Password: Minimum 8 characters
- Full Name: 2-100 characters
- Role: USER (default), ADMIN, SUPER_ADMIN

### Transfers
- Amount: Must be positive
- Daily limit: Cannot exceed configured limit
- Account status: Both must be ACTIVE
- Ownership: User must own FROM account
- Same account: Cannot transfer to self

---

## 🚨 Error Responses

### 400 Bad Request
```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Password must be at least 8 characters",
  "status": 400,
  "path": "/api/users"
}
```

### 401 Unauthorized
```json
{
  "errorCode": "UNAUTHORIZED",
  "message": "User not authenticated",
  "status": 401,
  "path": "/api/users/{id}"
}
```

### 403 Forbidden
```json
{
  "errorCode": "FORBIDDEN",
  "message": "You do not have access to this account",
  "status": 403,
  "path": "/api/customer/balance/999"
}
```

### 404 Not Found
```json
{
  "errorCode": "USER_NOT_FOUND",
  "message": "User not found with id: 999",
  "status": 404,
  "path": "/api/users/999",
  "details": {
    "entityType": "User",
    "entityId": "999"
  }
}
```

---

## 🧪 Testing

### Run Unit Tests
```bash
mvn test -Dtest=TransactionValidatorTest
```

### Run Integration Tests (H2)
```bash
mvn test -Dtest=UserControllerIntegrationTest
```

### Run All Tests
```bash
mvn test
```

### Compile Only (No Tests)
```bash
mvn clean compile -DskipTests
```

---

## 📝 Common Use Cases

### Create and Approve a Transfer
1. CREATOR: `POST /api/banking/transactions`
   - Status becomes PENDING
2. APPROVER: `POST /api/banking/transactions/{id}/approve`
   - Status becomes APPROVED
   - Funds transferred
3. VIEWER: `GET /api/banking/transactions/{id}`
   - View completed transaction

### Manage Users (Admin)
1. Create: `POST /api/users` (anyone)
2. List: `GET /api/users` (ADMIN only)
3. Update: `PUT /api/users/{id}` (ADMIN only)
4. Delete: `DELETE /api/users/{id}` (ADMIN only)

### Check Account Status
1. Get balance: `GET /api/customer/balance/{accountId}`
2. Get details: `GET /api/customer/account/{accountId}`
3. Get dashboard: `GET /api/customer/dashboard`

---

## 🔍 Key Implementation Details

### Async Audit Logging
- Critical operations: Synchronous audit
- Non-critical: Async via `@Async("auditExecutor")`
- Prevents audit logging from blocking transactions

### Transaction Safety
- Isolation Level: SERIALIZABLE
- Locking: Pessimistic write locks
- Daily limits: Enforced at DB level

### DTO Usage
- **Entity → DTO:** `UserDto.fromEntity(user)`
- **DTO → Entity:** Service layer only
- **API Responses:** Always use DTOs

---

## 🆘 Troubleshooting

### 401 Unauthorized
✓ Verify JWT token is valid
✓ Check Authorization header format: `Bearer {TOKEN}`
✓ Ensure token not expired

### 403 Forbidden
✓ Check user role matches endpoint requirement
✓ Verify account ownership for banking operations
✓ Check authorization rules: `@PreAuthorize("hasRole(...)")`

### 400 Bad Request
✓ Validate all required fields present
✓ Check email format is valid
✓ Ensure password is 8+ characters
✓ Verify amount is positive number

### 404 Not Found
✓ Verify user/account/transaction ID exists
✓ Check ID is numeric (not string)
✓ Ensure resource belongs to authenticated user

---

**Last Updated:** 2026-03-21  
**Version:** 1.0
