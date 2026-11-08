# ClearLedger

[![CI](https://github.com/16lv/clearledger/actions/workflows/ci.yml/badge.svg)](https://github.com/16lv/clearledger/actions/workflows/ci.yml)

**ClearLedger** is a Java 21 object-oriented banking & account ledger engine I built during my second year of Computer Science at Cardiff University. It models current and savings accounts, pluggable fee policies, immutable transaction statements, and multi-account transfers using integer pence arithmetic.

## Why I Built It

In our Year 2 Object-Oriented Programming module we spent a lot of time discussing why inheritance, encapsulation, and polymorphism matter in real financial codebases—and why storing money in `double` causes rounding bugs. I built `ClearLedger` to put those ideas into a clean, well-tested library without hiding behind a web framework.

## Key OOP Design Choices

- **Abstraction & Inheritance (`Account` → `CheckingAccount`, `SavingsAccount`)**: `Account` holds the encapsulated balance and statement history while delegating withdrawal eligibility rules (`canWithdraw`) to concrete subclasses.
- **Strategy Pattern (`FeePolicy`)**: A `@FunctionalInterface` injected into accounts so fee rules (zero fee, flat fee, tiered fee) can be swapped without modifying account classes.
- **Immutability (`TransactionEntry` record)**: Every deposit, withdrawal, fee, and interest payment appends an immutable Java 21 `record` with a snapshot of the post-transaction balance.
- **Integer Pence Arithmetic (`long`)**: All balances and fees are stored in integer pence (`15_000L` = `£150.00`) to avoid IEEE 754 floating-point drift.

## Project Layout

```text
clearledger/
├── pom.xml
└── src/
    ├── main/java/agency/shss7/clearledger/
    │   ├── model/
    │   │   ├── Account.java
    │   │   ├── CheckingAccount.java
    │   │   ├── FeePolicy.java
    │   │   ├── SavingsAccount.java
    │   │   └── TransactionEntry.java
    │   └── service/
    │       └── LedgerService.java
    └── test/java/agency/shss7/clearledger/service/
        └── LedgerServiceTest.java
```

## Running Tests

```bash
mvn test
```
