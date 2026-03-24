package com.agentic.service;

import com.agentic.dto.AiInsightsResponse.SpendingAlert;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * AI Alerts Service
 * 
 * Generates intelligent spending alerts based on:
 * - Category spending spikes (>30% month-over-month)
 * - High single transactions (>20% of monthly average)
 * - Budget threshold breaches
 * - Spending anomalies and unusual patterns
 * 
 * PHASE 5: Intelligent Alerts
 */
@Service
public class AiAlertsService {

    // Default category budgets (in LKR) - can be customized per user
    private static final Map<String, BigDecimal> DEFAULT_BUDGETS = new HashMap<>();

    static {
        DEFAULT_BUDGETS.put("GROCERIES", new BigDecimal("15000"));
        DEFAULT_BUDGETS.put("RESTAURANTS", new BigDecimal("8000"));
        DEFAULT_BUDGETS.put("TRANSPORT", new BigDecimal("5000"));
        DEFAULT_BUDGETS.put("UTILITIES", new BigDecimal("10000"));
        DEFAULT_BUDGETS.put("ENTERTAINMENT", new BigDecimal("3000"));
        DEFAULT_BUDGETS.put("SHOPPING", new BigDecimal("10000"));
        DEFAULT_BUDGETS.put("HEALTHCARE", new BigDecimal("5000"));
        DEFAULT_BUDGETS.put("INSURANCE", new BigDecimal("8000"));
        DEFAULT_BUDGETS.put("SUBSCRIPTIONS", new BigDecimal("2000"));
        DEFAULT_BUDGETS.put("EDUCATION", new BigDecimal("5000"));
    }

    /**
     * Generate alerts for spending anomalies, spikes, and threshold breaches
     */
    public List<SpendingAlert> generateAlerts(
            Map<String, BigDecimal> currentSpending,
            Map<String, BigDecimal> previousSpending,
            BigDecimal totalCurrentSpend,
            BigDecimal totalPreviousSpend) {

        List<SpendingAlert> alerts = new ArrayList<>();

        // Alert 1: Category Spending Spikes (>30% increase)
        alerts.addAll(detectCategorySpikes(currentSpending, previousSpending));

        // Alert 2: High Single Transaction Alerts (handled at transaction level)
        // Alert 3: Budget Threshold Breaches
        alerts.addAll(detectBudgetBreaches(currentSpending));

        // Alert 4: Sudden spending changes (>50% increase overall)
        alerts.addAll(detectAnomalousTrends(totalCurrentSpend, totalPreviousSpend));

        // Sort by severity (CRITICAL first, then WARNING, then INFO)
        alerts.sort((a, b) -> {
            int severityOrder = getSeverityOrder(b.getSeverity()) - getSeverityOrder(a.getSeverity());
            if (severityOrder != 0) return severityOrder;
            return b.getThresholdExceeded() != null ? 
                b.getThresholdExceeded().compareTo(a.getThresholdExceeded() != null ? a.getThresholdExceeded() : 0) : 0;
        });

        return alerts;
    }

    /**
     * Detect category spending spikes (>30% increase vs previous month)
     */
    private List<SpendingAlert> detectCategorySpikes(
            Map<String, BigDecimal> currentSpending,
            Map<String, BigDecimal> previousSpending) {

        List<SpendingAlert> alerts = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : currentSpending.entrySet()) {
            String category = entry.getKey();
            BigDecimal current = entry.getValue();
            BigDecimal previous = previousSpending.getOrDefault(category, BigDecimal.ZERO);

            // Only alert if there was spending in both periods
            if (previous.compareTo(BigDecimal.ZERO) > 0) {
                double percentChange = current.subtract(previous)
                    .divide(previous, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

                // CRITICAL: >50% spike
                if (percentChange > 50) {
                    SpendingAlert alert = new SpendingAlert();
                    alert.setType("CATEGORY_SPIKE");
                    alert.setSeverity("CRITICAL");
                    alert.setCategory(category);
                    alert.setAmount(current);
                    alert.setThresholdExceeded(percentChange - 50);
                    alert.setMessage(String.format(
                        "🚨 CRITICAL: %s spending spiked %.0f%% - LKR %.0f → LKR %.0f",
                        category, percentChange, previous, current
                    ));
                    alert.setRecommendation(String.format(
                        "Review recent %s transactions for unusual activity or one-time large purchases.",
                        category
                    ));
                    alerts.add(alert);
                }
                // WARNING: 30-50% spike
                else if (percentChange > 30) {
                    SpendingAlert alert = new SpendingAlert();
                    alert.setType("CATEGORY_SPIKE");
                    alert.setSeverity("WARNING");
                    alert.setCategory(category);
                    alert.setAmount(current);
                    alert.setThresholdExceeded(percentChange - 30);
                    alert.setMessage(String.format(
                        "⚠️  WARNING: %s spending increased %.0f%% compared to last month",
                        category, percentChange
                    ));
                    alert.setRecommendation(String.format(
                        "Consider if this increase is expected. Budget more carefully for %s.",
                        category
                    ));
                    alerts.add(alert);
                }
            }
        }

        return alerts;
    }

