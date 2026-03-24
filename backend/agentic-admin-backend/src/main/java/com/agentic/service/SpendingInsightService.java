package com.agentic.service;

import com.agentic.dto.SpendingInsightResponse;
import com.agentic.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SERVICE: Spending Insights and Analysis
 * 
 * Provides monthly spending breakdown with category totals,
 * month-over-month change percentages, and budget warnings.
 */
@Service
public class SpendingInsightService {

    private final TransactionRepository transactionRepository;

    public SpendingInsightService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Returns a full monthly spending breakdown with category totals,
     * month-over-month change percentages, and budget warnings.
     */
    public SpendingInsightResponse getMonthlySummary(Long accountId, int year, int month) {

        // Build date range for requested month
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end   = start.plusMonths(1).minusNanos(1);

        // Build date range for previous month (for comparison)
        LocalDateTime prevStart = start.minusMonths(1);
        LocalDateTime prevEnd   = start.minusNanos(1);

        // Fetch category sums for both months
        Map<String, BigDecimal> current  = aggregateByCategory(accountId, start, end);
        Map<String, BigDecimal> previous = aggregateByCategory(accountId, prevStart, prevEnd);

        // Calculate total spend this month
        BigDecimal totalSpend = current.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate % change per category vs last month
        Map<String, Double> changePercent = new LinkedHashMap<>();
        for (String category : current.keySet()) {
            BigDecimal curr = current.get(category);
            BigDecimal prev = previous.getOrDefault(category, BigDecimal.ZERO);
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                double change = curr.subtract(prev)
                    .divide(prev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
                changePercent.put(category, Math.round(change * 10.0) / 10.0);
            } else {
                changePercent.put(category, 100.0); // new category this month
            }
        }

        // Generate warnings for significant increases
        List<String> warnings = generateWarnings(current, previous, totalSpend);

        SpendingInsightResponse response = new SpendingInsightResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setTotalSpend(totalSpend);
        response.setByCategory(current);
        response.setPreviousMonth(previous);
        response.setChangePercent(changePercent);
        response.setWarnings(warnings);
        return response;
    }

    /**
     * Converts raw Object[] rows from the GROUP BY query into a readable map.
     */
    private Map<String, BigDecimal> aggregateByCategory(Long accountId,
                                                         LocalDateTime start,
                                                         LocalDateTime end) {
        List<Object[]> rows = transactionRepository.sumByCategory(accountId, start, end);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String category = row[0].toString();
            BigDecimal amount = (BigDecimal) row[1];
            result.put(category, amount);
        }
        return result;
    }

    /**
     * Produces human-readable warning strings for the AI to include in its reply.
     */
    private List<String> generateWarnings(Map<String, BigDecimal> current,
                                           Map<String, BigDecimal> previous,
                                           BigDecimal totalSpend) {
        List<String> warnings = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : current.entrySet()) {
            String category = entry.getKey();
            BigDecimal curr = entry.getValue();
            BigDecimal prev = previous.getOrDefault(category, BigDecimal.ZERO);

            // Warn if a category jumped more than 30% vs last month
            if (prev.compareTo(BigDecimal.ZERO) > 0) {
                double change = curr.subtract(prev)
                    .divide(prev, 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
                if (change > 30) {
                    warnings.add(String.format(
                        "%s spending is up %.0f%% compared to last month (LKR %.0f → LKR %.0f)",
                        category, change, prev, curr
                    ));
                }
            }

            // Warn if a single category exceeds 50% of total monthly spend
            if (totalSpend.compareTo(BigDecimal.ZERO) > 0) {
                double share = curr.divide(totalSpend, 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
                if (share > 50) {
                    warnings.add(String.format(
                        "%s accounts for %.0f%% of your total spending this month",
                        category, share
                    ));
                }
            }
        }
        return warnings;
    }
}
