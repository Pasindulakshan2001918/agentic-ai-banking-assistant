package com.agentic.ai.service;

import com.agentic.entity.Conversation;
import com.agentic.entity.Conversation.ConversationStatus;
import com.agentic.entity.User;
import com.agentic.exception.UnauthorizedException;
import com.agentic.exception.ValidationException;
import com.agentic.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ConversationManager {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    public ConversationManager(ConversationRepository conversationRepository,
                              UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Conversation getOrCreateConversation(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return conversationRepository.findByUserIdAndActiveTrue(userId)
            .orElseGet(() -> {
                Conversation conv = new Conversation();
                conv.setUser(user);
                conv.setStatus(ConversationStatus.INITIATED);
                return conversationRepository.save(conv);
            });
    }

    @Transactional
    public void endConversation(Long conversationId) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            conv.setStatus(ConversationStatus.COMPLETED);
            conversationRepository.save(conv);
        });
    }
}

