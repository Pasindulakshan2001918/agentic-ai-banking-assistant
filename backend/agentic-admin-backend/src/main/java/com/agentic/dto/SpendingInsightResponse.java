package com.agentic.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for monthly spending insights and analysis.
 * Includes category breakdown, month-over-month comparisons, and budget warnings.
 */
public class SpendingInsightResponse {

    private int year;
    private int month;
    private BigDecimal totalSpend;
    private Map<String, BigDecimal> byCategory;
    private Map<String, BigDecimal> previousMonth;      // for comparison
    private Map<String, Double> changePercent;          // e.g. FOOD → +20.0
    private List<String> warnings;                      // budget alerts

    public SpendingInsightResponse() {
    }

    public SpendingInsightResponse(int year, int month, BigDecimal totalSpend, 
                                    Map<String, BigDecimal> byCategory,
                                    Map<String, BigDecimal> previousMonth,
                                    Map<String, Double> changePercent,
                                    List<String> warnings) {
        this.year = year;
        this.month = month;
        this.totalSpend = totalSpend;
        this.byCategory = byCategory;
        this.previousMonth = previousMonth;
        this.changePercent = changePercent;
        this.warnings = warnings;
    }

    // ===== GETTERS AND SETTERS =====

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public BigDecimal getTotalSpend() {
        return totalSpend;
    }

    public void setTotalSpend(BigDecimal totalSpend) {
        this.totalSpend = totalSpend;
    }

    public Map<String, BigDecimal> getByCategory() {
        return byCategory;
    }

    public void setByCategory(Map<String, BigDecimal> byCategory) {
        this.byCategory = byCategory;
    }

    public Map<String, BigDecimal> getPreviousMonth() {
        return previousMonth;
    }

    public void setPreviousMonth(Map<String, BigDecimal> previousMonth) {
        this.previousMonth = previousMonth;
    }

    public Map<String, Double> getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(Map<String, Double> changePercent) {
        this.changePercent = changePercent;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
