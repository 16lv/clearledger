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
}
