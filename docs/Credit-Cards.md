# Credit cards

How Fern Wallet models credit cards. Everything is stored on the device and nothing is sent to a bank.

## Data model

- A card is a normal account with `type = CREDIT_CARD`, plus one row in `credit_cards` that uses the same id.
  That row holds:
  - the bank and product (catalogue ids, or custom names)
  - the network
  - the last four digits only (full card numbers, CVV and expiry are never asked for or stored)
  - the credit limit
  - the statement day and the due-date rule
  - the opening outstanding and its date
  - the status, artwork and reminder settings
- `credit_card_statements` stores only values the user copies from the bank statement: the actual statement date, the actual due date, the statement balance and the minimum due. Everything else is calculated from transactions.
- Database migration 130 → 131 is additive:
  - `accounts.type` is added with the default `'ASSET'`.
  - Two new tables are created.
  - Existing accounts, transactions and balances are not changed.
- Backups include both tables. Older backups without them import as normal asset accounts.

## Accounting

Cards use the same ledger as every other account.

| Action | Recorded as | Effect on the card |
|---|---|---|
| Purchase, fee, interest | Expense on the card | owed amount goes up |
| Refund, cashback, reversal | Income on the card | owed amount goes down |
| Bill payment | Transfer from a bank account to the card | owed amount goes down. It is **not** an expense, because the purchases already were. |

- A card's balance is the negative of what is owed (outstanding = −balance).
- Spending reports count each purchase once, when it happens.
- Refunds show up as income on Home. Category totals are expense minus refund.
- The **opening outstanding** is the debt that already existed when the card was added. It is stored on the card and is included in the balance from the opening date. It is never a transaction, so it never shows up as income or expense.
- The total balance is a net figure: card debt is subtracted. The card setting "Count card debt in total balance" can leave a card out of the total. Its purchases and refunds still count in income and expense.
- A payment above the outstanding amount leaves the card with a credit balance. The pay screen warns before this happens.
- Cards are edited only on the card screens. The generic "adjust balance" option is not offered for cards, because it would create fake income or expense.
- Closing a card keeps the card and its history. A closed card cannot be picked for new transactions or used as a payment source.

## Statements and due dates

- **Statement date** = the statement day, or the last day of the month if the month is shorter. For example, day 31 gives 28 Feb.
- **Statement period** = from the day after the previous statement date up to and including this statement date. A transaction dated on the statement date belongs to that statement.
- A statement is **closed** once today is after its statement date. Before that it is the current cycle.
- **Due date rule** can be set two ways:
  - *N days after the statement date*
  - *a day of the month*: the first such day after the statement date, moved to the last day of the month when needed
- If the user enters the actual dates from the bank, those replace the calculated dates for that statement.
- **Statement balance** = previous balance + opening amount in the period + charges − refunds − payments.
  - If the user enters the bank's statement balance, it replaces the calculated one.
- **Remaining due** = statement balance − payments and refunds made after the statement date, up to today. It is never below 0.
- **Statement status** is one of:
  - Nothing due
  - Paid
  - Partly paid
  - Due / Due today
  - Overdue (unpaid after the due date)
  - Carried to the next statement (an older statement that was not fully paid)

All money is calculated with `BigDecimal`. Fern Wallet calculates statements from the transactions the user recorded. They are not official bank statements. Fern Wallet does not calculate interest or late fees; record those as charges.

## Reminders

- A daily WorkManager job checks each card's latest statement.
- It shows one "due soon" notification N days before the due date (N is set per card, 0–10) and one on the due date.
- Notifications are de-duplicated per card, due date and kind. They are cleared once the statement is paid.
- Reminders are skipped when notifications are turned off in Settings, when the card's reminders are off, or when the card is closed.

## Known limitations

- The instrumented migration test (`IvyRoomDatabaseMigrationTest`) compiles but has not run on a device or emulator. The migration SQL is checked against the exported schema 131 on the JVM.
- There is no interest or EMI modelling, no add-on cards, and no shared limits between cards.
- Statements depend on the user recording transactions. Missing transactions make the calculated figures wrong.
