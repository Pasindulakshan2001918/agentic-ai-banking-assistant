package com.agentic.ai.service;

import com.agentic.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("SELECT c FROM Conversation c WHERE c.user.id = :userId AND c.status = 'ACTIVE' ORDER BY c.updatedAt DESC")
    Optional<Conversation> findByUserIdAndActiveTrue(@Param("userId") Long userId);
}
