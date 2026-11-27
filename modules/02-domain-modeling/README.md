# Module 2 — Model credit data and business outcomes

A credit request can now arrive with fractions of a cent, and invoices have an earliest eligible date. What should the validator do with half a cent? What about an invoice dated exactly on the cutoff? Think through those boundaries as you would before changing a COBOL validation paragraph.

**Learning objective** — Represent the request and its outcome in Java, then prove that rounding and the date cutoff match the business rules.

## Predict the outcomes

This lab uses a synthetic rounding and cutoff policy. Read the rules and predict the normalized amount and outcome in each row.

1. Round the entered amount to the nearest cent. If it's halfway between cents, round away from zero.
2. Check the invoice number first and the rounded amount second, in the same order as Module 1. A missing invoice number or an amount of zero or less is grounds for rejection.
3. If those checks pass, reject dates before the cutoff. The cutoff day itself is eligible.

Use `INV-1001` and cutoff `2026-01-01` for each case.

| Case | Entered amount | Invoice date |
| --- | ---: | --- |
| A | 12.505 | `2026-01-01` |
| B | 0.004 | `2026-01-02` |
| C | 0.005 | `2026-01-02` |
| D | 12.50 | `2025-12-31` |
| E | 12.5 | `2026-01-02` |

<details>
<summary>Check your predictions</summary>

| Case | Normalized amount | Outcome |
| --- | ---: | --- |
| A | 12.51 | `VALID` |
| B | 0.00 | `AMOUNT_NOT_POSITIVE` |
| C | 0.01 | `VALID` |
| D | 12.50 | `INVOICE_BEFORE_CUTOFF` |
| E | 12.50 | `VALID` |

</details>

## Build and test

