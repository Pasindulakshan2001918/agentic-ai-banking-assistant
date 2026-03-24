package com.agentic.service;

import com.agentic.entity.Beneficiary;
import com.agentic.entity.Account;
import com.agentic.repository.BeneficiaryRepository;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BeneficiaryService {
    
    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    
    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository,
                             AccountRepository accountRepository,
                             UserRepository userRepository,
                             AuditService auditService) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }
    
    /**
     * Add new beneficiary for a user
     */
    @Transactional
    public Beneficiary addBeneficiary(Long userId, String nickname, String accountHolderName, Long accountId) {
        
        // Verify user exists
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify account exists
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setUser(user);
        beneficiary.setNickname(nickname);
        beneficiary.setAccountHolderName(accountHolderName);
        beneficiary.setAccount(account);
        beneficiary.setStatus(Beneficiary.BeneficiaryStatus.ACTIVE);
        
        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        
        // Log audit
        auditService.logAction("Beneficiary", saved.getId(), "CREATE", "USER_" + userId,
            null, "Beneficiary added: " + nickname,
            "Added beneficiary '" + nickname + "' for account " + account.getAccountNumber());
        
        return saved;
    }
    
    /**
     * Get all beneficiaries for a user (CRITICAL FOR AI)
     */
    public List<Beneficiary> getUserBeneficiaries(Long userId) {
        return beneficiaryRepository.findAllActiveByUserId(userId);
    }
    
    /**
     * Find beneficiary by nickname (AI uses this: "Send to Nimal")
     */
    public Optional<Beneficiary> findByNickname(Long userId, String nickname) {
        return beneficiaryRepository.findByUserAndNickname(userId, nickname);
    }
    
    /**
     * Get beneficiary and verify ownership
     */
    public Beneficiary getBeneficiary(Long beneficiaryId, Long userId) {
        return beneficiaryRepository.findByIdAndUserId(beneficiaryId, userId)
            .orElseThrow(() -> new RuntimeException("Beneficiary not found or unauthorized access"));
    }
    
    /**
     * Update beneficiary
     */
    @Transactional
    public Beneficiary updateBeneficiary(Long beneficiaryId, Long userId, String nickname, 
                                        String accountHolderName) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId, userId);
        
        beneficiary.setNickname(nickname);
        beneficiary.setAccountHolderName(accountHolderName);
        
        Beneficiary updated = beneficiaryRepository.save(beneficiary);
        
        // Log audit
        auditService.logAction("Beneficiary", updated.getId(), "UPDATE", "USER_" + userId,
            null, "Beneficiary updated: " + nickname, 
            "Updated beneficiary information");
        
        return updated;
    }
    
    /**
     * Delete/Deactivate beneficiary
     */
    @Transactional
    public void deleteBeneficiary(Long beneficiaryId, Long userId) {
        Beneficiary beneficiary = getBeneficiary(beneficiaryId, userId);
        
        beneficiary.setStatus(Beneficiary.BeneficiaryStatus.INACTIVE);
        beneficiaryRepository.save(beneficiary);
        
        // Log audit
        auditService.logAction("Beneficiary", beneficiary.getId(), "DELETE", "USER_" + userId,
            null, "Beneficiary deleted: " + beneficiary.getNickname(),
            "Deactivated beneficiary account");
    }
}
