package com.lunorlabs.bank.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Payee(UUID id, String nickname, String beneficiaryName, String bankCode, String bankName,
                    String accountNo, OffsetDateTime addedAt) {
    public String last4() {
        return accountNo.length() <= 4 ? accountNo : accountNo.substring(accountNo.length() - 4);
    }
}
