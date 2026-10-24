package com.lunorlabs.bank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank")
public record BankProperties(String ownBankCode, boolean devLoginEnabled, Jwt jwt, Action action, Otp otp) {
    public record Jwt(String secret, int ttlMinutes) {}
    public record Action(int ttlMinutes) {}
    public record Otp(int ttlMinutes, int maxAttempts, int maxResends, boolean devEcho, String pepper) {}
}
