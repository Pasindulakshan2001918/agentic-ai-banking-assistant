package com.agentic.repository;

import com.agentic.entity.OneTimePassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OneTimePasswordRepository extends JpaRepository<OneTimePassword, Long> {
    
    /**
     * Find latest OTP for a user by reference
     */
    @Query("SELECT o FROM OneTimePassword o WHERE o.user.id = :userId AND o.reference = :reference " +
           "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OneTimePassword> findLatestByUserAndReference(@Param("userId") Long userId, 
                                                          @Param("reference") String reference);
    
    /**
     * Find OTP by code and user ID
     */
    Optional<OneTimePassword> findByOtpCodeAndUserId(String otpCode, Long userId);
}
