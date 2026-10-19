package com.lunorlabs.bank.action;

import com.lunorlabs.bank.common.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;

/** Add a constant + an ActionHandler bean to ship a new banking feature. Nothing else changes. */
public enum ActionType {
    FUND_TRANSFER("MSG_TRANSFER_DONE"),
    BILL_PAYMENT("MSG_BILL_DONE");

    private final String doneMessageKey;

    ActionType(String doneMessageKey) { this.doneMessageKey = doneMessageKey; }

    public String doneMessageKey() { return doneMessageKey; }

    public static ActionType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "actionType is required");
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Unsupported actionType: " + raw);
        }
    }
}
