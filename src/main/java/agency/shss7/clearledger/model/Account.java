package agency.shss7.clearledger.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class representing a ledger account.
 *
 * Balances are stored in integer pence (long) rather than double so we never
 * lose pennies to IEEE 754 floating-point rounding during transfers.
 */
public abstract class Account {
    private final String accountId;
    private final String ownerName;
    private long balancePence;
    private final FeePolicy feePolicy;
    private final List<TransactionEntry> ledgerEntries;

    protected Account(String accountId, String ownerName, long openingBalancePence, FeePolicy feePolicy) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId cannot be blank.");
        }
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("ownerName cannot be blank.");
        }
        if (openingBalancePence < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative.");
        }
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.balancePence = openingBalancePence;
        this.feePolicy = Objects.requireNonNull(feePolicy, "feePolicy is required.");
        this.ledgerEntries = new ArrayList<>();

        if (openingBalancePence > 0) {
            recordEntry("OPEN-" + accountId, TransactionEntry.EntryType.DEPOSIT, openingBalancePence, "Opening deposit");
        }
    }

    public final String getAccountId() {
        return accountId;
    }

    public final String getOwnerName() {
        return ownerName;
    }

    public final long getBalancePence() {
        return balancePence;
    }

    public final List<TransactionEntry> getStatement() {
        return Collections.unmodifiableList(ledgerEntries);
    }

    /**
     * Subclasses define how much below zero (if at all) the balance is allowed to drop.
     */
    protected abstract boolean canWithdraw(long totalDeductionPence);

    public abstract String getAccountTypeLabel();

    public void deposit(String txId, long amountPence, String note) {
        if (amountPence <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        this.balancePence += amountPence;
        recordEntry(txId, TransactionEntry.EntryType.DEPOSIT, amountPence, note);
    }

    public void withdraw(String txId, long amountPence, String note) {
        if (amountPence <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        long feePence = feePolicy.calculateFeePence(amountPence);
        long totalDeduction = amountPence + feePence;

        if (!canWithdraw(totalDeduction)) {
            throw new IllegalStateException(
                "Insufficient funds on account " + accountId + " for withdrawal of " + totalDeduction + "p"
            );
        }

        this.balancePence -= amountPence;
        recordEntry(txId, TransactionEntry.EntryType.WITHDRAWAL, amountPence, note);

        if (feePence > 0) {
            this.balancePence -= feePence;
            recordEntry(txId + "-FEE", TransactionEntry.EntryType.FEE, feePence, "Withdrawal fee");
        }
    }

    protected final void creditInternal(String txId, TransactionEntry.EntryType type, long amountPence, String note) {
        this.balancePence += amountPence;
        recordEntry(txId, type, amountPence, note);
    }

    protected final void debitInternal(String txId, TransactionEntry.EntryType type, long amountPence, String note) {
        if (!canWithdraw(amountPence)) {
            throw new IllegalStateException("Insufficient funds on account " + accountId);
        }
        this.balancePence -= amountPence;
        recordEntry(txId, type, amountPence, note);
    }

    private void recordEntry(String txId, TransactionEntry.EntryType type, long amountPence, String note) {
        ledgerEntries.add(new TransactionEntry(
            txId,
            accountId,
            type,
            amountPence,
            balancePence,
            note,
            Instant.now()
        ));
    }
}
