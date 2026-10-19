package com.lunorlabs.bank.policy;

import java.util.Map;

/** A reason code plus the values needed to fill its message placeholders (e.g. {limit}). */
public record ReasonDetail(ReasonCode code, Map<String, String> params) {}
