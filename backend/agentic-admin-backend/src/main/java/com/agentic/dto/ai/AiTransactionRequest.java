
package com.agentic.dto.ai;

import java.math.BigDecimal;

/**

* AI TRANSACTION REQUEST DTO

*/

public class AiTransactionRequest {

private Long userId;

private Long accountId;

private String dateRange; // "last 30 days", "this month", etc

private String transactionType; // "all", "transfer", "payment", etc

// Getters & Setters

public Long getUserId() {

return userId;

}

public void setUserId(Long userId) {

this.userId = userId;

}

public Long getAccountId() {

return accountId;

}

public void setAccountId(Long accountId) {

this.accountId = accountId;

}

public String getDateRange() {

return dateRange;

}

public void setDateRange(String dateRange) {

this.dateRange = dateRange;

}

public String getTransactionType() {

return transactionType;

}

public void setTransactionType(String transactionType) {

this.transactionType = transactionType;

}

}
