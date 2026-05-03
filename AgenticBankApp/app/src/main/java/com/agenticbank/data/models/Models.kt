package com.agenticbank.data.models

import com.google.gson.annotations.SerializedName

// ─── Auth ───────────────────────────────────────────────────────────────────

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    @SerializedName("fullName") val fullName: String
)

data class AuthResponse(
    val token: String,
    val username: String,
    val role: String,
    val accountId: Long?,
    val userId: Long?,
    val message: String?
)

// ─── Account / Balance ───────────────────────────────────────────────────────

data class BalanceResponse(
    val accountId: Long,
    val accountNumber: String,
    val balance: Double,
    val currency: String,
    val status: String,
    val timestamp: String
)

data class AccountResponse(
    val id: Long,
    val accountNumber: String,
    val accountType: String,
    val balance: Double,
    val status: String,
    val currency: String,
    val createdAt: String
)

data class AccountsResponse(
    val accounts: List<AccountResponse>
)

// ─── Transactions ─────────────────────────────────────────────────────────────

data class Transaction(
    val id: Long,
    val type: String,
    val amount: Double,
    val description: String?,
    val status: String,
    val createdAt: String,
    val referenceNumber: String?
)

data class TransactionHistoryResponse(
    val accountId: Long,
    val transactions: List<Transaction>,
    val page: Int,
    val size: Int,
    val totalTransactions: Int
)

data class RecentTransactionsResponse(
    val transactions: List<Transaction>
)

// ─── Transfer ─────────────────────────────────────────────────────────────────

data class TransferOtpRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double
)

data class TransferOtpResponse(
    val transferReference: String,
    val status: String,
    val message: String,
    val otp: String?,
    val expiryMinutes: Int,
    val timestamp: String
)

data class TransferVerifyRequest(
    val otpCode: String,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double
)

data class TransferResponse(
    val message: String,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val status: String,
    val timestamp: String,
    val initiatedBy: String?
)

// ─── Beneficiaries ───────────────────────────────────────────────────────────

data class BeneficiaryResponse(
    val id: Long,
    val nickname: String,
    val accountHolderName: String,
    val accountNumber: String,
    val status: String,
    val createdAt: String
)

data class BeneficiariesListResponse(
    val beneficiaries: List<BeneficiaryResponse>,
    val count: Int,
    val timestamp: String
)

data class BeneficiaryRequest(
    val nickname: String,
    val accountHolderName: String,
    val accountId: Long
)

// ─── Bills ────────────────────────────────────────────────────────────────────

data class Bill(
    val id: Long,
    val billType: String,
    val provider: String,
    val accountReference: String,
    val amount: Double?,
    val dueDate: String?,
    val status: String
)

data class BillsResponse(
    val bills: List<Bill>
)

data class BillPayRequest(
    val billId: Long,
    val amount: Double,
    val fromAccountId: Long,
    val otpCode: String
)

data class BillPayResponse(
    val message: String,
    val billId: Long,
    val amount: Double,
    val status: String,
    val receiptNumber: String?,
    val timestamp: String
)

// ─── Cards ────────────────────────────────────────────────────────────────────

data class Card(
    val id: Long,
    val cardNumber: String,
    val cardType: String,
    val status: String,
    val expiryDate: String?,
    val isContactless: Boolean?
)

data class CardsResponse(
    val cards: List<Card>
)

data class CardActionResponse(
    val message: String,
    val cardId: Long,
    val status: String
)

// ─── Scheduled Transfers ─────────────────────────────────────────────────────

data class ScheduledTransfer(
    val id: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val scheduledDate: String,
    val status: String,
    val description: String?
)

data class ScheduledTransfersResponse(
    val scheduledTransfers: List<ScheduledTransfer>
)

data class ScheduledTransferRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val scheduledDate: String,
    val description: String?
)

// ─── Insights ─────────────────────────────────────────────────────────────────

data class SpendingCategory(
    val category: String,
    val amount: Double,
    val percentage: Double
)

data class InsightsResponse(
    val totalSpending: Double,
    val categories: List<SpendingCategory>,
    val monthlyComparison: Map<String, Double>?,
    val alerts: List<String>?
)

// ─── AI Chat ──────────────────────────────────────────────────────────────────

data class AiChatRequest(
    val message: String,
    val accountId: Long?,
    val sessionId: String?
)

data class AiChatResponse(
    val reply: String,
    val intent: String?,
    val data: Map<String, Any>?,
    val requiresOtp: Boolean?,
    val transferReference: String?
)

// ─── Profile ──────────────────────────────────────────────────────────────────

data class ProfileResponse(
    val username: String,
    val email: String,
    val fullName: String?,
    val roles: List<String>
)

// ─── Generic ──────────────────────────────────────────────────────────────────

data class MessageResponse(
    val message: String
)
