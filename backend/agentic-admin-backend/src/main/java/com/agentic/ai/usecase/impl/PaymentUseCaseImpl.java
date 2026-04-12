package com.agentic.ai.usecase.impl;

import com.agentic.ai.usecase.PaymentUseCase;
import com.agentic.ai.service.AiResponseBuilder;
import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiPaymentRequest;
import com.agentic.dto.BillPaymentRequest;
import com.agentic.dto.BillPaymentResponse;
import com.agentic.entity.Conversation;
import com.agentic.service.BillService;
import com.agentic.service.OtpService;
import com.agentic.service.AccountService;
import org.springframework.stereotype.Service;

@Service
public class PaymentUseCaseImpl implements PaymentUseCase {

    private final BillService billService;
    private final OtpService otpService;
    private final AccountService accountService;
    private final AiResponseBuilder aiResponseBuilder;

    public PaymentUseCaseImpl(
            BillService billService,
            OtpService otpService,
            AccountService accountService,
            AiResponseBuilder aiResponseBuilder) {
        this.billService = billService;
        this.otpService = otpService;
        this.accountService = accountService;
        this.aiResponseBuilder = aiResponseBuilder;
    }

    @Override
    public AiBaseResponse execute(Conversation conversation, AiPaymentRequest request) {
        String operationId = aiResponseBuilder.generateOperationId();
        try {
            Long userId = conversation.getUser().getId();

            // OTP verification step
            if (conversation.getState() != null &&
                    conversation.getState().toString().equals("AWAITING_OTP")) {
                if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
                    return aiResponseBuilder.buildOtpResponse(operationId, conversation);
                }
                boolean otpValid = otpService.verifyOtp(
                    userId,
                    request.getOtpCode(),
                    "BILL-" + conversation.getId()
                );
                if (!otpValid) {
                    return aiResponseBuilder.buildFailureResponse(
                        operationId, "Invalid or expired OTP.", conversation);
                }
                BillPaymentRequest billReq = new BillPaymentRequest();
                billReq.setBillId(request.getBillId());
                billReq.setAccountId(accountService.getPrimaryAccount(userId)
                    .orElseThrow(() -> new RuntimeException("No primary account")).getId());
                billReq.setOtp(request.getOtpCode());
                billReq.setCustomAmount(request.getAmount());
                BillPaymentResponse billResponse =
                    billService.payBill(userId, billReq);
                return aiResponseBuilder.buildPaymentResponse(
                    operationId, "SUCCESS", billResponse.getMessage(),
                    request.getBillType(), billResponse.getAmountPaid(), conversation
                );
            }

            // Show confirmation
            if (request.getConfirmed() == null || !request.getConfirmed()) {
                String summary = "Pay LKR " + request.getAmount() +
                    " for " + request.getBillType() + " bill";
                return aiResponseBuilder.buildConfirmationResponse(operationId, summary, conversation);
            }

            // Generate OTP
            otpService.generateOtp(userId, "BILL-" + conversation.getId());
            return aiResponseBuilder.buildOtpResponse(operationId, conversation);

        } catch (Exception e) {
            return aiResponseBuilder.buildFailureResponse(operationId, e.getMessage(), conversation);
        }
    }
}
