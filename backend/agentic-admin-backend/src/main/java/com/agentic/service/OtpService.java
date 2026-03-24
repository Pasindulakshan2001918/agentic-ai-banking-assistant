package com.agentic.service;

import com.agentic.config.RateLimitingService;
import com.agentic.config.CorrelationIdHolder;
import com.agentic.entity.OneTimePassword;
import com.agentic.entity.User;
import com.agentic.repository.OneTimePasswordRepository;
import com.agentic.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@Service
public class OtpService {
    
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    
    private final OneTimePasswordRepository otpRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final RateLimitingService rateLimitingService;
    
    private static final int OTP_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    
    public OtpService(OneTimePasswordRepository otpRepository,
                     UserRepository userRepository,
                     AuditService auditService,
                     RateLimitingService rateLimitingService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.rateLimitingService = rateLimitingService;
    }
    
    /**
     * Generate and send OTP for transfer
     * 
     * 🔒 PHASE 6.3 — Rate Limiting Applied
     * - Max 5 OTP requests per minute (Bucket4j)
     * - Throws exception if rate limit exceeded
     * 
     * ⚠️ In production: send via SMS/Email
     * For now: return OTP (for testing)
     */
    @Transactional
    public String generateOtp(Long userId, String reference) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 🔒 PHASE 6.3 — Rate Limiting Check
        String userIdKey = "user_" + userId;
        if (!rateLimitingService.allowOtpRequest(userIdKey)) {
            String errorMsg = "⚠️  OTP requests rate limited: max 5 per minute";
            log.warn("🚫 RATE_LIMIT_EXCEEDED | User: {} | Reason: {}", userId, errorMsg);
            
            // Log audit of rate limit violation
            String correlationId = CorrelationIdHolder.getCorrelationId();
            auditService.logAction("OTP", userId, "GENERATE_FAILED_RATE_LIMIT", "SYSTEM",
                null, errorMsg, "User exceeded OTP request rate limit", correlationId);
            
            throw new RuntimeException(errorMsg);
        }
        
        // Invalidate any existing pending OTP for this reference
        otpRepository.findLatestByUserAndReference(userId, reference)
            .ifPresent(existingOtp -> {
                if (existingOtp.getStatus() == OneTimePassword.OtpStatus.PENDING) {
                    existingOtp.setStatus(OneTimePassword.OtpStatus.EXPIRED);
                    otpRepository.save(existingOtp);
                }
            });
        
        // Generate 6-digit OTP
        String otpCode = String.format("%06d", new Random().nextInt(1000000));
        
        OneTimePassword otp = new OneTimePassword();
        otp.setUser(user);
        otp.setOtpCode(otpCode);
        otp.setReference(reference);
        otp.setStatus(OneTimePassword.OtpStatus.PENDING);
        otp.setExpiryMinutes(EXPIRY_MINUTES);
        otp.setAttemptCount(0);
        
        OneTimePassword saved = otpRepository.save(otp);
        
        // Log audit with correlation ID
        String correlationId = CorrelationIdHolder.getCorrelationId();
        auditService.logAction("OTP", saved.getId(), "GENERATE", "SYSTEM",
            null, "OTP generated for user: " + user.getUsername(),
            "OTP generated for transfer reference: " + reference, correlationId);
        
        log.info("✅ OTP_GENERATED | UserId: {} | Reference: {} | CorrelationId: {}", 
            userId, reference, correlationId);
        
        // 🔒 In production: Send via SMS
        // TODO: Implement SMS gateway integration
        // smsService.sendOtp(user.getPhoneNumber(), otpCode);
        
