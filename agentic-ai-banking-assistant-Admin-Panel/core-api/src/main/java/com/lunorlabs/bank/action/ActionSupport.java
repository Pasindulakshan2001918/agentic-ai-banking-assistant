package com.lunorlabs.bank.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.lunorlabs.bank.common.Money;
import com.lunorlabs.bank.domain.Account;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Small helpers shared by handlers. Params from the LLM are untrusted: bad values become "missing", never exceptions. */
final class ActionSupport {
    private ActionSupport() {}

    static String text(JsonNode p, String name) {
        JsonNode n = p == null ? null : p.get(name);
        if (n == null || !n.isTextual()) return null;
        String t = n.asText().trim();
        return t.isEmpty() ? null : t;
    }

    static UUID uuid(JsonNode p, String name) {
        String t = text(p, name);
        if (t == null) return null;
        try {
            return UUID.fromString(t);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Only accounts the customer owns; a hallucinated or foreign id resolves to null (=> ask again). */
    static Account ownedAccount(List<Account> mine, UUID id) {
        if (id == null) return null;
        return mine.stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
    }

    static String remarks(JsonNode p) {
        String t = text(p, "remarks");
        if (t == null) return null;
        t = t.replaceAll("\\p{Cntrl}", "");
        return t.length() > 100 ? t.substring(0, 100) : t;
    }

    static String accountLabel(Account a) {
        String name = a.nickname() != null ? a.nickname() : a.productType();
        return name + " (••••" + a.last4() + ")";
    }

    static MissingField accountChoice(String field, String prompt, List<Account> mine, UUID exclude) {
        List<MissingField.Option> options = new ArrayList<>();
        for (Account a : mine) {
            if (a.id().equals(exclude) || !a.active()) continue;
            options.add(new MissingField.Option(a.id().toString(),
                    accountLabel(a) + " - " + Money.fmt(a.balance())));
        }
        return MissingField.choice(field, prompt, null, options);
    }
}
