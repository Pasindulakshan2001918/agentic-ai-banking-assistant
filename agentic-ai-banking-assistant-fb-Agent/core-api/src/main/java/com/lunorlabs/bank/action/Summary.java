package com.lunorlabs.bank.action;

import java.util.List;
import java.util.Map;

/** Stored in pending_actions.summary and transactions.counterparty; localized at read time. */
public record Summary(String titleKey, Map<String, String> titleParams, List<SummaryLine> lines) {}
