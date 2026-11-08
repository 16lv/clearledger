package agency.shss7.clearledger.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable ledger entry recorded on every deposit, withdrawal, fee, or interest payout.
 */
public record TransactionEntry(
    String transactionId,
    String accountId,
    EntryType type,
    long amountPence,
    long balanceAfterPence,
    String description,
    Instant timestamp
) {
    public enum EntryType {
        DEPOSIT,
        WITHDRAWAL,
        FEE,
        INTEREST
    }

    public TransactionEntry {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId cannot be blank");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId cannot be blank");
        }
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }
}
