# Module 3 — Build, debug, and review a Java change

**Defect report** — A request for 100.00 is rejected when exactly 100.00 of credit is available. The business rule allows a positive request up to and including the available credit.

How would you narrow that down in a COBOL job? Pick inputs just below, exactly at, and just above the limit. Predict which should pass. You'll hold those inputs steady as you reproduce the failure and trace the decision.

**Learning objective** — Reproduce a boundary defect, use a debugger to explain it, and verify a small fix with a regression test.

## Set up and build

From the repository root of a clean working copy, run `cd modules/03-build-debug-review`. The command-line program takes a positive requested amount and a nonnegative available amount, both in decimal units already rounded to cents. [Module 0 setup](../00-orientation/README.md#set-up-the-environment) · [Module 2 amounts](../02-domain-modeling/README.md)

Module 2 uses `HALF_UP` to normalize raw amounts. Here `setScale(2, RoundingMode.UNNECESSARY)` checks that each input can be expressed exactly in cents. It accepts `12.5000` as `12.50` and throws `ArithmeticException` for `12.501` because reducing the scale would require rounding. Your test calls the rule directly with different scales to verify the numerical comparison.

Compare these expected results with your predictions.

| Requested | Available | Expected outcome |
| ---: | ---: | --- |
| 99.99 | 100.00 | `WITHIN_LIMIT` |
| 100.00 | 100.00 | `WITHIN_LIMIT` |
| 100.01 | 100.00 | `EXCEEDS_LIMIT` |
| 0.01 | 0.00 | `EXCEEDS_LIMIT` |

Build the passing reference in `src/main/java` first. Run the Maven Wrapper for your shell from the lab directory.

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

The reference build reports four passing tests.

## Reproduce the defect

Copy the reported version over the lab's working copy of `CreditLimit.java`. Keep the supplied tests.

| Shell | Command |
| --- | --- |
| macOS or Linux | `cp exercise/CreditLimit.java src/main/java/training/performtoplatform/debugging/CreditLimit.java` |
| Windows PowerShell | `Copy-Item exercise\CreditLimit.java src\main\java\training\performtoplatform\debugging\CreditLimit.java` |
| Windows Command Prompt | `copy /Y exercise\CreditLimit.java src\main\java\training\performtoplatform\debugging\CreditLimit.java` |

Create `CreditLimitRegressionTest.java` alongside the existing test in `src/test/java/training/performtoplatform/debugging`. In your test, use a requested amount of `new BigDecimal("100.0")` and available credit of `new BigDecimal("100.00")`. Write an assertion from the defect report. Decide what the result should be when both amounts represent the same number of cents.

<details>
<summary>Hint — write a single JUnit test</summary>

Use `package training.performtoplatform.debugging;` so the test can call package-private `CreditLimit.allows`. Import `java.math.BigDecimal`, `org.junit.jupiter.api.Test`, and `org.junit.jupiter.api.Assertions.assertTrue` as a static import. Use a package-private class named `CreditLimitRegressionTest`. Annotate its test method with `@Test`, declare the method with return type `void`, and pass the result of the call to `assertTrue`.

</details>

Run the same wrapper command with `clean test "-Dtest=CreditLimitRegressionTest"` in place of `clean verify`. Your test should run once and show that the application returns `false` where the rule expects `true`.

Maven compiles the application before running the tests. Use those classes to reproduce the behavior.

```text
java -cp target/classes training.performtoplatform.debugging.CreditLimitCheck 100.00 100.00
```

The reported version prints `EXCEEDS_LIMIT`. Record both the test name and the command-line result.

## Trace the comparison in the debugger

Both the test and the command-line run show the same failure. Use a breakpoint to see the value that drove the decision. Start `jdb` with the same input and the freshly compiled classes.

```text
jdb -sourcepath src/main/java -classpath target/classes training.performtoplatform.debugging.CreditLimitCheck 100.00 100.00
```

At the debugger prompt, enter each command separately. Wait for the breakpoint before continuing.

```text
stop in training.performtoplatform.debugging.CreditLimit.allows
run
```

Use `where` to see that `CreditLimitCheck.main` called `allows`. Use `next` to step over the comparison and pause on the return statement. Then enter these commands one at a time.

```text
locals
list
cont
```

`locals` shows `comparison = 0`. `BigDecimal.compareTo` returns a negative value when the request is smaller, zero when the amounts are equal, and a positive value when the request is larger. Use `list` to read the return condition. What does that condition return when the comparison is zero? After `cont`, confirm that the program prints `EXCEEDS_LIMIT`.

`CreditLimit.allows` expects non-null amounts in the same unit, with a positive request and nonnegative available credit. The command-line program checks the signs and accepts only amounts representable in whole cents before calling the method.

<details>
<summary>Alternative — use a Java IDE debugger</summary>

Import the lab as a Maven project. Debug `CreditLimitCheck.main` with arguments `100.00 100.00`. Set a breakpoint on the return statement in `CreditLimit.allows`, then inspect `comparison` and the call stack.

</details>

## Fix and review the change

1. Change the comparison in the working copy of `CreditLimit.allows` so equality passes and a larger request still fails. Keep the regression test's expectation.
2. Rerun `clean verify` and confirm that all five tests pass, including the one you wrote. Run the JAR for the reported input and a request one cent above the limit.

   ```text
   java -jar target/module-3-build-debug-review.jar 100.00 100.00
   java -jar target/module-3-build-debug-review.jar 100.01 100.00
   ```

   The JAR prints `WITHIN_LIMIT` and `EXCEEDS_LIMIT`, respectively.

3. Compare the reported source with your fix. The command exits with status 1 when it finds a difference. The diff should show one changed comparison.

   ```text
   git diff --no-index -- exercise/CreditLimit.java src/main/java/training/performtoplatform/debugging/CreditLimit.java
   ```

   Write a short review note that identifies the failing input, the comparison value you observed, and the test that protects the fix.

## Completion criteria

- [ ] Write a regression test for equal amounts with different scales and reproduce the rejection with both that test and the command-line program.
- [ ] Show the debugger stopped in `CreditLimit.allows`. Identify the caller and explain why `comparison = 0` leads to the reported result.
- [ ] Fix the comparison and show five passing tests, including your regression and the supplied cases.
- [ ] Run the packaged JAR for the exact limit and one cent above it. Confirm both outputs against the business rule.
- [ ] Show the one-line diff against the reported version and explain how the regression test captures the boundary.

Next, store a credit request and its history atomically in [Module 4](../04-jdbc-persistence/README.md).
