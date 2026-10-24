package com.lunorlabs.bank.action;

import java.util.List;

/** One piece of information the agent must still collect from the customer. */
public record MissingField(String field, String type, String prompt, String hint, List<Option> options) {

    public record Option(String value, String label) {}

    public static MissingField money(String field, String prompt, String hint) {
        return new MissingField(field, "money", prompt, hint, null);
    }

    public static MissingField text(String field, String prompt, String hint) {
        return new MissingField(field, "text", prompt, hint, null);
    }

    public static MissingField choice(String field, String prompt, String hint, List<Option> options) {
        return new MissingField(field, "choice", prompt, hint, options);
    }
}
