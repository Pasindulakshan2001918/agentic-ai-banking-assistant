package com.lunorlabs.bank.otp;

import com.lunorlabs.bank.config.BankProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Dev sender: logs instead of sending an SMS. The code itself is logged only when dev-echo is on. */
@Component
public class LogOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(LogOtpSender.class);
    private final boolean echo;

    public LogOtpSender(BankProperties props) { this.echo = props.otp().devEcho(); }

    @Override
    public void send(String mobile, String code) {
        String masked = mobile.length() > 4 ? "******" + mobile.substring(mobile.length() - 4) : "****";
        log.info("[OTP simulated] to {} code {}", masked, echo ? code : "******");
    }
}
