package com.lunorlabs.bank.action;

import java.util.List;

public record LocalizedSummary(String title, List<Line> lines) {
    public record Line(String label, String value) {}
}
