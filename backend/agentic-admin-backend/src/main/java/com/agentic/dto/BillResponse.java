package com.agentic.dto;

import com.agentic.entity.BillType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO when AI fetches a bill.
 */
public class BillResponse {
    
    private Long billId;
    private String providerName;
    private BillType billType;
    private String accountReference;
    private BigDecimal outstandingAmount;
    private LocalDate dueDate;
    private String status;

    public BillResponse() {
    }

    public BillResponse(Long billId, String providerName, BillType billType,
                       String accountReference, BigDecimal outstandingAmount,
                       LocalDate dueDate, String status) {
        this.billId = billId;
        this.providerName = providerName;
        this.billType = billType;
        this.accountReference = accountReference;
        this.outstandingAmount = outstandingAmount;
        this.dueDate = dueDate;
        this.status = status;
    }

    // ===== GETTERS AND SETTERS =====

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public String getAccountReference() {
        return accountReference;
    }

    public void setAccountReference(String accountReference) {
        this.accountReference = accountReference;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
