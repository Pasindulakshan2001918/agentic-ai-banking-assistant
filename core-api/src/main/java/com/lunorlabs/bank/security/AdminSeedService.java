package com.lunorlabs.bank.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminSeedService implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeedService.class);
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

    private final JdbcClient jdbc;

    public AdminSeedService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            int count = jdbc.sql("SELECT COUNT(*) FROM admin_accounts WHERE lower(email) = 'admin@bank.com'")
                    .query(Integer.class).single();
            if (count == 0) {
                String hash = BCRYPT.encode("Admin@1234");
                jdbc.sql("INSERT INTO admin_accounts (id, email, full_name, password_hash, role, permissions, capabilities, status) " +
                         "VALUES (?, 'admin@bank.com', 'System Administrator', ?, 'SUPER_ADMIN', ARRAY['*']::text[], ARRAY['SYSTEM_ADMIN']::text[], 'ACTIVE')")
                        .param(1, UUID.randomUUID())
                        .param(2, hash)
                        .update();
                log.info("Default super-admin created: admin@bank.com / Admin@1234");
            }
        } catch (Exception e) {
            log.warn("Could not seed default admin: {}", e.getMessage());
        }
    }
}
