package com.agentic.service;

import com.agentic.config.SmsConfig;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private final SmsConfig smsConfig;

    public SmsService(SmsConfig smsConfig) {
        this.smsConfig = smsConfig;
        if (smsConfig.isEnabled()) {
            Twilio.init(
                smsConfig.getTwilio().getAccountSid(),
                smsConfig.getTwilio().getAuthToken()
            );
            log.info("SMS service initialized with Twilio");
        } else {
            log.info("SMS service is DISABLED — OTPs will be logged only");
        }
    }

    /**
     * Send an OTP SMS to the given phone number.
     * If SMS is disabled (sms.enabled=false), logs the OTP instead.
     * This allows development without a real Twilio account.
     */
    public void sendOtp(String toPhoneNumber, String otpCode) {
        String messageBody = "Your Agentic Bank OTP is: " + otpCode +
            ". Valid for 5 minutes. Do not share this code.";

        if (!smsConfig.isEnabled()) {
            log.info("[SMS DISABLED] OTP for {}: {}", maskPhone(toPhoneNumber), otpCode);
            return;
        }

        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            log.warn("Cannot send OTP — phone number is null or empty");
            return;
        }

        try {
            Message message = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(smsConfig.getTwilio().getFromNumber()),
                messageBody
            ).create();
            log.info("OTP SMS sent to {} — SID: {}", maskPhone(toPhoneNumber), message.getSid());
        } catch (Exception e) {
            log.error("Failed to send OTP SMS to {}: {}", maskPhone(toPhoneNumber), e.getMessage());
            // Do NOT rethrow — SMS failure should not block the OTP from being generated
            // The OTP is still saved in the database and can be retrieved another way
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
