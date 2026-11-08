package agency.shss7.clearledger.model;

/**
 * Interest-bearing savings account.
 * Cannot go into overdraft and enforces a minimum reserve balance in pence.
 */
public final class SavingsAccount extends Account {
    private final int annualBasisPoints;
    private final long minimumReservePence;

    public SavingsAccount(
        String accountId,
        String ownerName,
        long openingBalancePence,
        int annualBasisPoints,
        long minimumReservePence
    ) {
        super(accountId, ownerName, openingBalancePence, FeePolicy.zeroFee());
        if (annualBasisPoints < 0) {
            throw new IllegalArgumentException("Interest rate basis points cannot be negative.");
        }
        if (minimumReservePence < 0 || openingBalancePence < minimumReservePence) {
            throw new IllegalArgumentException("Opening balance must meet minimum reserve.");
        }
        this.annualBasisPoints = annualBasisPoints;
        this.minimumReservePence = minimumReservePence;
    }

    public int getAnnualBasisPoints() {
        return annualBasisPoints;
    }

    public long getMinimumReservePence() {
        return minimumReservePence;
    }

    @Override
    protected boolean canWithdraw(long totalDeductionPence) {
        return (getBalancePence() - totalDeductionPence) >= minimumReservePence;
    }

    /**
     * Applies one month of simple interest based on the account's annual basis points
     * (100 bps = 1.00% per year).
     */
    public long applyMonthlyInterest(String txId) {
        long currentBalance = getBalancePence();
        if (currentBalance <= 0 || annualBasisPoints == 0) {
            return 0L;
        }
        // Integer division rounds down to the nearest penny
        long interestPence = (currentBalance * annualBasisPoints) / (10_000L * 12L);
        if (interestPence > 0) {
            creditInternal(
                txId,
                TransactionEntry.EntryType.INTEREST,
                interestPence,
                "Monthly interest (" + annualBasisPoints + " bps)"
            );
        }
        return interestPence;
    }

    @Override
    public String getAccountTypeLabel() {
        return "SAVINGS";
    }
}
