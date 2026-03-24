package com.agentic.config;

import com.agentic.entity.Account;
import com.agentic.entity.Card;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.CardRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * SEED DATA: Card Initialization
 * 
 * On application startup, creates demo cards for testing.
 * Only creates cards if none exist.
 */
@Component
public class CardDataInitializer implements ApplicationRunner {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    public CardDataInitializer(CardRepository cardRepository,
                               AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Only initialize if no cards exist
        if (cardRepository.count() > 0) {
            return;
        }

        System.out.println("🏦 Initializing demo cards...");

        // Get account for user ID 1 (or create test setup)
        Account account = accountRepository.findPrimaryByUserId(1L).orElse(null);
        if (account == null) {
            System.out.println("⚠️  Account not found for userId=1L. Skipping card initialization.");
            return;
        }

        // Create demo DEBIT card
        Card demoCard = new Card();
        demoCard.setAccount(account);
        demoCard.setCardType(Card.CardType.DEBIT);
        demoCard.setStatus(Card.CardStatus.ACTIVE);
        demoCard.setMaskedCardNumber("****-****-****-4521");
        demoCard.setLastFourDigits("4521");
        demoCard.setExpiryDate(LocalDate.of(2027, 12, 31));
        demoCard.setOnlinePaymentsEnabled(true);
        demoCard.setContactlessEnabled(true);
        demoCard.setBlockReason(null);

        cardRepository.save(demoCard);
        System.out.println("✅ Demo card created: ****-4521 (DEBIT, ACTIVE)");
    }
}
