package com.lunorlabs.bank.policy;

import com.lunorlabs.bank.domain.ProductRule;

import java.math.BigDecimal;

/**
 * Everything the engine needs, already loaded. The engine itself does no I/O,
 * which makes every rule unit-testable in milliseconds.
 *
 * @param rule            matched product-matrix row, or null if none exists
 * @param availableBalance balance of the debited account, or null if nothing is debited
 * @param newPayee        true for a brand-new destination or one inside the cooling window
 * @param customerMaxDaily per-customer override of the daily cap, or null
 */
public record PolicyInput(ProductRule rule, BigDecimal amount, BigDecimal availableBalance,
                          boolean accountActive, boolean newPayee, BigDecimal usedToday,
                          BigDecimal customerMaxDaily) {}
