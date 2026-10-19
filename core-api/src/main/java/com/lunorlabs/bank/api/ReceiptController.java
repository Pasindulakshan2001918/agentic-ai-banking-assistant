package com.lunorlabs.bank.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunorlabs.bank.action.LocalizedSummary;
import com.lunorlabs.bank.action.Summary;
import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.common.Json;
import com.lunorlabs.bank.common.RequestMeta;
import com.lunorlabs.bank.i18n.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {
    private record Row(String txnRef, OffsetDateTime at, String snapshot) {}

    private final JdbcClient jdbc;
    private final MessageService messages;
    private final ObjectMapper om;

    public ReceiptController(JdbcClient jdbc, MessageService messages, ObjectMapper om) {
        this.jdbc = jdbc;
        this.messages = messages;
        this.om = om;
    }

    @GetMapping("/{txnRef}")
    public Map<String, Object> receipt(HttpServletRequest req, @PathVariable String txnRef) {
        RequestMeta meta = RequestMeta.from(req);
        Row row = jdbc.sql("SELECT txn_ref, created_at, CAST(counterparty AS text) AS snap "
                        + "FROM transactions WHERE txn_ref = :r AND customer_id = :c")
                .param("r", txnRef).param("c", meta.customerId())
                .query((rs, i) -> new Row(rs.getString("txn_ref"), rs.getObject("created_at", OffsetDateTime.class),
                        rs.getString("snap")))
                .optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Receipt not found"));

        Summary s = om.convertValue(Json.read(om, row.snapshot()).get("summary"), Summary.class);
        LocalizedSummary ls = messages.localize(s, meta.lang());

        List<LocalizedSummary.Line> lines = new ArrayList<>();
        lines.add(new LocalizedSummary.Line(messages.text("LBL_TXN_REF", meta.lang(), Map.of()), row.txnRef()));
        lines.addAll(ls.lines());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("txnRef", row.txnRef());
        m.put("title", messages.text("TITLE_RECEIPT", meta.lang(), Map.of()));
        m.put("completedAt", row.at());
        m.put("lines", lines);
        m.put("footer", messages.text("MSG_RECEIPT_FOOTER", meta.lang(), Map.of()));
        return m;
    }
}
