package agency.shss7.clearledger.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import agency.shss7.clearledger.model.CheckingAccount;
import agency.shss7.clearledger.model.FeePolicy;
import agency.shss7.clearledger.model.SavingsAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LedgerServiceTest {
    private LedgerService ledger;
    private CheckingAccount checking;
    private SavingsAccount savings;

    @BeforeEach
    void setUp() {
        ledger = new LedgerService();
        // £150.00 opening balance, £50.00 overdraft limit, £0.25 flat withdrawal fee
        checking = new CheckingAccount("CHK-01", "Sheikh S.", 15_000L, 5_000L, FeePolicy.flatFee(25L));
        // £2000.00 opening balance, 4.80% annual interest (480 bps), £100.00 minimum reserve
        savings = new SavingsAccount("SAV-01", "Sheikh S.", 200_000L, 480, 10_000L);

        ledger.registerAccount(checking);
        ledger.registerAccount(savings);
    }

    @Test
    void checkingAccountAllowsOverdraftWithinLimitAndAppliesFee() {
        // Withdraw £180.00 + £0.25 fee -> new balance should be -£30.25 (-3025p)
        checking.withdraw("W-101", 18_000L, "Server hosting invoice");
        assertEquals(-3_025L, checking.getBalancePence());
        assertEquals(3, checking.getStatement().size()); // opening + withdrawal + fee
    }

    @Test
    void checkingAccountRejectsWithdrawalExceedingOverdraftLimit() {
        assertThrows(
            IllegalStateException.class,
            () -> checking.withdraw("W-102", 20_000L, "Exceeds overdraft once 25p fee is added")
        );
        assertEquals(15_000L, checking.getBalancePence());
    }

    @Test
    void savingsAccountEnforcesMinimumReserveAndPaysInterest() {
        // Minimum reserve is £100.00 (10_000p), so withdrawing £1950.00 from £2000.00 must fail
        assertThrows(
            IllegalStateException.class,
            () -> savings.withdraw("W-201", 195_000L, "Too large withdrawal")
        );

        // Monthly interest on £2000.00 at 480 bps = (200_000 * 480) / 120_000 = 800p (£8.00)
        long interestPaid = ledger.runMonthlyInterestCycle();
        assertEquals(800L, interestPaid);
        assertEquals(200_800L, savings.getBalancePence());
    }

    @Test
    void transferMovesFundsAtomicallyBetweenAccounts() {
        String txId = ledger.transfer("SAV-01", "CHK-01", 25_000L, "Top up current account");
        assertTrue(txId.startsWith("TX-"));
        assertEquals(175_000L, savings.getBalancePence());
        assertEquals(40_000L, checking.getBalancePence());
        assertEquals(215_000L, ledger.getTotalHoldingsPence());
    }
}