        // For testing: return OTP (REMOVE IN PRODUCTION)
        return otpCode;
    }
    
    /**
     * Verify OTP code
     * 
     * 🔒 PHASE 6 — Observability
     * - Logs with correlation ID
     * - Tracks failures in audit log
     */
    @Transactional
    public boolean verifyOtp(Long userId, String otpCode, String reference) {
        
        OneTimePassword otp = otpRepository.findLatestByUserAndReference(userId, reference)
            .orElseThrow(() -> new RuntimeException("No pending OTP found"));
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        
        // Check if OTP is expired
        LocalDateTime expiryTime = otp.getCreatedAt().plusMinutes(EXPIRY_MINUTES);
        if (LocalDateTime.now().isAfter(expiryTime)) {
            otp.setStatus(OneTimePassword.OtpStatus.EXPIRED);
            otpRepository.save(otp);
            
            log.warn("⏰ OTP_EXPIRED | UserId: {} | Reference: {} | CorrelationId: {}", 
                userId, reference, correlationId);
            
            auditService.logAction("OTP", otp.getId(), "VERIFY_FAILED_EXPIRED", "USER_" + userId,
                null, "OTP expired",
                "OTP verification failed: OTP has expired", correlationId);
            
            throw new RuntimeException("OTP has expired");
        }
        
        // Check if too many attempts
        if (otp.getAttemptCount() >= MAX_ATTEMPTS) {
            otp.setStatus(OneTimePassword.OtpStatus.FAILED);
            otpRepository.save(otp);
            
            log.warn("🔐 OTP_MAX_ATTEMPTS_EXCEEDED | UserId: {} | Reference: {} | CorrelationId: {}", 
                userId, reference, correlationId);
            
            auditService.logAction("OTP", otp.getId(), "VERIFY_FAILED_MAX_ATTEMPTS", "USER_" + userId,
                null, "Max attempts exceeded",
                "OTP verification failed: Too many attempts", correlationId);
            
            throw new RuntimeException("Maximum OTP verification attempts exceeded");
        }
        
        // Verify OTP code
        if (!otp.getOtpCode().equals(otpCode)) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
            
            log.warn("❌ OTP_INVALID | UserId: {} | Reference: {} | Attempt: {}/{} | CorrelationId: {}", 
                userId, reference, otp.getAttemptCount(), MAX_ATTEMPTS, correlationId);
            
            auditService.logAction("OTP", otp.getId(), "VERIFY_FAILED_INVALID", "USER_" + userId,
                null, "Invalid OTP code provided",
                "OTP verification failed: Invalid code (attempt " + otp.getAttemptCount() + "/" + MAX_ATTEMPTS + ")", correlationId);
            
            throw new RuntimeException("Invalid OTP code");
        }
        
        // Mark as verified
        otp.setStatus(OneTimePassword.OtpStatus.VERIFIED);
        otp.setVerifiedAt(LocalDateTime.now());
        otp.setVerifiedBy("USER_" + userId);
        otpRepository.save(otp);
        
        log.info("✅ OTP_VERIFIED | UserId: {} | Reference: {} | CorrelationId: {}", 
            userId, reference, correlationId);
        
        // Log audit
        auditService.logAction("OTP", otp.getId(), "VERIFY", "USER_" + userId,
            null, "OTP verified successfully",
            "OTP verified for reference: " + reference, correlationId);
        
        return true;
    }
    
    /**
     * Check if OTP is valid and not used
     */
    public boolean isOtpValid(Long userId, String reference) {
        boolean isValid = otpRepository.findLatestByUserAndReference(userId, reference)
            .map(otp -> {
                // Check status
                if (otp.getStatus() != OneTimePassword.OtpStatus.VERIFIED) {
                    return false;
                }
                // Check not expired
                LocalDateTime expiryTime = otp.getCreatedAt().plusMinutes(EXPIRY_MINUTES);
                return LocalDateTime.now().isBefore(expiryTime);
            })
            .orElse(false);
        
        String correlationId = CorrelationIdHolder.getCorrelationId();
        if (isValid) {
            log.debug("✅ OTP_VALID | UserId: {} | Reference: {} | CorrelationId: {}", 
                userId, reference, correlationId);
        } else {
            log.warn("❌ OTP_INVALID | UserId: {} | Reference: {} | CorrelationId: {}", 
                userId, reference, correlationId);
        }
        
        return isValid;
    }
    
    /**
     * Invalidate OTP after use
     */
    @Transactional
    public void invalidateOtp(Long userId, String reference) {
        otpRepository.findLatestByUserAndReference(userId, reference)
            .ifPresent(otp -> {
                otp.setStatus(OneTimePassword.OtpStatus.EXPIRED);
                otpRepository.save(otp);
                
                String correlationId = CorrelationIdHolder.getCorrelationId();
                log.info("🔄 OTP_INVALIDATED | UserId: {} | Reference: {} | CorrelationId: {}", 
                    userId, reference, correlationId);
                
                auditService.logAction("OTP", otp.getId(), "INVALIDATE", "SYSTEM",
                    "VERIFIED", "EXPIRED",
                    "OTP invalidated after use", correlationId);
            });
    }
}
