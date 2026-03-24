package com.agentic.repository;

import com.agentic.entity.Beneficiary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    
    /**
     * Find all active beneficiaries for a user
     */
    @Query("SELECT b FROM Beneficiary b WHERE b.user.id = :userId AND b.status = 'ACTIVE' " +
           "ORDER BY b.createdAt DESC")
    List<Beneficiary> findAllActiveByUserId(@Param("userId") Long userId);
    
    /**
     * Find beneficiary by nickname and user ID (for AI: "Send to Nimal")
     */
    @Query("SELECT b FROM Beneficiary b WHERE b.user.id = :userId AND b.nickname = :nickname " +
           "AND b.status = 'ACTIVE'")
    Optional<Beneficiary> findByUserAndNickname(@Param("userId") Long userId, 
                                               @Param("nickname") String nickname);
    
    /**
     * Find beneficiary by ID and verify ownership
     */
    @Query("SELECT b FROM Beneficiary b WHERE b.id = :id AND b.user.id = :userId")
    Optional<Beneficiary> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    
    /**
     * Paginated list of beneficiaries for a user
     */
    Page<Beneficiary> findByUserId(Long userId, Pageable pageable);
}
