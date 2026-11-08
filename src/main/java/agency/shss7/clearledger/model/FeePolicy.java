package agency.shss7.clearledger.model;

/**
 * Strategy interface for calculating per-withdrawal fees.
 * Lets us swap out fee rules (e.g. student accounts vs standard accounts)
 * without hardcoding if/else chains inside Account subclasses.
 */
@FunctionalInterface
public interface FeePolicy {
    long calculateFeePence(long withdrawalAmountPence);

    static FeePolicy zeroFee() {
        return amount -> 0L;
    }

    static FeePolicy flatFee(long flatFeePence) {
        if (flatFeePence < 0) {
            throw new IllegalArgumentException("Flat fee cannot be negative.");
        }
        return amount -> flatFeePence;
    }

    /**
     * Charges a basis-point fee (100 bps = 1.00%) only on the portion of a withdrawal
     * above freeThresholdPence.
     */
    static FeePolicy tieredFee(long freeThresholdPence, int rateBasisPoints) {
        if (freeThresholdPence < 0 || rateBasisPoints < 0) {
            throw new IllegalArgumentException("Threshold and basis points cannot be negative.");
        }
        return amount -> {
            if (amount <= freeThresholdPence) {
                return 0L;
            }
            long taxablePence = amount - freeThresholdPence;
            return (taxablePence * rateBasisPoints) / 10_000L;
        };
    }
}
