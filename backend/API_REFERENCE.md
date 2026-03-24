# Agentic Admin Backend - API Quick Reference

## Base URL
```
http://localhost:8080/api/customer
```

## Authentication
All endpoints require `@PreAuthorize("isAuthenticated()") - pass JWT token in Authorization header

---

## BALANCE & ACCOUNT ENDPOINTS

### 1. Get Account Balance
```http
GET /balance/{accountId}
Authorization: Bearer {jwt_token}

Response (200):
{
  "accountId": 1,
  "accountNumber": "ACC001",
  "balance": 50000.00,
  "currency": "USD",
  "status": "ACTIVE",
  "timestamp": "2026-03-20T11:16:21"
}
```

### 2. Get Account Details
```http
GET /account/{accountId}
Authorization: Bearer {jwt_token}

Response (200):
{
  "id": 1,
  "accountNumber": "ACC001",
  "accountType": "SAVINGS",
  "balance": 50000.00,
  "status": "ACTIVE",
  "currency": "USD",
  "createdAt": "2026-03-20T10:00:00"
}
```

---

## TRANSFER ENDPOINTS (OTP Flow)

### Step 1: Request OTP for Transfer
```http
POST /transfer/request-otp
Authorization: Bearer {jwt_token}
Content-Type: application/json

Body:
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 5000.00
}

Response (200):
{
  "transferReference": "TXN_1234567890",
  "status": "OTP_SENT",
  "message": "OTP sent to registered email/phone",
  "otp": "123456",
  "expiryMinutes": 5,
  "timestamp": "2026-03-20T11:16:21"
}
```

### Step 2: Verify OTP & Execute Transfer
```http
POST /transfer/verify-otp
Authorization: Bearer {jwt_token}
Content-Type: application/json

Body:
{
  "otpCode": "123456",
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 5000.00
}

Response (200):
{
  "message": "Transfer successful",
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 5000.00,
  "status": "COMPLETED",
  "timestamp": "2026-03-20T11:16:21",
  "initiatedBy": "john@example.com"
}
```

---

## TRANSACTION HISTORY ENDPOINTS

### Get Transaction History
```http
GET /transactions/{accountId}?page=0&size=10
Authorization: Bearer {jwt_token}

Response (200):
{
  "accountId": 1,
  "transactions": [...],
  "page": 0,
  "size": 10,
  "totalTransactions": 42
}
```

---

## BENEFICIARY ENDPOINTS

### 1. Get All Beneficiaries
```http
GET /beneficiaries
Authorization: Bearer {jwt_token}

Response (200):
{
  "beneficiaries": [
    {
      "id": 1,
      "nickname": "Nimal",
      "accountHolderName": "Nimal Fernando",
      "accountNumber": "ACC002",
      "status": "ACTIVE",
      "createdAt": "2026-03-15T10:00:00"
    },
    {
      "id": 2,
      "nickname": "Mom",
      "accountHolderName": "Maria Garcia",
      "accountNumber": "ACC003",
      "status": "ACTIVE",
      "createdAt": "2026-03-16T10:00:00"
    }
  ],
  "count": 2,
  "timestamp": "2026-03-20T11:16:21"
}
```

### 2. Add Beneficiary
```http
POST /beneficiaries
Authorization: Bearer {jwt_token}
Content-Type: application/json

Body:
{
  "nickname": "Nimal",
  "accountHolderName": "Nimal Fernando",
  "accountId": 5
}

Response (201):
{
  "id": 1,
  "nickname": "Nimal",
  "accountHolderName": "Nimal Fernando",
  "accountNumber": "ACC002",
  "status": "ACTIVE",
  "createdAt": "2026-03-20T11:16:21"
}
```

### 3. Update Beneficiary
```http
PUT /beneficiaries/{id}
Authorization: Bearer {jwt_token}
Content-Type: application/json

Body:
{
  "nickname": "Nimal (Updated)",
  "accountHolderName": "Nimal Fernando",
  "accountId": 5
}

Response (200):
{
  "id": 1,
  "nickname": "Nimal (Updated)",
  "accountHolderName": "Nimal Fernando",
  "accountNumber": "ACC002",
  "status": "ACTIVE",
  "createdAt": "2026-03-20T11:16:21"
}
```

### 4. Delete Beneficiary
```http
DELETE /beneficiaries/{id}
Authorization: Bearer {jwt_token}

Response (200):
{
  "message": "Beneficiary deleted successfully"
}
```

---

## ERROR RESPONSES

### Unauthorized Access (403)
```json
{
  "error": "You do not own the source account"
}
```

### Invalid OTP (400)
```json
{
  "error": "Invalid OTP code"
}
```

### OTP Expired (400)
```json
{
  "error": "OTP has expired"
}
```

### Insufficient Balance (400)
```json
{
  "error": "Insufficient balance"
}
```

### Concurrent Transaction (400)
```json
{
  "error": "Concurrent transaction detected. Please try again."
}
```

---

## AI INTEGRATION GUIDE

### Intent Mapping

| User Says | Intent | API Call | Required Params |
|-----------|--------|----------|-----------------|
| "What's my balance?" | `check_balance` | `GET /balance/{accountId}` | accountId (use default) |
| "Send 5000 to Nimal" | `transfer_money` | 1. `GET /beneficiaries` → find Nimal 2. `POST /transfer/request-otp` 3. `POST /transfer/verify-otp` | amount, beneficiary nickname |
| "Show my beneficiaries" | `get_beneficiaries` | `GET /beneficiaries` | None |
| "Show recent transactions" | `recent_transactions` | `GET /transactions/{accountId}` | accountId |

### OTP Workflow (For AI)
```
1. User: "Send 5000 to Nimal"
   ↓
2. AI: Extract intent (transfer_money), amount (5000), beneficiary (Nimal)
   ↓
3. AI: Call GET /beneficiaries → Find Nimal's account
   ↓
4. AI: Call POST /transfer/request-otp with fromAccountId, toAccountId, amount
   ↓
5. AI: Ask user for OTP ("Enter the OTP sent to your email")
   ↓
6. User: "123456"
   ↓
7. AI: Call POST /transfer/verify-otp with otpCode, accounts, amount
   ↓
8. AI: Confirm to user ("Transfer of 5000 to Nimal completed successfully")
```

---

## Testing Checklist

- [ ] Test balance retrieval with ownership validation
- [ ] Test OTP generation and expiry
- [ ] Test OTP rate limiting (3 attempts max)
- [ ] Test concurrent transfers (optimistic locking)
- [ ] Test beneficiary CRUD operations
- [ ] Test unauthorized access attempts
- [ ] Test transfer workflow end-to-end
- [ ] Test transaction history pagination
