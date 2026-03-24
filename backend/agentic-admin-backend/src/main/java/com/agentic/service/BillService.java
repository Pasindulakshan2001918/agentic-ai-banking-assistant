package com.agentic.service;

import com.agentic.dto.BillPaymentRequest;
import com.agentic.dto.BillPaymentResponse;
import com.agentic.dto.BillResponse;
import com.agentic.entity.*;
import com.agentic.exception.*;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.BillProviderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SERVICE: Bill Payment Processing
 * 
 * Handles utility bill payments with OTP verification.
 * Reuses existing TransactionService, OtpService, and AuditService — no duplication.
 * 
 * FLOW:
 * 1. AI calls getBill() to fetch outstanding bill
 * 2. AI shows user the bill amount
 * 3. User confirms payment
 * 4. AI requests OTP from /api/public/otp/request
 * 5. User enters OTP
 * 6. AI calls payBill() with OTP
 * 7. BillService verifies OTP → debits account → marks bill PAID → records transaction
 */
@Service
public class BillService {

    private final BillProviderRepository billProviderRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final OtpService otpService;
    private final AuditService auditService;

    public BillService(BillProviderRepository billProviderRepository,
                       AccountRepository accountRepository,
                       TransactionService transactionService,
                       OtpService otpService,
                       AuditService auditService) {
        this.billProviderRepository = billProviderRepository;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.otpService = otpService;
        this.auditService = auditService;
    }

    /**
     * Step 1 of 2 — fetch the outstanding bill.
     * The AI calls this first to show the user what they owe.
     * 
     * @param userId The authenticated user
     * @param billType The type of bill (ELECTRICITY, WATER, MOBILE_RECHARGE)
     * @return Bill response with amount, due date, etc.
     * @throws EntityNotFoundException if no pending bill found
     */
    public BillResponse getBill(Long userId, BillType billType) {
        BillProvider bill = billProviderRepository
            .findByUserIdAndBillTypeAndStatus(userId, billType, BillProvider.BillStatus.PENDING)
            .orElseThrow(() -> new EntityNotFoundException(
                "No pending " + billType.name().toLowerCase() + " bill found for your account."
            ));

        BillResponse response = new BillResponse();
        response.setBillId(bill.getId());
        response.setProviderName(bill.getProviderName());
        response.setBillType(bill.getBillType());
        response.setAccountReference(bill.getAccountReference());
        response.setOutstandingAmount(bill.getOutstandingAmount());
        response.setDueDate(bill.getDueDate());
        response.setStatus(bill.getStatus().name());
        return response;
    }

    /**
     * Step 2 of 2 — pay the bill after OTP is verified.
     * Atomic transaction: OTP check → balance deduction → bill marked PAID → transaction recorded.
     * 
     * @param userId The authenticated user
     * @param request Bill payment request with billId, accountId, and OTP
     * @return Payment response with reference number and balance
     * @throws ValidationException if OTP is invalid
     * @throws UnauthorizedException if user doesn't own the bill or account
     * @throws InsufficientFundsException if account balance is too low
     */
    @Transactional
    public BillPaymentResponse payBill(Long userId, BillPaymentRequest request) {

        // 1. Load and validate the bill first to generate OTP reference
        BillProvider bill = billProviderRepository.findById(request.getBillId())
            .orElseThrow(() -> new EntityNotFoundException("Bill not found."));

        if (!bill.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to pay this bill.");
        }
        if (bill.getStatus() == BillProvider.BillStatus.PAID) {
            throw new InvalidTransactionException("This bill has already been paid.");
        }

        // Generate OTP reference based on bill
        String otpReference = "BILL-" + bill.getId();

        // 2. Verify OTP using the bill reference
        boolean otpValid = otpService.verifyOtp(userId, request.getOtp(), otpReference);
        if (!otpValid) {
            throw new ValidationException("Invalid or expired OTP. Please request a new one.");
        }

        // 3. Determine amount — for mobile recharge, allow custom amount
        BigDecimal amountToPay = (bill.getBillType() == BillType.MOBILE_RECHARGE
                && request.getCustomAmount() != null
                && request.getCustomAmount().compareTo(BigDecimal.ZERO) > 0)
            ? request.getCustomAmount()
            : bill.getOutstandingAmount();

        // 4. Load and validate the bank account
        Account account = accountRepository.findById(request.getAccountId())
            .orElseThrow(() -> new EntityNotFoundException("Account not found."));

        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not own this account.");
        }
        if (account.getBalance().compareTo(amountToPay) < 0) {
            throw new InsufficientFundsException(
                "Insufficient balance. Available: LKR " + account.getBalance()
                + ", Required: LKR " + amountToPay,
                account.getBalance(),
                amountToPay
            );
        }

        // 5. Debit the account
        account.setBalance(account.getBalance().subtract(amountToPay));
        accountRepository.save(account);

        // 6. Mark bill as PAID
        bill.setStatus(BillProvider.BillStatus.PAID);
        billProviderRepository.save(bill);

        // 7. Record the transaction (reuse existing TransactionService)
        String reference = generateReference();
        Transaction tx = new Transaction();
        tx.setFromAccount(account);
        tx.setToAccount(account);   // bill payment — same account as placeholder
        tx.setAmount(amountToPay);
        tx.setType(Transaction.TransactionType.BILL_PAYMENT);
        tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setDescription(bill.getProviderName() + " bill payment - " + bill.getAccountReference());
        tx.setReferenceNumber(reference);
        tx.setCategory(SpendingCategory.UTILITIES);
        tx.setIdempotencyKey(UUID.randomUUID().toString());
        tx.setCreatedBy(userId.toString());
        transactionService.save(tx);

        // 8. Async audit log
        auditService.logAction("BILL", bill.getId(), "PAYMENT", userId.toString(),
            null, "Bill payment processed",
            "Paid " + bill.getProviderName() + " LKR " + amountToPay + " Ref: " + reference);

        // 9. Build response
        BillPaymentResponse response = new BillPaymentResponse();
        response.setReferenceNumber(reference);
        response.setProviderName(bill.getProviderName());
        response.setAmountPaid(amountToPay);
        response.setRemainingBalance(account.getBalance());
        response.setPaidAt(LocalDateTime.now());
        response.setMessage("Payment successful. " + bill.getProviderName()
            + " bill of LKR " + amountToPay + " paid successfully.");
        return response;
    }

    /**
     * Fetch all pending bills for a user.
     * Useful for dashboard display — shows all outstanding bills at a glance.
     * 
     * @param userId The authenticated user
     * @return List of pending bills
     */
    public List<BillResponse> getAllPendingBills(Long userId) {
        return billProviderRepository
            .findByUserIdAndStatus(userId, BillProvider.BillStatus.PENDING)
            .stream()
            .map(bill -> {
                BillResponse r = new BillResponse();
                r.setBillId(bill.getId());
                r.setProviderName(bill.getProviderName());
                r.setBillType(bill.getBillType());
                r.setAccountReference(bill.getAccountReference());
                r.setOutstandingAmount(bill.getOutstandingAmount());
                r.setDueDate(bill.getDueDate());
                r.setStatus(bill.getStatus().name());
                return r;
            })
            .collect(Collectors.toList());
    }

    /**
     * Generate a unique reference number for bill payment.
     */
    private String generateReference() {
        return "BILL-" + System.currentTimeMillis();
    }
}
