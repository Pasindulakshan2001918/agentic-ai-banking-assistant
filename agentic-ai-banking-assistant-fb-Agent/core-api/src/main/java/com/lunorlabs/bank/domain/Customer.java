package com.lunorlabs.bank.domain;

import java.util.UUID;

public record Customer(UUID id, String cif, String fullName, String mobile, String email, String preferredLanguage) {}
