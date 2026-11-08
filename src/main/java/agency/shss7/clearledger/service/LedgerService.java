package agency.shss7.clearledger.service;

import agency.shss7.clearledger.model.Account;
import agency.shss7.clearledger.model.SavingsAccount;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates multi-account operations such as atomic transfers and month-end
 * interest runs across polymorphic Account instances.
 */
public final class LedgerService {
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final AtomicLong txSequence = new AtomicLong(1000L);

    public void registerAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("account cannot be null.");
        }
        if (accounts.containsKey(account.getAccountId())) {
            throw new IllegalArgumentException("Duplicate account ID: " + account.getAccountId());
        }
        accounts.put(account.getAccountId(), account);
    }

    public Account getAccount(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new NoSuchElementException("Unknown account ID: " + accountId);
        }
        return account;
    }

    public Collection<Account> listAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    /**
     * Transfers funds between two registered accounts.
     * Debits the source account first so overdraft/reserve rules are checked before
     * crediting the destination account.
     */
    public String transfer(String fromAccountId, String toAccountId, long amountPence, String reference) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }
        if (amountPence <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive.");
        }

        Account source = getAccount(fromAccountId);
        Account target = getAccount(toAccountId);
        String txId = "TX-" + txSequence.incrementAndGet();

        source.withdraw(txId + "-OUT", amountPence, "Transfer to " + toAccountId + ": " + reference);
        target.deposit(txId + "-IN", amountPence, "Transfer from " + fromAccountId + ": " + reference);
        return txId;
    }

    /**
     * Iterates over all registered accounts and triggers monthly interest accrual
     * on any SavingsAccount instances using Java 21 pattern matching for instanceof.
     */
    public long runMonthlyInterestCycle() {
        long totalInterestPaidPence = 0L;
        for (Account account : accounts.values()) {
            if (account instanceof SavingsAccount savings) {
                String txId = "INT-" + txSequence.incrementAndGet();
                totalInterestPaidPence += savings.applyMonthlyInterest(txId);
            }
        }
        return totalInterestPaidPence;
    }

    public long getTotalHoldingsPence() {
        return accounts.values().stream()
            .mapToLong(Account::getBalancePence)
            .sum();
    }
}
