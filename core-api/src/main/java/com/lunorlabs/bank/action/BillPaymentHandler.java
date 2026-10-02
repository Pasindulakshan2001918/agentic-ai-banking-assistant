package com.lunorlabs.bank.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.lunorlabs.bank.common.Lang;
import com.lunorlabs.bank.common.Money;
import com.lunorlabs.bank.domain.Account;
import com.lunorlabs.bank.domain.Biller;
import com.lunorlabs.bank.domain.ProductRule;
import com.lunorlabs.bank.domain.RegisteredBiller;
import com.lunorlabs.bank.i18n.MessageService;
import com.lunorlabs.bank.ledger.LedgerService;
import com.lunorlabs.bank.policy.Decision;
import com.lunorlabs.bank.policy.PolicyEngine;
import com.lunorlabs.bank.policy.PolicyInput;
import com.lunorlabs.bank.policy.PolicyResult;
import com.lunorlabs.bank.repo.AccountRepo;
import com.lunorlabs.bank.repo.BillerRepo;
import com.lunorlabs.bank.repo.RuleRepo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.lunorlabs.bank.action.ActionSupport.*;

/**
 * BILL_PAYMENT params: fromAccountId, amount, and either registeredBillerId
 * or (billerId + referenceNo) for an ad-hoc bill.
 */
@Component
public class BillPaymentHandler extends LedgerActionHandler {

    private final AccountRepo accounts;
    private final BillerRepo billers;
    private final RuleRepo rules;
    private final PolicyEngine policy;
    private final MessageService msg;

    public BillPaymentHandler(LedgerService ledger, AccountRepo accounts, BillerRepo billers, RuleRepo rules,
                              PolicyEngine policy, MessageService msg) {
        super(ledger);
        this.accounts = accounts;
        this.billers = billers;
        this.rules = rules;
        this.policy = policy;
        this.msg = msg;
    }

    @Override
    public ActionType type() { return ActionType.BILL_PAYMENT; }

    @Override
    public PrepareOutcome prepare(PrepareContext ctx, JsonNode p) {
        UUID cust = ctx.customerId();
        Lang lang = ctx.lang();
        List<Account> mine = accounts.findByCustomer(cust);
        List<MissingField> missing = new ArrayList<>();

        Account from = ownedAccount(mine, uuid(p, "fromAccountId"));
        if (from == null) {
            missing.add(accountChoice("fromAccountId", msg.text("ASK_FROM_ACCOUNT", lang, Map.of()), mine, null));
        }

        RegisteredBiller reg = null;
        Biller biller = null;
        String reference = null;
        UUID regId = uuid(p, "registeredBillerId");
        if (regId != null) reg = billers.findRegistered(regId, cust).orElse(null);

        if (reg != null) {
            biller = reg.biller();
            reference = reg.referenceNo();
        } else {
            UUID billerId = uuid(p, "billerId");
            if (billerId != null) biller = billers.findById(billerId).orElse(null);
            reference = text(p, "referenceNo");
            if (biller == null) {
                missing.add(billerChoice(lang, cust));
            } else if (reference == null) {
                missing.add(MissingField.text("referenceNo", msg.text("ASK_REFERENCE", lang, Map.of()),
                        "Label for this biller: " + biller.referenceLabel()));
            }
        }

        Optional<BigDecimal> amountOpt = Money.tryParse(p.get("amount"));
        if (amountOpt.isEmpty()) {
            String hint = (reg != null && reg.amountDue() != null)
                    ? "Current due is " + Money.fmt(reg.amountDue()) + " (due " + reg.dueDate() + ")."
                    : "Must be a positive number with at most 2 decimals.";
            missing.add(MissingField.money("amount", msg.text("ASK_AMOUNT", lang, Map.of()), hint));
        }
        if (!missing.isEmpty()) return PrepareOutcome.needsInfo(missing);

        BigDecimal amount = amountOpt.get();
        ProductRule rule = rules.find("BILL_PAYMENT", from.productType(), "INTERNAL").orElse(null);
        PolicyResult result = policy.evaluate(new PolicyInput(
                rule, amount, from.balance(), from.active(), false,
                rules.usedToday(cust, "BILL_PAYMENT"),
                rules.customerMaxDaily(cust, "BILL_PAYMENT").orElse(null)));
        if (result.decision() == Decision.DENY) return PrepareOutcome.denied(result);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fromAccountId", from.id().toString());
        params.put("amount", amount.toPlainString());
        if (reg != null) {
            params.put("registeredBillerId", reg.id().toString());
        } else {
            params.put("billerId", biller.id().toString());
            params.put("referenceNo", reference);
        }

        Map<String, Object> cp = new LinkedHashMap<>();
        cp.put("type", "BILLER");
        cp.put("billerCode", biller.code());
        cp.put("referenceNo", reference);

        List<SummaryLine> lines = new ArrayList<>();
        lines.add(new SummaryLine("LBL_FROM", accountLabel(from)));
        lines.add(new SummaryLine("LBL_BILLER", biller.name(lang)));
        lines.add(new SummaryLine("LBL_REFERENCE", reference));
        lines.add(new SummaryLine("LBL_AMOUNT", Money.fmt(amount)));
        if (result.fee().signum() > 0) lines.add(new SummaryLine("LBL_FEE", Money.fmt(result.fee())));
        Summary summary = new Summary("TITLE_BILL", Map.of("amount", Money.fmt(amount)), lines);

        ExecutionPlan plan = new ExecutionPlan(from.id(), null, amount, result.fee(), null, cp,
                reg != null ? reg.id() : null);
        return PrepareOutcome.ready(result, params, summary, plan);
    }

    private MissingField billerChoice(Lang lang, UUID cust) {
        List<MissingField.Option> options = new ArrayList<>();
        for (RegisteredBiller rb : billers.registered(cust)) {
            String due = rb.amountDue() != null ? " (due " + Money.fmt(rb.amountDue()) + ")" : "";
            String name = rb.nickname() != null ? rb.nickname() : rb.biller().name(lang);
            options.add(new MissingField.Option(rb.id().toString(), name + " - " + rb.biller().name(lang) + due));
        }
        return MissingField.choice("registeredBillerId", msg.text("ASK_BILLER", lang, Map.of()),
                "If the customer names a biller that is not in this list, call GET /billers?q=... and send "
                        + "billerId + referenceNo for an ad-hoc payment.",
                options);
    }
}
