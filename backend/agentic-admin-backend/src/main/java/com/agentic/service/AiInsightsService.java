package com.agentic.service;

import com.agentic.dto.AiInsightsResponse;
import com.agentic.dto.AiInsightsResponse.*;
import com.agentic.repository.TransactionRepository;
import com.agentic.entity.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Insights Service
 * 
 * Orchestrates comprehensive spending insights with:
 * 1. Categorization - Merchant → Category mapping
 * 2. Comparison - Month-over-month percentage changes
 * 3. Alerts - Intelligent threshold-based warnings
 * 
 * PHASE 5: AI Insights Upgrade
 */
@Service
public class AiInsightsService {

    private final TransactionRepository transactionRepository;
    private final MerchantCategoryService merchantCategoryService;
    private final AiAlertsService aiAlertsService;

    public AiInsightsService(TransactionRepository transactionRepository,
                              MerchantCategoryService merchantCategoryService,
                              AiAlertsService aiAlertsService) {
        this.transactionRepository = transactionRepository;
        this.merchantCategoryService = merchantCategoryService;
        this.aiAlertsService = aiAlertsService;
    }

    /**
     * Generate comprehensive AI Insights for a given month
     * Includes categorization, comparisons, and alerts
     * 
     * @param accountId Account to analyze
     * @param year Year of analysis
     * @param month Month of analysis (1-12)
     * @return Comprehensive AiInsightsResponse
     */
    public AiInsightsResponse generateMonthlyInsights(Long accountId, int year, int month) {
        try {
            AiInsightsResponse response = new AiInsightsResponse();
            response.setYear(year);
            response.setMonth(month);
            response.setAnalysisTimestamp(LocalDateTime.now().toString());

            // 1. CATEGORIZATION: Build merchant mappings and category breakdown
            YearMonth currentMonth = YearMonth.of(year, month);
            Map<String, CategoryInsight> categorization = buildCategorization(accountId, currentMonth);
            response.setCategorization(categorization);

            // 2. Build merchant → category mappings
            List<MerchantCategoryMapping> merchantMappings = buildMerchantMappings(accountId, currentMonth);
            response.setMerchantMappings(merchantMappings);

            // 3. Calculate total spending
            BigDecimal totalSpend = categorization.values().stream()
                .map(CategoryInsight::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.setTotalSpending(totalSpend);

            // 4. COMPARISON: Month-over-month analysis
            Comparison comparison = buildComparison(accountId, currentMonth);
            response.setComparison(comparison);
            response.setPreviousMonthTotal(comparison.getPreviousMonthAmount());

            // 5. ALERTS: Generate intelligent alerts and warnings
            Map<String, BigDecimal> curryCategoryMap = categorization.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getTotalAmount()));
            
            Map<String, BigDecimal> prevCategoryMap = buildCategoryMap(
                accountId, 
                currentMonth.minusMonths(1)
            );

            List<SpendingAlert> alerts = aiAlertsService.generateAlerts(
                curryCategoryMap,
                prevCategoryMap,
                totalSpend,
                comparison.getPreviousMonthAmount()
            );
            response.setAlerts(alerts);

            // 6. Generate recommendations based on insights
            List<String> recommendations = generateRecommendations(categorization, alerts, totalSpend);
            response.setRecommendations(recommendations);

            // 7. Determine overall analysis status
            String status = determineAnalysisStatus(alerts);
            response.setAnalysisStatus(status);

            return response;

        } catch (Exception e) {
            // Fallback response on error
            return createErrorResponse(year, month, e);
        }
    }

    /**
     * Build categorized spending breakdown by merchant categorization
     */
    private Map<String, CategoryInsight> buildCategorization(Long accountId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findSpendingByAccountAndPeriod(accountId, start, end);
        Map<String, List<Transaction>> byCategory = new HashMap<>();

        // Group transactions by category based on merchant
        for (Transaction trans : transactions) {
            String merchant = trans.getMerchantName() != null ? trans.getMerchantName() : "Unknown";
            MerchantCategoryService.SpendingCategory category = 
                merchantCategoryService.categorizeMerchant(merchant);
            
            byCategory.computeIfAbsent(category.name(), k -> new ArrayList<>()).add(trans);
        }

        // Build CategoryInsight objects
        BigDecimal totalSpend = transactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, CategoryInsight> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Transaction>> entry : byCategory.entrySet()) {
            String categoryName = entry.getKey();
            List<Transaction> catTransactions = entry.getValue();

            BigDecimal categoryTotal = catTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            Double percentOfTotal = totalSpend.compareTo(BigDecimal.ZERO) > 0 ?
                categoryTotal.divide(totalSpend, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue() : 0.0;

            BigDecimal avgTransaction = new BigDecimal(catTransactions.size()).compareTo(BigDecimal.ZERO) > 0 ?
                categoryTotal.divide(new BigDecimal(catTransactions.size()), 2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;

            BigDecimal maxTransaction = catTransactions.stream()
                .map(Transaction::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

            List<String> topMerchants = catTransactions.stream()
                .collect(Collectors.groupingBy(
                    Transaction::getMerchantName,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            CategoryInsight insight = new CategoryInsight(categoryName, categoryTotal, percentOfTotal);
            insight.setTransactionCount(catTransactions.size());
            insight.setAverageTransaction(avgTransaction);
            insight.setHighestTransaction(maxTransaction);
            insight.setTopMerchants(topMerchants);

            result.put(categoryName, insight);
        }

        return result;
    }

    /**
     * Build merchant → category mappings with frequency and amounts
     */
    private List<MerchantCategoryMapping> buildMerchantMappings(Long accountId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findSpendingByAccountAndPeriod(accountId, start, end);

        return transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getMerchantName,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ))
            .entrySet().stream()
            .map(entry -> {
                String merchant = entry.getKey() != null ? entry.getKey() : "Unknown";
                BigDecimal total = entry.getValue();
                
                long frequency = transactions.stream()
                    .filter(t -> {
                        String tMerchant = t.getMerchantName() != null ? t.getMerchantName() : "Unknown";
                        return tMerchant.equals(merchant);
                    })
                    .count();

                MerchantCategoryService.SpendingCategory category = 
                    merchantCategoryService.categorizeMerchant(merchant);

                return new MerchantCategoryMapping(
                    merchant,
                    category.name(),
                    (int) frequency,
                    total
                );
            })
            .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
            .limit(20)  // Top 20 merchants
            .collect(Collectors.toList());
    }

    /**
     * Build category map for a given month
     */
    private Map<String, BigDecimal> buildCategoryMap(Long accountId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findSpendingByAccountAndPeriod(accountId, start, end);
        Map<String, BigDecimal> categoryMap = new HashMap<>();

        for (Transaction trans : transactions) {
            String merchant = trans.getMerchantName() != null ? trans.getMerchantName() : "Unknown";
            MerchantCategoryService.SpendingCategory category = 
                merchantCategoryService.categorizeMerchant(merchant);
            
            categoryMap.merge(category.name(), trans.getAmount(), BigDecimal::add);
        }

        return categoryMap;
    }

    /**
     * Build month-over-month comparison with trend analysis
     */
    private Comparison buildComparison(Long accountId, YearMonth currentMonth) {
        Comparison comparison = new Comparison();

        // Get previous month data
        YearMonth prevMonth = currentMonth.minusMonths(1);
        LocalDateTime prevStart = prevMonth.atDay(1).atStartOfDay();
        LocalDateTime prevEnd = prevMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal prevTotal = transactionRepository.findSpendingByAccountAndPeriod(accountId, prevStart, prevEnd)
            .stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        comparison.setPreviousMonthAmount(prevTotal);

        // Calculate current month total
        LocalDateTime currStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime currEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal currTotal = transactionRepository.findSpendingByAccountAndPeriod(accountId, currStart, currEnd)
            .stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate percentage change
        double percentChange = 0.0;
        String trend = "STABLE";

        if (prevTotal.compareTo(BigDecimal.ZERO) > 0) {
            percentChange = currTotal.subtract(prevTotal)
                .divide(prevTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

            if (percentChange > 5) {
                trend = "UP";
            } else if (percentChange < -5) {
                trend = "DOWN";
            }
        }

        comparison.setPercentChange(percentChange);
        comparison.setTrend(trend);

        // Build category-level comparisons
        Map<String, Double> categoryChanges = new HashMap<>();
        Map<String, BigDecimal> currCategoryMap = buildCategoryMap(accountId, currentMonth);
        Map<String, BigDecimal> prevCategoryMap = buildCategoryMap(accountId, prevMonth);

        for (String category : currCategoryMap.keySet()) {
            BigDecimal curr = currCategoryMap.get(category);
            BigDecimal prev = prevCategoryMap.getOrDefault(category, BigDecimal.ZERO);

            double change = prev.compareTo(BigDecimal.ZERO) > 0 ?
                curr.subtract(prev).divide(prev, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue() : 100.0;

            categoryChanges.put(category, change);
        }

        comparison.setCategoryChanges(categoryChanges);

        // Generate human-readable interpretation
        String interpretation = String.format(
            "Spending was %.0f%% %s compared to %s. %s month historically.",
            Math.abs(percentChange), 
            trend.equals("UP") ? "higher" : trend.equals("DOWN") ? "lower" : "stable",
            prevMonth,
            trend.equals("UP") ? "Higher" : "Lower"
        );
        comparison.setInterpretation(interpretation);

        return comparison;
    }

    /**
     * Generate AI-friendly recommendations based on insights
     */
    private List<String> generateRecommendations(Map<String, CategoryInsight> categorization,
                                                   List<SpendingAlert> alerts,
                                                   BigDecimal totalSpend) {
        List<String> recommendations = new ArrayList<>();

        // Recommendation 1: Biggest spending category
        if (!categorization.isEmpty()) {
            CategoryInsight maxCategory = categorization.values().stream()
                .max(Comparator.comparing(CategoryInsight::getTotalAmount))
                .orElse(null);

            if (maxCategory != null && maxCategory.getPercentOfTotal() > 30) {
                recommendations.add(String.format(
                    "💡 %s is your largest expense category (%.0f%% of budget). Consider if this is necessary.",
                    maxCategory.getCategory(), maxCategory.getPercentOfTotal()
                ));
            }
        }

        // Recommendation 2: Alert-based actions
        long criticalAlerts = alerts.stream().filter(a -> "CRITICAL".equals(a.getSeverity())).count();
        long warningAlerts = alerts.stream().filter(a -> "WARNING".equals(a.getSeverity())).count();

        if (criticalAlerts > 0) {
            recommendations.add(String.format(
                "🚨 You have %d critical spending alerts. Review them immediately to prevent budget overruns.",
                criticalAlerts
            ));
        } else if (warningAlerts > 0) {
            recommendations.add(String.format(
                "⚠️  You have %d spending warnings. Consider adjusting your budget allocation.",
                warningAlerts
            ));
        } else {
            recommendations.add("✅ Great job! Your spending is within normal ranges this month.");
        }

        // Recommendation 3: Savings opportunity
        BigDecimal highSpending = categorization.values().stream()
            .filter(c -> c.getPercentOfTotal() > 25)
            .map(CategoryInsight::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (highSpending.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal potential = highSpending.multiply(BigDecimal.valueOf(0.1));  // 10% savings
            recommendations.add(String.format(
                "💰 Potential savings: Reducing high-value categories by 10%% could save ~LKR %.0f",
                potential
            ));
        }

        return recommendations;
    }

    /**
     * Determine overall analysis status based on alert severity
     */
    private String determineAnalysisStatus(List<SpendingAlert> alerts) {
        if (alerts.isEmpty()) {
            return "NORMAL";
        }

        boolean hasCritical = alerts.stream().anyMatch(a -> "CRITICAL".equals(a.getSeverity()));
        if (hasCritical) {
            return "CRITICAL";
        }

        boolean hasWarning = alerts.stream().anyMatch(a -> "WARNING".equals(a.getSeverity()));
        return hasWarning ? "CAUTION" : "NORMAL";
    }

    /**
     * Create error response for failed analysis
     */
    private AiInsightsResponse createErrorResponse(int year, int month, Exception e) {
        AiInsightsResponse response = new AiInsightsResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setAnalysisStatus("ERROR");
        response.setAnalysisTimestamp(LocalDateTime.now().toString());
        response.setTotalSpending(BigDecimal.ZERO);
        response.setCategorization(new HashMap<>());
        response.setMerchantMappings(new ArrayList<>());
        response.setAlerts(Collections.singletonList(
            new SpendingAlert("ERROR", "CRITICAL", "Failed to generate insights: " + e.getMessage())
        ));
        return response;
    }
}
