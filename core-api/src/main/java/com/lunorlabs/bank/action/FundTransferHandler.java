package com.lunorlabs.bank.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.lunorlabs.bank.common.Lang;
import com.lunorlabs.bank.common.Money;
import com.lunorlabs.bank.config.BankProperties;
import com.lunorlabs.bank.domain.Account;
import com.lunorlabs.bank.domain.Payee;
import com.lunorlabs.bank.domain.ProductRule;
import com.lunorlabs.bank.i18n.MessageService;
import com.lunorlabs.bank.ledger.LedgerService;
import com.lunorlabs.bank.policy.Decision;
import com.lunorlabs.bank.policy.PolicyEngine;
import com.lunorlabs.bank.policy.PolicyInput;
import com.lunorlabs.bank.policy.PolicyResult;
import com.lunorlabs.bank.policy.ReasonCode;
import com.lunorlabs.bank.repo.AccountRepo;
import com.lunorlabs.bank.repo.PayeeRepo;
import com.lunorlabs.bank.repo.RuleRepo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.lunorlabs.bank.action.ActionSupport.*;

/**
 * FUND_TRANSFER params: fromAccountId, amount, and one destination:
 * toOwnAccountId | toPayeeId | toNewAccount{bankCode,accountNo,name}; optional remarks.
 */
@Component
public class FundTransferHandler extends LedgerActionHandler {

    private record NewAccount(String bankCode, String accountNo, String name) {
        static NewAccount from(JsonNode n) {
            if (n == null || !n.isObject()) return null;
            String bank = text(n, "bankCode"), no = text(n, "accountNo"), name = text(n, "name");
            return (bank == null || no == null || name == null) ? null : new NewAccount(bank, no, name);
        }
        String last4() { return accountNo.length() <= 4 ? accountNo : accountNo.substring(accountNo.length() - 4); }
    }

    private final AccountRepo accounts;
    private final PayeeRepo payees;
    private final RuleRepo rules;
    private final PolicyEngine policy;
    private final MessageService msg;
    private final BankProperties props;

    public FundTransferHandler(LedgerService ledger, AccountRepo accounts, PayeeRepo payees, RuleRepo rules,
                               PolicyEngine policy, MessageService msg, BankProperties props) {
        super(ledger);
        this.accounts = accounts;
        this.payees = payees;
        this.rules = rules;
        this.policy = policy;
        this.msg = msg;
        this.props = props;
    }

    @Override
    public ActionType type() { return ActionType.FUND_TRANSFER; }

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

        Account toOwn = ownedAccount(mine, uuid(p, "toOwnAccountId"));
        Payee payee = null;
        UUID payeeId = uuid(p, "toPayeeId");
        if (toOwn == null && payeeId != null) payee = payees.findOwned(payeeId, cust).orElse(null);
        NewAccount newAcc = (toOwn == null && payee == null) ? NewAccount.from(p.get("toNewAccount")) : null;

        if (from != null && toOwn != null && from.id().equals(toOwn.id())) {
            return PrepareOutcome.denied(PolicyResult.deny(ReasonCode.SAME_ACCOUNT_TRANSFER, Map.of()));
        }

        if (toOwn == null && payee == null && newAcc == null) {
            missing.add(destinationChoice(lang, mine, from, cust));
        }

        Optional<BigDecimal> amountOpt = Money.tryParse(p.get("amount"));
        if (amountOpt.isEmpty()) {
            missing.add(MissingField.money("amount", msg.text("ASK_AMOUNT", lang, Map.of()),
                    "Must be a positive number with at most 2 decimals."));
        }
        if (!missing.isEmpty()) return PrepareOutcome.needsInfo(missing);

        BigDecimal amount = amountOpt.get();
        String rail;
        String destLabel;
        boolean newPayee = false;
        Map<String, Object> destParams = new LinkedHashMap<>();
        Map<String, Object> cp = new LinkedHashMap<>();

