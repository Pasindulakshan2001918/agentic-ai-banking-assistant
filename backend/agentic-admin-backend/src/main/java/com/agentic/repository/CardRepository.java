package com.agentic.repository;

import com.agentic.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for card data.
 * Provides queries for card management and blocking operations.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    
    /**
     * All cards for an account
     */
    List<Card> findByAccountId(Long accountId);

    /**
     * Active cards only — used to check if user has an active card
     */
    List<Card> findByAccountIdAndStatus(Long accountId, Card.CardStatus status);

    /**
     * Find a specific card and verify it belongs to the right account
     */
    Optional<Card> findByIdAndAccountId(Long cardId, Long accountId);
}
