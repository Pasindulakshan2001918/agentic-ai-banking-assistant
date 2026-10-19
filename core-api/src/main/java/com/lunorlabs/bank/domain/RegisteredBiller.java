package com.lunorlabs.bank.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegisteredBiller(UUID id, String referenceNo, String nickname, Biller biller,
                               BigDecimal amountDue, LocalDate dueDate) {}
