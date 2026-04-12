package com.agentic.ai.service;

import org.springframework.stereotype.Service;

@Service
public class IntentRouter {

    public String route(String userMessage) {
        String msg = userMessage.toLowerCase();
        if (msg.contains("balance")) return "CHECK_BALANCE";
        if (msg.contains("transfer") || msg.contains("send")) return "TRANSFER_MONEY";
        if (msg.contains("bill") || msg.contains("electricity") || msg.contains("water")) return "PAY_BILL";
        if (msg.contains("block") || msg.contains("card")) return "CARD_ACTION";
        if (msg.contains("insight") || msg.contains("spending")) return "GET_INSIGHTS";
        if (msg.contains("transaction") || msg.contains("history")) return "TRANSACTION_HISTORY";
        return "UNKNOWN";
    }
}