From the repository root, run `cd modules/02-domain-modeling` and build the supplied reference with the Maven Wrapper. [Module 0 setup](../00-orientation/README.md#set-up-the-environment)

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

Look for `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

## Run the JAR

Supply the invoice number, decimal amount, invoice date, and cutoff date in that order. The JAR prints the outcome followed by the rounded amount. Compare these three runs with your predictions for A, B, and D.

```text
java -jar target/module-2-domain-modeling.jar INV-1001 12.505 2026-01-01 2026-01-01
java -jar target/module-2-domain-modeling.jar INV-1001 0.004 2026-01-02 2026-01-01
java -jar target/module-2-domain-modeling.jar INV-1001 12.50 2025-12-31 2026-01-01
```

```text
VALID 12.51
AMOUNT_NOT_POSITIVE 0.00
INVOICE_BEFORE_CUTOFF 12.50
```

Now try February 30. A date before the cutoff can still be a valid calendar date. What do you expect the program to do with an impossible date?

```text
java -jar target/module-2-domain-modeling.jar INV-1001 12.50 2026-02-30 2026-01-01
```

Java reports a `DateTimeParseException` and exits with a nonzero status. The program couldn't turn the text into a date, so it stopped before applying the business rule.

## Read the amount and date checks

`CreditRequest` is a Java record. It groups the invoice number, amount, and business date much like a COBOL group item. Each `new CreditRequest(...)` runs the compact constructor. Java supplies its parameters from the record declaration and stores their final values after the constructor body runs. The constructor uses `BigDecimal` to hold an exact decimal value and stores the amount with two digits after the decimal point. [CreditRequest.java](src/main/java/training/performtoplatform/modeling/CreditRequest.java)

```java
requestedAmount = Objects.requireNonNull(requestedAmount, "requestedAmount")
        .setScale(2, RoundingMode.HALF_UP);
```

`requireNonNull` returns the amount if it is present and throws if it is missing. Reassigning the constructor parameter stores the normalized amount in the record. The separate `requireNonNull(invoiceDate, ...)` call checks the date and leaves the parameter value as supplied. `setScale(2, ...)` applies the lab's rounding policy. That's why the positivity check sees `0.00` for an input of `0.004`.

The validator uses `LocalDate` for calendar dates. `request.invoiceDate().isBefore(cutoffDate)` is true only for dates earlier than the cutoff. The validator returns a `ValidationOutcome` enum, a fixed set of named results. `VALID`, `INVOICE_REQUIRED`, and `AMOUNT_NOT_POSITIVE` carry forward Module 1's `00`, `04`, and `08`. `INVOICE_BEFORE_CUTOFF` names the new rejection. [CreditValidation.java](src/main/java/training/performtoplatform/modeling/CreditValidation.java)

The record declaration supplies methods called accessors, such as `invoiceDate()` and `requestedAmount()`. The parentheses indicate a method call. Calling one on `request` reads the value from that particular object.

### Compare amount and scale

The program creates the `BigDecimal` from the original command-line text, preserving its decimal value before rounding. If `a` is `new BigDecimal("12.5")` and `b` is `new BigDecimal("12.50")`, `a.equals(b)` is `false` because their scales differ. The tests use that behavior to verify that the stored amount has two decimal places. For a business comparison by amount, use `compareTo`. Here `a.compareTo(b)` returns zero. [JDK decimal types](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)

## Diagnose and repair the model

1. In a working copy, change the record constructor from `RoundingMode.HALF_UP` to `RoundingMode.DOWN`. Change the date check from `request.invoiceDate().isBefore(cutoffDate)` to `!request.invoiceDate().isAfter(cutoffDate)`. Keep the test expectations in place.
2. Rerun the build. Find the failures for the half-cent input and for an invoice dated on the cutoff. Use the named cases to find the expected and actual results, then explain which expression caused each failure. [CreditValidationTest.java](src/test/java/training/performtoplatform/modeling/CreditValidationTest.java)
3. Restore the rounding and cutoff rules, then confirm all 16 tests pass. Rerun the JAR examples to check the behavior you observed earlier. The missing-invoice and amount checks still take priority over the date check.

## Reject invoices dated after the business date

A request is now eligible only when its invoice date falls from the existing cutoff through the supplied business date, inclusive. An invoice dated later returns `INVOICE_AFTER_BUSINESS_DATE`. Keep the existing checks in their current order and add the new check after the cutoff check. Use a supplied business date so the same inputs always produce the same result.

1. Add a `LocalDate businessDate` parameter to `validate` and pass it from both callers. In `main`, require five arguments, update the usage message, and parse the business date from `args[4]`. In the test, use `LocalDate.of(2026, 2, 1)` for the business date and append `"2026-02-01"` to the malformed-input fixture's command-line arguments. Run the 16 original cases before adding the rule.
2. Add the enum value and four rows to `validationCases`. Predict the results for valid requests dated January 31, February 1, and February 2, 2026. Also test a request for zero dated February 2. Show the February 2 case with a positive amount failing before you change the rule.
3. Implement the rule with the record accessor and `LocalDate.isAfter`. Run `clean verify` and show 20 passing cases. Run the packaged JAR with `INV-1001 12.50 2026-02-02 2026-01-01 2026-02-01` and check for `INVOICE_AFTER_BUSINESS_DATE 12.50`.

## Completion criteria

- [ ] Predict the five outcomes and normalized amounts before opening the answer key. Explain any differences.
- [ ] Show a named rounding failure and a named cutoff failure after the temporary code changes.
- [ ] Restore the two rules and show all 16 parameterized cases passing.
- [ ] Run the JAR with the sample inputs. Explain how a business rejection appears in the output and how a date parsing failure appears.
- [ ] Explain why the record normalizes the amount before the positivity check, why the cutoff day is eligible, and how the enum carries forward Module 1's outcomes.
- [ ] Show the future-invoice test failing before the rule and passing afterward. Explain why the business date itself is eligible and why a zero amount still takes priority.

Next, investigate a regression with the build and debugger in [Module 3](../03-build-debug-review/README.md).
