package com.agentic.service;

import com.agentic.dto.BeneficiaryResponse;
import com.agentic.entity.Account;
import com.agentic.entity.Beneficiary;
import com.agentic.repository.BeneficiaryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BENEFICIARY TRANSFER SERVICE
 * Resolves beneficiary nicknames to accounts for transfers
 * 
 * CRITICAL FOR AI: "Send to Nimal" → Account ID
 */
@Service
public class BeneficiaryTransferService {
    
    private final BeneficiaryRepository beneficiaryRepository;
    
    public BeneficiaryTransferService(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }
    
    /**
     * Find beneficiary by nickname and return their account
     * Used by AI to resolve: "Send 5000 to Nimal"
     */
    public Account resolveBeneficiaryToAccount(Long userId, String nickname) {
        Beneficiary beneficiary = beneficiaryRepository
            .findByUserAndNickname(userId, nickname)
            .orElseThrow(() -> new RuntimeException(
                "Beneficiary '" + nickname + "' not found. Available beneficiaries: " + 
                listBeneficiaryNicknames(userId)));
        
        return beneficiary.getAccount();
    }
    
    /**
     * Get list of beneficiary nicknames for a user
     */
    public List<String> listBeneficiaryNicknames(Long userId) {
        return beneficiaryRepository.findAllActiveByUserId(userId)
            .stream()
            .map(Beneficiary::getNickname)
            .collect(Collectors.toList());
    }
    
    /**
     * Get detailed beneficiary info for a user
     */
    public List<BeneficiaryResponse> listUserBeneficiaries(Long userId) {
        return beneficiaryRepository.findAllActiveByUserId(userId)
            .stream()
            .map(BeneficiaryResponse::new)
            .collect(Collectors.toList());
    }
    
    /**
     * Verify beneficiary exists and is active
     */
    public boolean beneficiaryExists(Long userId, String nickname) {
        return beneficiaryRepository.findByUserAndNickname(userId, nickname).isPresent();
    }
}
