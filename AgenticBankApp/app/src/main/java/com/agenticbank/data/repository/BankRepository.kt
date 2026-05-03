package com.agenticbank.data.repository

import android.content.Context
import com.agenticbank.data.api.RetrofitClient
import com.agenticbank.data.models.*
import retrofit2.Response

class BankRepository(context: Context) {

    private val api = RetrofitClient.getApiService(context)

    // ─── Auth ──────────────────────────────────────────────────────────────
    suspend fun login(username: String, password: String) =
        api.login(LoginRequest(username, password))

    suspend fun register(username: String, password: String, email: String, fullName: String) =
        api.register(RegisterRequest(username, password, email, fullName))

    suspend fun getProfile() = api.getProfile()

    // ─── Account ───────────────────────────────────────────────────────────
    suspend fun getAccounts() = api.getAccounts()
    suspend fun getBalance(accountId: Long) = api.getBalance(accountId)
    suspend fun getAccountDetails(accountId: Long) = api.getAccountDetails(accountId)

    // ─── Transactions ──────────────────────────────────────────────────────
    suspend fun getTransactions(accountId: Long, page: Int = 0, size: Int = 20) =
        api.getTransactions(accountId, page, size)

    suspend fun getRecentTransactions() = api.getRecentTransactions()

    // ─── Transfer ──────────────────────────────────────────────────────────
    suspend fun requestTransferOtp(fromAccountId: Long, toAccountId: Long, amount: Double) =
        api.requestTransferOtp(TransferOtpRequest(fromAccountId, toAccountId, amount))

    suspend fun verifyTransferOtp(
        otp: String, fromAccountId: Long, toAccountId: Long, amount: Double
    ) = api.verifyTransferOtp(TransferVerifyRequest(otp, fromAccountId, toAccountId, amount))

    // ─── Beneficiaries ─────────────────────────────────────────────────────
    suspend fun getBeneficiaries() = api.getBeneficiaries()
    suspend fun addBeneficiary(nickname: String, holderName: String, accountId: Long) =
        api.addBeneficiary(BeneficiaryRequest(nickname, holderName, accountId))

    suspend fun updateBeneficiary(id: Long, nickname: String, holderName: String, accountId: Long) =
        api.updateBeneficiary(id, BeneficiaryRequest(nickname, holderName, accountId))

    suspend fun deleteBeneficiary(id: Long) = api.deleteBeneficiary(id)

    // ─── Bills ─────────────────────────────────────────────────────────────
    suspend fun getBills() = api.getBills()
    suspend fun payBill(billId: Long, amount: Double, fromAccountId: Long, otp: String) =
        api.payBill(BillPayRequest(billId, amount, fromAccountId, otp))

    // ─── Cards ─────────────────────────────────────────────────────────────
    suspend fun getCards() = api.getCards()
    suspend fun blockCard(cardId: Long) = api.blockCard(cardId)
    suspend fun unblockCard(cardId: Long) = api.unblockCard(cardId)

    // ─── Scheduled Transfers ───────────────────────────────────────────────
    suspend fun getScheduledTransfers() = api.getScheduledTransfers()
    suspend fun createScheduledTransfer(
        fromAccountId: Long, toAccountId: Long,
        amount: Double, scheduledDate: String, description: String?
    ) = api.createScheduledTransfer(
        ScheduledTransferRequest(fromAccountId, toAccountId, amount, scheduledDate, description)
    )
    suspend fun cancelScheduledTransfer(id: Long) = api.cancelScheduledTransfer(id)

    // ─── Insights ──────────────────────────────────────────────────────────
    suspend fun getInsights() = api.getInsights()

    // ─── AI Chat ───────────────────────────────────────────────────────────
    suspend fun sendChatMessage(message: String, accountId: Long?, sessionId: String?) =
        api.sendChatMessage(AiChatRequest(message, accountId, sessionId))
}
