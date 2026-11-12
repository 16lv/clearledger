# Java Banking Ledger (java-banking-ledger)

[![CI](https://github.com/16lv/java-banking-ledger/actions/workflows/ci.yml/badge.svg)](https://github.com/16lv/java-banking-ledger/actions/workflows/ci.yml)

**Java Banking Ledger** ($(System.Collections.Hashtable.newRepo)) is a Java 21 object-oriented banking and transaction ledger engine developed during my second year of Computer Science at Cardiff University. It models current (`CheckingAccount`) and savings (`SavingsAccount`) accounts, pluggable withdrawal fee strategies (`FeePolicy`), immutable audit statements (`TransactionEntry`), and atomic inter-account transfers using integer pence arithmetic.

## Why I Built It

In our Year 2 Object-Oriented Programming module we studied how inheritance, encapsulation, and polymorphism apply to financial domain models—and why storing currency in binary floating-point (`double`) leads to subtle rounding errors. I built Java Banking Ledger to apply those concepts in a self-contained Java 21 library without hiding the domain logic behind a web framework.

## Architecture & OOP Design Decisions

```text
               ┌───────────────────────────────────┐
               │       <<abstract>> Account        │
               │ - accountId: String               │
               │ - balancePence: long              │
               │ - feePolicy: FeePolicy            │
               │ - ledgerEntries: List<Entry>      │
               │ # canWithdraw(long): boolean      │
               └─────────────────┬─────────────────┘
                                 │
            ┌────────────────────┴────────────────────┐
            ▼                                         ▼
┌───────────────────────────────┐         ┌───────────────────────────────┐
│        CheckingAccount        │         │        SavingsAccount         │
│ - overdraftLimitPence: long   │         │ - annualBasisPoints: int      │
│ + canWithdraw(): allows down  │         │ - minimumReservePence: long   │
│   to -overdraftLimitPence     │         │ + applyMonthlyInterest(): long│
└───────────────────────────────┘         └───────────────────────────────┘
```

1. **Abstract Template Method (`Account.withdraw` -> `canWithdraw`)**: The base `Account` class encapsulates `balancePence` and `ledgerEntries` (`private` fields exposed only via `Collections.unmodifiableList`). Its `withdraw` method calculates the total deduction (`amountPence + feePence`) and delegates the solvency check to the abstract `canWithdraw(long)` hook implemented by each subclass.
2. **Strategy Pattern (`FeePolicy`)**: Declared as a `@FunctionalInterface` with static factories (`zeroFee()`, `flatFee(long)`, and `tieredFee(long, int)`). This allows fee behaviour to be injected per account instance without cluttering `Account` subclasses with conditional fee branches.
3. **Immutable Statement Records (`TransactionEntry`)**: Every deposit, withdrawal, fee deduction, and interest payout appends a Java 21 `record` containing the transaction ID, `EntryType` (`DEPOSIT`, `WITHDRAWAL`, `FEE`, `INTEREST`), amount in pence, post-transaction balance snapshot, and `Instant` timestamp.
4. **Integer Pence Arithmetic (`long`)**: Balances, overdraft limits, reserves, and fees are stored in integer pence (`15_000L` = `GBP 150.00`), and annual interest rates are expressed in basis points (`480 bps` = `4.80%`), avoiding IEEE 754 floating-point drift.

## Project Structure

```text
clearledger/
├── pom.xml
└── src/
    ├── main/java/agency/shss7/clearledger/
    │   ├── model/
    │   │   ├── Account.java            # Abstract base account & template methods
    │   │   ├── CheckingAccount.java    # Overdraft-enabled current account
    │   │   ├── FeePolicy.java          # Strategy interface (zero, flat, tiered fees)
    │   │   ├── SavingsAccount.java     # Reserve-enforced, interest-bearing account
    │   │   └── TransactionEntry.java   # Immutable Java 21 statement record
    │   └── service/
    │       └── LedgerService.java      # Multi-account transfers, interest cycle & CLI demo
    └── test/java/agency/shss7/clearledger/service/
        └── LedgerServiceTest.java      # JUnit 5 test suite
```

## Getting Started

### Prerequisites

- **Java 21** (OpenJDK 21 / Eclipse Temurin)
- **Maven 3.9+**

### Run the JUnit 5 Test Suite

```bash
mvn test
```

### Build & Run the CLI Ledger Simulation

```bash
mvn package
java -jar target/clearledger-1.0.0.jar
```

Example output:

```text
=== CLEARLEDGER ACCOUNT & STATEMENT SUMMARY ===
CHK-01 [CHECKING] Owner: Sheikh S. | Closing Balance: GBP 219.75
  OPEN-CHK-01  DEPOSIT        GBP 150.00 -> After:   GBP 150.00 (Opening deposit)
  W-101        WITHDRAWAL     GBP 180.00 -> After:   -GBP 30.00 (Cloud server hosting invoice)
  W-101-FEE    FEE              GBP 0.25 -> After:   -GBP 30.25 (Withdrawal fee)
  TX-1001-IN   DEPOSIT        GBP 250.00 -> After:   GBP 219.75 (Transfer from SAV-01: Current account top-up)

SAV-01 [SAVINGS] Owner: Sheikh S. | Closing Balance: GBP 1757.00
  OPEN-SAV-01  DEPOSIT       GBP 2000.00 -> After:  GBP 2000.00 (Opening deposit)
  TX-1001-OUT  WITHDRAWAL     GBP 250.00 -> After:  GBP 1750.00 (Transfer to CHK-01: Current account top-up)
  INT-1002     INTEREST         GBP 7.00 -> After:  GBP 1757.00 (Monthly interest (480 bps))
```

## Known Limitations & Future Improvements

- `LedgerService` stores accounts in an in-memory `LinkedHashMap` for deterministic single-threaded execution; adding per-account `ReentrantLock` ordering (sorted by `accountId` to prevent deadlocks) would make concurrent transfers thread-safe.
- Monthly interest currently uses simple integer division (`(balance * bps) / 120_000L`), which truncates fractional pennies; tracking sub-penny remainder accumulators would model daily compounding more accurately.
