package com.lunorlabs.bank.action;

import com.lunorlabs.bank.common.Lang;

import java.util.UUID;

public record PrepareContext(UUID customerId, UUID sessionId, Lang lang) {}
