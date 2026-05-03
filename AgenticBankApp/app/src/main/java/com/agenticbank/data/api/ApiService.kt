package com.agenticbank.data.api

import com.agenticbank.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Auth ──────────────────────────────────────────────────────────────
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("auth/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    // ─── Account / Balance ─────────────────────────────────────────────────
    @GET("api/customer/accounts")
    suspend fun getAccounts(): Response<AccountsResponse>

    @GET("api/customer/balance/{accountId}")
    suspend fun getBalance(@Path("accountId") accountId: Long): Response<BalanceResponse>

    @GET("api/customer/account/{accountId}")
    suspend fun getAccountDetails(@Path("accountId") accountId: Long): Response<AccountResponse>

    // ─── Transactions ──────────────────────────────────────────────────────
    @GET("api/customer/transactions/{accountId}")
    suspend fun getTransactions(
        @Path("accountId") accountId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<TransactionHistoryResponse>

    @GET("api/customer/transactions/recent")
    suspend fun getRecentTransactions(): Response<RecentTransactionsResponse>

    // ─── Transfer ──────────────────────────────────────────────────────────
    @POST("api/customer/transfer/request-otp")
    suspend fun requestTransferOtp(@Body request: TransferOtpRequest): Response<TransferOtpResponse>

    @POST("api/customer/transfer/verify-otp")
    suspend fun verifyTransferOtp(@Body request: TransferVerifyRequest): Response<TransferResponse>

    @POST("api/customer/transfer")
    suspend fun directTransfer(@Body request: TransferOtpRequest): Response<TransferResponse>

    // ─── Beneficiaries ─────────────────────────────────────────────────────
    @GET("api/customer/beneficiaries")
    suspend fun getBeneficiaries(): Response<BeneficiariesListResponse>

    @POST("api/customer/beneficiaries")
    suspend fun addBeneficiary(@Body request: BeneficiaryRequest): Response<BeneficiaryResponse>

    @PUT("api/customer/beneficiaries/{id}")
    suspend fun updateBeneficiary(
        @Path("id") id: Long,
        @Body request: BeneficiaryRequest
    ): Response<BeneficiaryResponse>

    @DELETE("api/customer/beneficiaries/{id}")
    suspend fun deleteBeneficiary(@Path("id") id: Long): Response<MessageResponse>

    // ─── Bills ─────────────────────────────────────────────────────────────
    @GET("api/customer/bills")
    suspend fun getBills(): Response<BillsResponse>

    @POST("api/customer/bills/pay")
    suspend fun payBill(@Body request: BillPayRequest): Response<BillPayResponse>

    // ─── Cards ─────────────────────────────────────────────────────────────
    @GET("api/customer/cards")
    suspend fun getCards(): Response<CardsResponse>

    @POST("api/customer/cards/{cardId}/block")
    suspend fun blockCard(@Path("cardId") cardId: Long): Response<CardActionResponse>

    @POST("api/customer/cards/{cardId}/unblock")
    suspend fun unblockCard(@Path("cardId") cardId: Long): Response<CardActionResponse>

    // ─── Scheduled Transfers ───────────────────────────────────────────────
    @GET("api/scheduled-transfers")
    suspend fun getScheduledTransfers(): Response<ScheduledTransfersResponse>

    @POST("api/scheduled-transfers")
    suspend fun createScheduledTransfer(
        @Body request: ScheduledTransferRequest
    ): Response<ScheduledTransfer>

    @DELETE("api/scheduled-transfers/{id}")
    suspend fun cancelScheduledTransfer(@Path("id") id: Long): Response<MessageResponse>

    // ─── Insights ──────────────────────────────────────────────────────────
    @GET("api/ai/v1/insights")
    suspend fun getInsights(): Response<InsightsResponse>

    // ─── AI Chat ───────────────────────────────────────────────────────────
    @POST("api/ai/chat")
    suspend fun sendChatMessage(@Body request: AiChatRequest): Response<AiChatResponse>
}
