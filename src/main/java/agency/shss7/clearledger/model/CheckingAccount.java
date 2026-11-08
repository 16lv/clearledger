package agency.shss7.clearledger.model;

/**
 * Day-to-day current account that supports an agreed overdraft limit in pence.
 */
public final class CheckingAccount extends Account {
    private final long overdraftLimitPence;

    public CheckingAccount(
        String accountId,
        String ownerName,
        long openingBalancePence,
        long overdraftLimitPence,
        FeePolicy feePolicy
    ) {
        super(accountId, ownerName, openingBalancePence, feePolicy);
        if (overdraftLimitPence < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        }
        this.overdraftLimitPence = overdraftLimitPence;
    }

    public long getOverdraftLimitPence() {
        return overdraftLimitPence;
    }

    @Override
    protected boolean canWithdraw(long totalDeductionPence) {
        // Allowed to go negative down to -overdraftLimitPence
        return (getBalancePence() - totalDeductionPence) >= -overdraftLimitPence;
    }

    @Override
    public String getAccountTypeLabel() {
        return "CHECKING";
    }
}
