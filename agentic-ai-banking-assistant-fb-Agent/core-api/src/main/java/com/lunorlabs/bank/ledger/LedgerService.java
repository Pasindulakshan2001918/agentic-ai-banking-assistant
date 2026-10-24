package com.lunorlabs.bank.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunorlabs.bank.action.ExecutionPlan;
import com.lunorlabs.bank.action.InsufficientFundsException;
import com.lunorlabs.bank.action.Summary;
import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.common.Json;
import com.lunorlabs.bank.domain.Account;
import com.lunorlabs.bank.domain.PendingAction;
import com.lunorlabs.bank.repo.AccountRepo;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The only place money moves. One DB transaction: lock the pending row, atomic debit, optional credit,
 * ledger row, mark completed. transactions.pending_action_id is UNIQUE, so a replay can never double-post.
 * This is also the seam where a real core-banking / ABS call would be added.
 */
@Service
public class LedgerService {
    private final JdbcClient jdbc;
    private final AccountRepo accounts;
    private final ObjectMapper om;

    public LedgerService(JdbcClient jdbc, AccountRepo accounts, ObjectMapper om) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.om = om;
    }

    @Transactional
    public PostResult post(PendingAction p, ExecutionPlan plan, Summary summary, String idempotencyKey) {
        String status = jdbc.sql("SELECT status FROM pending_actions WHERE id = :id FOR UPDATE")
                .param("id", p.id()).query(String.class).single();

        if ("COMPLETED".equals(status)) {
            return findByPending(p.id()).orElseThrow(() -> new IllegalStateException("Completed action has no transaction"));
        }
        if (!List.of("PREPARED", "AWAITING_OTP").contains(status)) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTION_ALREADY_DONE", "This action is no longer pending.");
        }

        BigDecimal total = plan.amount().add(plan.fee());
        if (!accounts.debit(plan.fromAccountId(), total)) {
            throw new InsufficientFundsException();   // rolls back this transaction
        }
        if (plan.creditAccountId() != null) {
            accounts.credit(plan.creditAccountId(), plan.amount());

            // If recipient account belongs to a different customer, record incoming transaction
            accounts.findById(plan.creditAccountId()).ifPresent(creditAcc -> {
                if (!creditAcc.customerId().equals(p.customerId())) {
                    Account senderAcc = accounts.findById(plan.fromAccountId()).orElse(null);
                    String senderLabel = senderAcc != null ? ("Account " + senderAcc.accountNo()) : "Customer";
                    String cpJson = Json.write(om, Map.of(
                            "type", "TRANSFER_RECEIVED",
                            "senderAccountNo", senderAcc != null ? senderAcc.accountNo() : "",
                            "remarks", plan.remarks() != null ? plan.remarks() : "Transfer received"
                    ));
                    jdbc.sql("""
                        INSERT INTO transactions
                          (txn_ref, customer_id, from_account_id, action_type,
                           counterparty, amount, fee, remarks)
                        VALUES ('TXN-' || to_char(now() AT TIME ZONE 'Asia/Colombo', 'YYYYMMDD') || '-' || nextval('txn_ref_seq'),
                                :cust, :acc, 'FUND_TRANSFER', CAST(:cp AS jsonb), :amt, 0, :rem)
                        """)
                        .param("cust", creditAcc.customerId())
                        .param("acc", creditAcc.id())
                        .param("cp", cpJson)
                        .param("amt", plan.amount())
                        .param("rem", "Transfer received from " + senderLabel + (plan.remarks() != null ? (": " + plan.remarks()) : ""))
                        .update();
                }
            });
        }

        String snapshot = Json.write(om, Map.of("summary", summary, "counterparty", plan.counterparty()));
        PostResult r = jdbc.sql("""
                INSERT INTO transactions
                  (txn_ref, customer_id, pending_action_id, action_type, from_account_id,
                   counterparty, amount, fee, remarks)
                VALUES ('TXN-' || to_char(now() AT TIME ZONE 'Asia/Colombo', 'YYYYMMDD') || '-' || nextval('txn_ref_seq'),
                        :cust, :pa, :type, :from, CAST(:cp AS jsonb), :amt, :fee, :rem)
                RETURNING txn_ref, created_at
                """)
                .param("cust", p.customerId()).param("pa", p.id()).param("type", p.actionType())
                .param("from", plan.fromAccountId()).param("cp", snapshot)
                .param("amt", plan.amount()).param("fee", plan.fee()).param("rem", plan.remarks())
                .query((rs, i) -> new PostResult(rs.getString("txn_ref"),
                        rs.getObject("created_at", OffsetDateTime.class), false))
                .single();

        if (plan.registeredBillerId() != null) {
            jdbc.sql("UPDATE bill_dues SET status = 'PAID' "
                            + "WHERE registered_biller_id = :r AND status = 'UNPAID' AND amount_due <= :a")
                    .param("r", plan.registeredBillerId()).param("a", plan.amount()).update();
        }

        jdbc.sql("UPDATE pending_actions SET status = 'COMPLETED', completed_at = now(), idempotency_key = :k WHERE id = :id")
                .param("k", idempotencyKey).param("id", p.id()).update();
        return r;
    }

    public Optional<PostResult> findByPending(UUID pendingId) {
        return jdbc.sql("SELECT txn_ref, created_at FROM transactions WHERE pending_action_id = :id")
                .param("id", pendingId)
                .query((rs, i) -> new PostResult(rs.getString("txn_ref"),
                        rs.getObject("created_at", OffsetDateTime.class), true))
                .optional();
    }
}
