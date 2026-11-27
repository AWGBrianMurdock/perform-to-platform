# Module 9 — Deliver a reviewed, reproducible change

A working change is not ready just because it passed once on one workstation. What evidence should a reviewer see? Which checks establish behavior, which establish that the project builds elsewhere, and which questions still require an owner?

**Learning objective** — Deliver a small rule change with a failing regression test, a focused review, a reproducible build, and a documented smoke check.

**Prerequisites** — Complete Modules 0–8 and have a clean course checkout on a branch you can change.

This module is a delivery exercise over the Module 8 project; it does not add another independent Maven project.

## Define the change

In a disposable branch, add the fictional `$1,000.00` maximum to Module 8's `CreditRequestValidator`. Keep the existing rule order: the invoice-required outcome takes precedence over the positive-amount check, which takes precedence over the amount cap.

Use these boundary cases:

| Request | Expected result |
| --- | --- |
| Invoice present; amount `$1,000.00` | `201 Created`; both rows are written. |
| Invoice present; amount `$1,000.01` | `422 Unprocessable Content`; neither row is written. |
| Invoice missing; amount `$1,001.00` | `422 Unprocessable Content` with `INVOICE_REQUIRED`; neither row is written. |

The cap and precedence are fictional course policy. In a real change, identify the domain owner and obtain agreement on the value, comparison, rounding, and precedence before implementation.

## Make the change test-first

1. Confirm `git status --short` shows no unrelated learner changes. Create a branch from the course revision you intend to change.
2. Add `rejectsAmountsAboveTheExerciseLimit` to `CreditApiSecurityTest`. Use a submitter identity and CSRF token as the existing authorized-write tests do. Check the response outcome and verify that neither the request nor history table contains a row.
3. Run that test by itself and observe it fail because the current validator accepts the request. Save the command and failing assertion as evidence; this is the regression gate before the fix.
4. Add a named `AMOUNT_EXCEEDS_LIMIT` outcome. In `CreditRequestValidator`, compare `BigDecimal` values with `compareTo`, preserving the documented rule order. Do not use `equals` for a numeric boundary.
5. Run the focused test again. Add the exact-limit and precedence cases, then run `clean verify` in Module 8.

PowerShell focused-test command, from `modules/08-secure-service` after adding the test:

```text
..\00-orientation\mvnw.cmd --batch-mode --no-transfer-progress "-Dtest=CreditApiSecurityTest#rejectsAmountsAboveTheExerciseLimit" test
```

macOS or Linux:

```text
../00-orientation/mvnw --batch-mode --no-transfer-progress -Dtest=CreditApiSecurityTest#rejectsAmountsAboveTheExerciseLimit test
```

## Review the delivery evidence

Before committing, inspect the change as a reviewer would:

- `git diff --check` reports no whitespace errors.
- The diff contains the named rule, its tests, and no unrelated source or course files.
- The tests cover the accepted boundary, one cent above it, and precedence when two rules reject.
- `clean verify` passes in the changed Maven project, and its log identifies the JDK and test result.
- The manual smoke check uses a training-only user ID and synthetic invoice data. Record the observed health status and accepted/rejected results without recording the password or any credential.
- The build and test failure from step 3 are tied to the same branch and test as the passing result after the fix.

The repository workflow discovers every Maven project and verifies each on Windows and Linux. It also builds and checks the full HTML course site. Submit the branch through the team's normal review process and wait for the workflow result. A local pass establishes evidence from that checkout; the workflow adds evidence from its runner environments. Neither proves that the exercise cap is valid production policy or that the deployment is ready.

In the review summary, state the behavior change, test evidence, CI result, manual smoke result, and any decision still owned by someone else. Do not describe a planned or unrun check as passed.

## Completion criteria

- Show the focused test failing before the implementation and passing after it.
- Explain why `BigDecimal.compareTo` is used and show the three rule-order cases.
- Show the review diff, local build result, CI result, and smoke evidence for the same change.
- Identify the domain-policy decision that the example cannot make for a production system.
- State what each piece of evidence supports and the limits of that evidence.

Next, combine the service and audit lessons in [Module 10: the capstone](../10-credit-decision-capstone/README.md).