        if (toOwn != null) {
            rail = "INTERNAL";
            destLabel = accountLabel(toOwn);
            destParams.put("toOwnAccountId", toOwn.id().toString());
            cp.put("type", "OWN_ACCOUNT");
            cp.put("accountId", toOwn.id().toString());
        } else if (payee != null) {
            rail = props.ownBankCode().equals(payee.bankCode()) ? "INTERNAL" : "JUSTPAY";
            destLabel = payee.nickname() + " - " + payee.beneficiaryName()
                    + " (" + payee.bankName() + " ••••" + payee.last4() + ")";
            destParams.put("toPayeeId", payee.id().toString());
            cp.put("type", "PAYEE");
            cp.put("payeeId", payee.id().toString());
        } else {
            rail = props.ownBankCode().equals(newAcc.bankCode()) ? "INTERNAL" : "JUSTPAY";
            destLabel = newAcc.name() + " (" + newAcc.bankCode() + " ••••" + newAcc.last4() + ")";
            newPayee = true;
            Map<String, Object> na = new LinkedHashMap<>();
            na.put("bankCode", newAcc.bankCode());
            na.put("accountNo", newAcc.accountNo());
            na.put("name", newAcc.name());
            destParams.put("toNewAccount", na);
            cp.put("type", "NEW_ACCOUNT");
            cp.put("bankCode", newAcc.bankCode());
            cp.put("accountLast4", newAcc.last4());
        }

        ProductRule rule = rules.find("FUND_TRANSFER", from.productType(), rail).orElse(null);
        if (payee != null) {
            int cooling = rule != null ? rule.newPayeeCoolingHrs() : 24;
            newPayee = payee.addedAt().plusHours(cooling).isAfter(OffsetDateTime.now());
        }

        PolicyResult result = policy.evaluate(new PolicyInput(
                rule, amount, from.balance(), from.active(), newPayee,
                rules.usedToday(cust, "FUND_TRANSFER"),
                rules.customerMaxDaily(cust, "FUND_TRANSFER").orElse(null)));
        if (result.decision() == Decision.DENY) return PrepareOutcome.denied(result);

        String remarks = remarks(p);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fromAccountId", from.id().toString());
        params.put("amount", amount.toPlainString());
        params.putAll(destParams);
        if (remarks != null) params.put("remarks", remarks);

        List<SummaryLine> lines = new ArrayList<>();
        lines.add(new SummaryLine("LBL_FROM", accountLabel(from)));
        lines.add(new SummaryLine("LBL_TO", destLabel));
        lines.add(new SummaryLine("LBL_AMOUNT", Money.fmt(amount)));
        if (result.fee().signum() > 0) lines.add(new SummaryLine("LBL_FEE", Money.fmt(result.fee())));
        Summary summary = new Summary("TITLE_TRANSFER", Map.of("amount", Money.fmt(amount)), lines);

        ExecutionPlan plan = new ExecutionPlan(from.id(), toOwn != null ? toOwn.id() : null,
                amount, result.fee(), remarks, cp, null);
        return PrepareOutcome.ready(result, params, summary, plan);
    }

    /** Option values are "field=<uuid>" so the agent knows which param to send back. */
    private MissingField destinationChoice(Lang lang, List<Account> mine, Account from, UUID cust) {
        List<MissingField.Option> options = new ArrayList<>();
        for (Account a : mine) {
            if (!a.active() || (from != null && a.id().equals(from.id()))) continue;
            options.add(new MissingField.Option("toOwnAccountId=" + a.id(), accountLabel(a)));
        }
        for (Payee py : payees.search(cust, null)) {
            options.add(new MissingField.Option("toPayeeId=" + py.id(),
                    py.nickname() + " - " + py.beneficiaryName()));
        }
        return MissingField.choice("destination", msg.text("ASK_PAYEE", lang, Map.of()),
                "Send the chosen value as its own param, e.g. toPayeeId=<uuid> becomes {\"toPayeeId\":\"<uuid>\"}. "
                        + "For an unsaved account send toNewAccount{bankCode,accountNo,name}.",
                options);
    }
}
