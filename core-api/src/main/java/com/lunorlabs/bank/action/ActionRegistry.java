package com.lunorlabs.bank.action;

import com.lunorlabs.bank.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ActionRegistry {
    private final Map<ActionType, ActionHandler> handlers = new EnumMap<>(ActionType.class);

    public ActionRegistry(List<ActionHandler> all) {
        for (ActionHandler h : all) handlers.put(h.type(), h);
    }

    public ActionHandler get(ActionType type) {
        ActionHandler h = handlers.get(type);
        if (h == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "No handler for " + type);
        }
        return h;
    }
}