    /**
     * Detect budget threshold breaches
     */
    private List<SpendingAlert> detectBudgetBreaches(Map<String, BigDecimal> currentSpending) {
        List<SpendingAlert> alerts = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : currentSpending.entrySet()) {
            String category = entry.getKey();
            BigDecimal amount = entry.getValue();

            BigDecimal budget = DEFAULT_BUDGETS.getOrDefault(category, BigDecimal.valueOf(50000));

            if (amount.compareTo(budget) > 0) {
                double percentOverBudget = amount.subtract(budget)
                    .divide(budget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();

                // CRITICAL: >25% over budget
                if (percentOverBudget > 25) {
                    SpendingAlert alert = new SpendingAlert();
                    alert.setType("BUDGET_THRESHOLD");
                    alert.setSeverity("CRITICAL");
                    alert.setCategory(category);
                    alert.setAmount(amount);
                    alert.setThresholdExceeded(percentOverBudget - 25);
                    alert.setMessage(String.format(
                        "🚨 CRITICAL: %s exceeded budget by LKR %.0f (%.0f%% over limit)",
                        category, amount.subtract(budget), percentOverBudget
                    ));
                    alert.setRecommendation(String.format(
                        "Limit %s spending to LKR %.0f per month. Adjust budget or reduce discretionary spending.",
                        category, budget
                    ));
                    alerts.add(alert);
                }
                // WARNING: >10% over budget
                else if (percentOverBudget > 10) {
                    SpendingAlert alert = new SpendingAlert();
                    alert.setType("BUDGET_THRESHOLD");
                    alert.setSeverity("WARNING");
                    alert.setCategory(category);
                    alert.setAmount(amount);
                    alert.setThresholdExceeded(percentOverBudget - 10);
                    alert.setMessage(String.format(
                        "⚠️  WARNING: %s exceeded budget by LKR %.0f",
                        category, amount.subtract(budget)
                    ));
                    alert.setRecommendation(String.format(
                        "Monitor %s spending closely. Budgeted: LKR %.0f, Actual: LKR %.0f",
                        category, budget, amount
                    ));
                    alerts.add(alert);
                }
            }
        }

        return alerts;
    }

    /**
     * Detect anomalous spending trends (sudden overall increases, suspicious patterns)
     */
    private List<SpendingAlert> detectAnomalousTrends(
            BigDecimal totalCurrentSpend,
            BigDecimal totalPreviousSpend) {

        List<SpendingAlert> alerts = new ArrayList<>();

        if (totalPreviousSpend.compareTo(BigDecimal.ZERO) > 0) {
            double percentChange = totalCurrentSpend.subtract(totalPreviousSpend)
                .divide(totalPreviousSpend, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

            // CRITICAL: >75% overall increase
            if (percentChange > 75) {
                SpendingAlert alert = new SpendingAlert();
                alert.setType("ANOMALY");
                alert.setSeverity("CRITICAL");
                alert.setAmount(totalCurrentSpend);
                alert.setThresholdExceeded(percentChange - 75);
                alert.setMessage(String.format(
                    "🚨 CRITICAL: Overall spending spiked %.0f%% - LKR %.0f → LKR %.0f",
                    percentChange, totalPreviousSpend, totalCurrentSpend
                ));
                alert.setRecommendation(
                    "Unusual spending pattern detected. Review all transactions for fraud or unauthorized access."
                );
                alerts.add(alert);
            }
            // WARNING: 50-75% increase
            else if (percentChange > 50) {
                SpendingAlert alert = new SpendingAlert();
                alert.setType("ANOMALY");
                alert.setSeverity("WARNING");
                alert.setAmount(totalCurrentSpend);
                alert.setThresholdExceeded(percentChange - 50);
                alert.setMessage(String.format(
                    "⚠️  WARNING: Overall spending increased %.0f%% this month",
                    percentChange
                ));
                alert.setRecommendation(
                    "Review high-value transactions and verify all charges are authorized."
                );
                alerts.add(alert);
            }
        }

        return alerts;
    }

    /**
     * Helper: Get numeric order for severity levels (for sorting)
     */
    private int getSeverityOrder(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 3;
            case "WARNING" -> 2;
            case "INFO" -> 1;
            default -> 0;
        };
    }

    /**
     * Get default budget for a category
     */
    public BigDecimal getDefaultBudget(String category) {
        return DEFAULT_BUDGETS.getOrDefault(category, new BigDecimal("50000"));
    }

    /**
     * Set custom budget for a category (for future user-customization)
     */
    public void setCustomBudget(String category, BigDecimal amount) {
        DEFAULT_BUDGETS.put(category, amount);
    }
}
