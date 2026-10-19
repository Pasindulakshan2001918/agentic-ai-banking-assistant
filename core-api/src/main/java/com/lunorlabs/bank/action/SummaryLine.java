package com.lunorlabs.bank.action;

/** labelKey is an i18n key; value is language-neutral (names, masked numbers, formatted money). */
public record SummaryLine(String labelKey, String value) {}
