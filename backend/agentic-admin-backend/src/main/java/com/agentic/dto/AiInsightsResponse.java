package com.agentic.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI Insights Response DTO
 * 
 * Comprehensive spending insights with categorization, comparisons, and alerts.
 * Designed for AI layer consumption with structured, actionable data.
 * 
 * PHASE 5: AI Insights Upgrade
 * - Categorization: Merchant → Category mapping
 * - Comparison: Month-over-month % change
 * - Alerts: Intelligent spending threshold warnings
 */
public class AiInsightsResponse {

    // Period Information
    private int year;
    private int month;
    
    // Summary Metrics
    private BigDecimal totalSpending;
    private BigDecimal previousMonthTotal;
    
    // Categorization
    private Map<String, CategoryInsight> categorization;  // category → detailed breakdown
    private List<MerchantCategoryMapping> merchantMappings;  // merchant → category mappings
    
    // Comparison Data (Month-over-Month)
    private Comparison comparison;
    
    // Alerts & Warnings
    private List<SpendingAlert> alerts;
    private List<String> recommendations;
    
    // Metadata
    private String analysisTimestamp;
    private String analysisStatus;  // "NORMAL", "CAUTION", "WARNING", "CRITICAL"

    // ===== INNER CLASSES =====

    /**
     * Detailed breakdown for each spending category
     */
    public static class CategoryInsight {
        private String category;
        private BigDecimal totalAmount;
        private Double percentOfTotal;
        private Integer transactionCount;
        private BigDecimal averageTransaction;
        private BigDecimal highestTransaction;
        private List<String> topMerchants;

        public CategoryInsight() {}
        public CategoryInsight(String category, BigDecimal totalAmount, Double percentOfTotal) {
            this.category = category;
            this.totalAmount = totalAmount;
            this.percentOfTotal = percentOfTotal;
        }

        // Getters and Setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public Double getPercentOfTotal() { return percentOfTotal; }
        public void setPercentOfTotal(Double percentOfTotal) { this.percentOfTotal = percentOfTotal; }
        public Integer getTransactionCount() { return transactionCount; }
        public void setTransactionCount(Integer transactionCount) { this.transactionCount = transactionCount; }
        public BigDecimal getAverageTransaction() { return averageTransaction; }
        public void setAverageTransaction(BigDecimal averageTransaction) { this.averageTransaction = averageTransaction; }
        public BigDecimal getHighestTransaction() { return highestTransaction; }
        public void setHighestTransaction(BigDecimal highestTransaction) { this.highestTransaction = highestTransaction; }
        public List<String> getTopMerchants() { return topMerchants; }
        public void setTopMerchants(List<String> topMerchants) { this.topMerchants = topMerchants; }
    }

    /**
     * Merchant to Category Mapping
     */
    public static class MerchantCategoryMapping {
        private String merchant;
        private String category;
        private Integer frequency;  // times appeared this month
        private BigDecimal totalAmount;

        public MerchantCategoryMapping() {}
        public MerchantCategoryMapping(String merchant, String category, Integer frequency, BigDecimal totalAmount) {
            this.merchant = merchant;
            this.category = category;
            this.frequency = frequency;
            this.totalAmount = totalAmount;
        }

        public String getMerchant() { return merchant; }
        public void setMerchant(String merchant) { this.merchant = merchant; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Integer getFrequency() { return frequency; }
        public void setFrequency(Integer frequency) { this.frequency = frequency; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    }

    /**
     * Month-over-Month Comparison
     */
    public static class Comparison {
        private BigDecimal previousMonthAmount;
        private Double percentChange;  // (current - previous) / previous * 100
        private String trend;  // "UP", "DOWN", "STABLE"
        private Map<String, Double> categoryChanges;  // category → % change
        private String interpretation;  // human-readable summary

        public Comparison() {}
        public Comparison(BigDecimal previousMonthAmount, Double percentChange, String trend) {
            this.previousMonthAmount = previousMonthAmount;
            this.percentChange = percentChange;
            this.trend = trend;
        }

        public BigDecimal getPreviousMonthAmount() { return previousMonthAmount; }
        public void setPreviousMonthAmount(BigDecimal previousMonthAmount) { this.previousMonthAmount = previousMonthAmount; }
        public Double getPercentChange() { return percentChange; }
        public void setPercentChange(Double percentChange) { this.percentChange = percentChange; }
        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
        public Map<String, Double> getCategoryChanges() { return categoryChanges; }
        public void setCategoryChanges(Map<String, Double> categoryChanges) { this.categoryChanges = categoryChanges; }
        public String getInterpretation() { return interpretation; }
        public void setInterpretation(String interpretation) { this.interpretation = interpretation; }
    }

    /**
     * Spending Alert with severity level
     */
    public static class SpendingAlert {
        private String type;  // "CATEGORY_SPIKE", "HIGH_SINGLE_TRANSACTION", "BUDGET_THRESHOLD", "ANOMALY"
        private String severity;  // "INFO", "WARNING", "CRITICAL"
        private String category;
        private BigDecimal amount;
        private String message;
        private String recommendation;
        private Double thresholdExceeded;  // % over threshold

        public SpendingAlert() {}
        public SpendingAlert(String type, String severity, String message) {
            this.type = type;
            this.severity = severity;
            this.message = message;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
        public Double getThresholdExceeded() { return thresholdExceeded; }
        public void setThresholdExceeded(Double thresholdExceeded) { this.thresholdExceeded = thresholdExceeded; }
    }

    // ===== MAIN CLASS GETTERS AND SETTERS =====

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    
    public BigDecimal getTotalSpending() { return totalSpending; }
    public void setTotalSpending(BigDecimal totalSpending) { this.totalSpending = totalSpending; }
    
    public BigDecimal getPreviousMonthTotal() { return previousMonthTotal; }
    public void setPreviousMonthTotal(BigDecimal previousMonthTotal) { this.previousMonthTotal = previousMonthTotal; }
    
    public Map<String, CategoryInsight> getCategorization() { return categorization; }
    public void setCategorization(Map<String, CategoryInsight> categorization) { this.categorization = categorization; }
    
    public List<MerchantCategoryMapping> getMerchantMappings() { return merchantMappings; }
    public void setMerchantMappings(List<MerchantCategoryMapping> merchantMappings) { this.merchantMappings = merchantMappings; }
    
    public Comparison getComparison() { return comparison; }
    public void setComparison(Comparison comparison) { this.comparison = comparison; }
    
    public List<SpendingAlert> getAlerts() { return alerts; }
    public void setAlerts(List<SpendingAlert> alerts) { this.alerts = alerts; }
    
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    
    public String getAnalysisTimestamp() { return analysisTimestamp; }
    public void setAnalysisTimestamp(String analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }
    
    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
}
