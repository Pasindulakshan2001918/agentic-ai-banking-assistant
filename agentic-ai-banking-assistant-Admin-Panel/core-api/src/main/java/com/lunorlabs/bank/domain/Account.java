package com.lunorlabs.bank.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Account(UUID id, UUID customerId, String accountNo, String productType, String nickname,
                      String currency, BigDecimal balance, String status) {
    public String last4() {
        return accountNo.length() <= 4 ? accountNo : accountNo.substring(accountNo.length() - 4);
    }
    public boolean active() { return "ACTIVE".equals(status); }
}
