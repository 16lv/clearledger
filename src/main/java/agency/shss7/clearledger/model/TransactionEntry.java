package agency.shss7.clearledger.model;

import java.time.Instant;

// Immutable ledger entry recorded on every deposit, withdrawal, fee, or interest payout
public record TransactionEntry(
    String type,
    long amountPence,
    long balanceAfterPence,
    String note,
    Instant timestamp
) {
    public TransactionEntry {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Transaction type cannot be blank");
        }
    }
}
