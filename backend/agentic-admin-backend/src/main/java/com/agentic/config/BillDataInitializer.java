package com.agentic.config;

import com.agentic.entity.BillProvider;
import com.agentic.entity.BillType;
import com.agentic.repository.BillProviderRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DATA INITIALIZER: Seed bills for demonstration.
 * 
 * Creates sample bill data when application starts.
 * In production, this would be replaced by real utility API integration.
 * 
 * Only seeds if no bills exist in database.
 */
@Component
public class BillDataInitializer implements ApplicationRunner {

    private final BillProviderRepository billProviderRepository;

    public BillDataInitializer(BillProviderRepository billProviderRepository) {
        this.billProviderRepository = billProviderRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Only seed if no bills exist
        if (billProviderRepository.count() > 0) {
            System.out.println("Bill data already exists, skipping initialization");
            return;
        }

        System.out.println("Initializing sample bill data...");

        Long demoUserId = 1L; // your test user

        // CEB Electricity Bill
        BillProvider ceb = new BillProvider();
        ceb.setUserId(demoUserId);
        ceb.setBillType(BillType.ELECTRICITY);
        ceb.setProviderName("CEB");
        ceb.setAccountReference("1234567890");
        ceb.setOutstandingAmount(new BigDecimal("4200.00"));
        ceb.setDueDate(LocalDate.now().plusDays(7));
        ceb.setStatus(BillProvider.BillStatus.PENDING);

        // NWSDB Water Bill
        BillProvider nwsdb = new BillProvider();
        nwsdb.setUserId(demoUserId);
        nwsdb.setBillType(BillType.WATER);
        nwsdb.setProviderName("NWSDB");
        nwsdb.setAccountReference("WB-9988776");
        nwsdb.setOutstandingAmount(new BigDecimal("1850.00"));
        nwsdb.setDueDate(LocalDate.now().plusDays(14));
        nwsdb.setStatus(BillProvider.BillStatus.PENDING);

        // Mobitel Mobile Recharge
        BillProvider mobitel = new BillProvider();
        mobitel.setUserId(demoUserId);
        mobitel.setBillType(BillType.MOBILE_RECHARGE);
        mobitel.setProviderName("Mobitel");
        mobitel.setAccountReference("0771234567");
        mobitel.setOutstandingAmount(new BigDecimal("500.00")); // suggested top-up
        mobitel.setDueDate(LocalDate.now());
        mobitel.setStatus(BillProvider.BillStatus.PENDING);

        billProviderRepository.saveAll(List.of(ceb, nwsdb, mobitel));
        
        System.out.println("Successfully initialized 3 sample bills (CEB, NWSDB, Mobitel)");
    }
}
