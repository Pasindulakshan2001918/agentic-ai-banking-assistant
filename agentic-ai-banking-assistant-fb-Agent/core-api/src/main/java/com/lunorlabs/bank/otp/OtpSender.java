package com.lunorlabs.bank.otp;

/** Seam for a real SMS gateway. */
public interface OtpSender {
    void send(String mobile, String code);
}
