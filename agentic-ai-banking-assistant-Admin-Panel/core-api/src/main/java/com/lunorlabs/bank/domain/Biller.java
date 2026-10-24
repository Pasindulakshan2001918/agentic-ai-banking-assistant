package com.lunorlabs.bank.domain;

import com.lunorlabs.bank.common.Lang;

import java.util.UUID;

public record Biller(UUID id, String code, String nameEn, String nameSi, String nameTa,
                     String referenceLabel, String category, double score) {
    public String name(Lang lang) {
        return switch (lang) {
            case SI -> nameSi;
            case TA -> nameTa;
            default -> nameEn;
        };
    }
}
