
package com.agentic.dto.ai;

/**

* AI INSIGHTS REQUEST DTO

*/

public class AiInsightsRequest {

private Long userId;

private String analysisType; // "spending", "categories", "merchants", etc

private String period; // "this month", "last 3 months", etc

// Getters & Setters

public Long getUserId() {

return userId;

}

public void setUserId(Long userId) {

this.userId = userId;

}

public String getAnalysisType() {

return analysisType;

}

public void setAnalysisType(String analysisType) {

this.analysisType = analysisType;

}

public String getPeriod() {

return period;

}

public void setPeriod(String period) {

this.period = period;

}

}
