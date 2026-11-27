# Module 4 — Persist a credit request with SQL and JDBC

A credit request needs two rows. The request insert succeeds, then the history insert fails. A second operator looks up the request by ID. What should they see if both writes belong to the same unit of work?

Think about how you would check this in a COBOL job that stopped after its first write. Make a prediction. In this lab, you'll submit one request successfully, force the second write to fail on another request, and check what the database kept.

**Learning objective** — Use Java and SQL to save both rows in one transaction and prove what happens when the history insert fails.

## Set up and build

Use the JDK and Maven Wrapper from Module 0. From the repository root, run `cd modules/04-jdbc-persistence`. The build includes H2, a database that runs inside the Java program. H2 lets you run the lab without a database server. Verify SQL dialect, locking, and isolation behavior against the production database when applying these ideas at work. [Module 0 setup](../00-orientation/README.md#set-up-the-environment) · [H2 embedded mode](https://www.h2database.com/html/features.html#connection_modes)

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

Look for three passing tests and `BUILD SUCCESS`. The demo saves rows in `target/credit-lab.mv.db`, so separate runs can look them up. A build that starts with `clean` removes that demo file.

## Commit and retrieve a request

Run these commands from the lab directory. The `submit` arguments are the request ID, invoice number, decimal amount, and synthetic user ID of the person submitting the request. The history stores the user ID; an application can look up a display name separately when needed. Use an amount that is already rounded to cents.

`CreditLab` uses a `switch` statement to select one command from the first argument. An arrow case runs only its own block. The `submit` block uses the same `setScale(2, UNNECESSARY)` check as Module 3 to require an amount expressible in whole cents.

```text
java -jar target/module-4-jdbc-persistence.jar init
java -jar target/module-4-jdbc-persistence.jar submit REQ-1001 INV-1001 12.50 USER-TRAINEE-001
java -jar target/module-4-jdbc-persistence.jar find REQ-1001
```

```text
SCHEMA_READY
SUBMITTED REQ-1001
FOUND REQ-1001 INV-1001 12.50
```

`init` creates the tables. The separate `find` run confirms the request survived after the program that submitted it exited.

## Force a failure and verify rollback

Try the failure from the opening question. Use a new request ID and pass one space for the submitter user ID. The first insert has a valid ID, invoice, and amount. The history table requires a nonblank user ID, so its insert fails.

```text
java -jar target/module-4-jdbc-persistence.jar submit REQ-ROLLBACK INV-1002 5.00 " "
java -jar target/module-4-jdbc-persistence.jar find REQ-ROLLBACK
```

The first command reports a `SQLException` for `NONBLANK_HISTORY_USER_ID`. That constraint is on the history table. Run the lookup separately after the error. It prints `NOT_FOUND REQ-ROLLBACK`. Compare the result with your prediction, then retry with the same ID and a valid submitter user ID.

```text
java -jar target/module-4-jdbc-persistence.jar submit REQ-ROLLBACK INV-1002 5.00 USER-TRAINEE-001
java -jar target/module-4-jdbc-persistence.jar find REQ-ROLLBACK
```

The retry prints `SUBMITTED REQ-ROLLBACK` and the lookup prints `FOUND REQ-ROLLBACK INV-1002 5.00`. Reusing the ID succeeds because the failed attempt left no request row behind.

Here you have evidence of rollback before retrying. In [Module 7](../07-idempotent-http-api/README.md#trace-the-retry-path), a caller retries after losing the response and has to allow for the possibility that the first request already committed.

## Trace the two writes

In `CreditLab.main`, `new CreditRepository(JDBC_URL)` creates an object from the `CreditRepository` class. The constructor saves the database URL in that object's `jdbcUrl` field. Calls such as `repository.submit(...)` use that instance's configuration when opening connections. Calling the same method on a repository constructed with a different URL would use that repository's configuration. [CreditLab.java](src/main/java/training/performtoplatform/jdbc/CreditLab.java)

Open `submit` and follow the connection passed into both inserts. JDBC is the Java API used here to talk to the database. A JDBC `Connection` represents one database session. `setAutoCommit(false)` keeps each statement from committing on its own. [CreditRepository.java](src/main/java/training/performtoplatform/jdbc/CreditRepository.java) · [JDBC connection reference](https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/Connection.html)

```text
One connection with auto-commit off
  Insert the request
    ├─ History insert succeeds → commit both rows
    └─ History insert fails    → roll back the request insert
```

Find the two insert calls inside `submit`. Which lines run before the error, and which line keeps the first write from surviving it?

`throws SQLException` declares a checked exception. A caller must handle it or allow it to propagate by declaring it. The `catch (SQLException | RuntimeException failure)` block handles either type by attempting rollback. It then rethrows the original failure.

`try (...)` is try-with-resources. Java closes the declared connection, statements, and results when their blocks exit, including after a failure. Inside the connection's block, `submit` still controls the transaction with `commit()` or `rollback()`. Compare this with closing a COBOL cursor or file on both the normal and error paths.

<details>
<summary>Explore a rollback failure</summary>

If rollback also fails, `failure.addSuppressed(rollbackFailure)` attaches that second error to the first. A caller can investigate both failures while retaining the error that triggered rollback.

</details>

## Find where Java passes values to SQL

In `insertRequest`, the three `?` placeholders stand for the request ID, invoice number, and amount, in that order. If you have used host variables in embedded SQL, the idea is familiar. A JDBC `PreparedStatement` binds the values separately from the SQL text.

The triple double quotes around the SQL form a text block, a Java string that can span lines.

```java
statement.setString(1, request.requestId());
statement.setString(2, request.invoiceNumber());
statement.setBigDecimal(3, request.requestedAmount());
statement.executeUpdate();
```

Follow the same idea in `findById`. The new connection runs a query for the supplied ID. Its `ResultSet` holds the returned rows. `next()` checks whether a row exists before Java reads its columns.

`Connection`, `PreparedStatement`, and `ResultSet` are Java interfaces. They declare operations that the database driver's objects implement. The lab opens connections with `DriverManager`. An application can use a pooled data source to borrow and return connections while keeping the same JDBC interfaces.

The lookup returns `Optional<CreditRequest>`, which represents a request that may be absent. In the CLI, `map` runs the expression beginning `request ->` when a request is present. That expression is a lambda, a short function that formats the found request. `orElse` supplies the missing-request message when the lookup is empty.

## Check both tables

The command-line lookup checked the request table. To check the history table too, read the test `rollsBackRequestWhenHistoryInsertFailsAndAllowsRetry`. Look for three pieces of evidence. The exception names the history constraint. Queries through fresh connections find neither row. A retry with the same ID writes one request and one history entry. Another test checks that a previously committed request and its history survive the rollback. Each test uses its own temporary H2 database. [CreditRepositoryIntegrationTest.java](src/test/java/training/performtoplatform/jdbc/CreditRepositoryIntegrationTest.java)

<details>
<summary>Optional exploration — read the SQL error</summary>

The database reports SQLSTATE `23513` for the check constraint violation. The test checks that code and the constraint name to identify which insert failed. The success test uses an invoice number with an apostrophe to illustrate that a bound parameter reaches the database as a value.

</details>

## Show why auto-commit matters

1. In a disposable lab copy, temporarily remove `connection.setAutoCommit(false)` from `submit`. Predict which table will contain a row after the failed write.
2. Use the same Maven Wrapper command from the build section, with the arguments below in place of `clean verify`.

   ```text
   clean test "-Dtest=CreditRepositoryIntegrationTest#rollsBackRequestWhenHistoryInsertFailsAndAllowsRetry"
   ```

3. Find the failed state assertion. With auto-commit enabled, the first insert commits before the second insert fails, so the lookup finds the request row. The history query still finds no row.
4. Restore `setAutoCommit(false)`, rerun `clean verify`, and confirm all three integration tests pass.

## Add a request history lookup

An operator needs to see the history for one request in the order the events were recorded. In your working copy, add a package-private `CreditHistoryEntry` record with `eventType` and `submittedByUserId` string components in the same package as `CreditRepository`. Write `List<CreditHistoryEntry> findHistoryByRequestId(String requestId) throws SQLException` in the repository.

1. Select `event_type` and `submitted_by_user_id` from `credit_history`, filter by the bound request ID, and order by ascending `history_id`. Use `findById` as a model for binding the parameter and closing the resources.
2. Import `java.util.List` and `java.util.ArrayList` and create a list with `List<CreditHistoryEntry> entries = new ArrayList<>();`. `List<CreditHistoryEntry>` is the ordered collection interface for these entries. `ArrayList` provides the implementation. Use `while (result.next())` to advance once per row and add a `CreditHistoryEntry` each time. Return the empty list when the query finds no rows.
3. Add an integration test for the new method. Submit two requests with different IDs and user IDs. Insert an `APPROVED` history row for the first request directly in the test database. Look up that request and use `List.of(...)` to assert that only its two events are returned, with `SUBMITTED` before `APPROVED`.

   <details>
   <summary>Hint — insert a second event in the test database</summary>

   In the test, open a connection as `historyFor` does. Prepare `INSERT INTO credit_history (request_id, event_type, submitted_by_user_id) VALUES (?, ?, ?)`. Bind the first request's ID, `"APPROVED"`, and `"USER-REVIEWER-001"` in that order. Run `executeUpdate()` and close the connection before calling the new lookup. This insert sets up a test case for multiple events.

   </details>

4. In the existing forced-failure test, add an assertion that the new lookup is empty after rollback and shows one entry after the retry. Keep the direct database assertions and their test helper to check the transaction independently.
5. Run `clean verify` and show four passing integration tests. Explain how the query limits the results to the requested ID and how the loop behaves when the database returns no rows.

## Completion criteria

- [ ] Build the lab and show three passing integration tests.
- [ ] Submit and retrieve a request in separate command-line invocations. Identify where the `PreparedStatement` binds each request value.
- [ ] Force the history constraint violation, show the failed request is absent, and submit the same ID successfully with a valid user ID.
- [ ] Explain how the integration tests check both tables from fresh connections and verify that a previously committed request survives.
- [ ] Reproduce the partial write with auto-commit, restore the transaction, and explain why both inserts have to share one connection.
- [ ] Implement the history lookup and show the tests for two ordered events, no history after rollback, and one entry after retry.

Next, assemble a service and its repository by hand in the [plain-Java exercise](../04a-plain-java/README.md).
