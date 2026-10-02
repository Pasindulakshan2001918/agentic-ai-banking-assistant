package com.lunorlabs.bank.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.lunorlabs.bank.action.ActionService;
import com.lunorlabs.bank.action.PrepareResponse;
import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.common.RequestMeta;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {
    public record PrepareRequest(String actionType, JsonNode params) {}
    public record ConfirmRequest(String confirmationToken, String otp) {}

    private final ActionService service;

    public ActionController(ActionService service) { this.service = service; }

    @PostMapping("/prepare")
    public PrepareResponse prepare(HttpServletRequest req, @RequestBody PrepareRequest body) {
        return service.prepare(RequestMeta.from(req), body.actionType(), body.params());
    }

    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(HttpServletRequest req, @PathVariable UUID id,
                                       @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
                                       @RequestBody ConfirmRequest body) {
        if (idemKey == null || idemKey.isBlank() || idemKey.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "Idempotency-Key header (max 64 chars) is required");
        }
        return service.confirm(RequestMeta.from(req), id, body.confirmationToken(), body.otp(), idemKey);
    }

    @PostMapping("/{id}/otp/resend")
    public Map<String, Object> resend(HttpServletRequest req, @PathVariable UUID id) {
        return service.resendOtp(RequestMeta.from(req), id);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(HttpServletRequest req, @PathVariable UUID id) {
        return service.cancel(RequestMeta.from(req), id);
    }
}
