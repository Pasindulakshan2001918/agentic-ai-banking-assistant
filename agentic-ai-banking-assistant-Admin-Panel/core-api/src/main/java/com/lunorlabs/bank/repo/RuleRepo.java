package com.lunorlabs.bank.repo;

import com.lunorlabs.bank.domain.ProductRule;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Reads the product matrix, per-customer overrides and today's usage. Data only; no decisions here. */
@Repository
public class RuleRepo {
    private static final RowMapper<ProductRule> MAP = (rs, i) -> new ProductRule(
            rs.getString("action_type"), rs.getString("source_product"), rs.getString("rail"),
            rs.getBoolean("enabled"), rs.getBigDecimal("min_amount"), rs.getBigDecimal("max_per_txn"),
            rs.getBigDecimal("max_daily"), rs.getBigDecimal("otp_threshold_per_txn"),
            rs.getBigDecimal("otp_threshold_daily"), rs.getBoolean("always_otp"),
            rs.getInt("new_payee_cooling_hrs"), rs.getBigDecimal("fee_flat"), rs.getBigDecimal("fee_pct"));

    private final JdbcClient jdbc;

    public RuleRepo(JdbcClient jdbc) { this.jdbc = jdbc; }

    /** Exact source product wins over the 'ANY' row. */
    public Optional<ProductRule> find(String actionType, String sourceProduct, String rail) {
        return jdbc.sql("""
                SELECT * FROM product_rules
                WHERE action_type = :a AND rail = :r AND source_product IN (:sp, 'ANY')
                ORDER BY (source_product = 'ANY')
                LIMIT 1
                """)
                .param("a", actionType).param("r", rail).param("sp", sourceProduct)
                .query(MAP).optional();
    }

    public Optional<BigDecimal> customerMaxDaily(UUID customerId, String actionType) {
        return jdbc.sql("SELECT max_daily FROM customer_limits WHERE customer_id = :c AND action_type = :a")
                .param("c", customerId).param("a", actionType)
                .query(BigDecimal.class).optional();
    }

    public BigDecimal usedToday(UUID customerId, String actionType) {
        return jdbc.sql("SELECT COALESCE((SELECT used_today FROM v_daily_usage "
                        + "WHERE customer_id = :c AND action_type = :a), 0)")
                .param("c", customerId).param("a", actionType)
                .query(BigDecimal.class).single();
    }
}
