# Module 5 — Wire the credit workflow with Spring Boot

In Module 4, you controlled a JDBC transaction directly. In Module 4A, you passed a repository into a service with a constructor. Now follow the same two-write operation through Spring Boot. Which component creates the service? Where does the transaction begin? What remains in the database if the second write fails?

**Learning objective** — Trace Spring's construction of a service and JDBC repository, then show that the service transaction keeps a request and its initial history entry atomic.

**Prerequisites** — Complete Modules 0, 1, 3, 4, and 4A. Module 2 provides the `BigDecimal` and explicit-outcome model used here.

## Build and run the supplied workflow

From the repository root, run `cd modules/05-spring-boot-workflow`. Use the JDK 21 and Maven Wrapper from Module 0.

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

Look for three passing tests and `BUILD SUCCESS`. The tests start the Spring application context and exercise the service bean against an embedded H2 database. The request record applies Module 2's `HALF_UP` cent normalization before validation and persistence. The database contains only synthetic course data.

Run the packaged application:

```text
java -jar target/module-5-spring-boot-workflow.jar
```

After Spring starts, the demo runner submits a sample request and prints:

```text
Submission result for REQ-DEMO: ACCEPTED
```

The request and history row live in an in-memory database. They disappear when the application exits.

## Trace the objects Spring creates

Start with `CreditWorkflowApplication`. `@SpringBootApplication` identifies the application configuration, enables auto-configuration, and starts component scanning from this package. Open the constructor of `CreditSubmissionService`. Its required dependencies are `CreditRequestValidator` and `CreditRequestRepository`.

Find the components that satisfy those constructor parameters:

1. `CreditRequestValidator` is registered with `@Component`.
2. `JdbcCreditRequestRepository` implements `CreditRequestRepository` and is registered with `@Repository`.
3. Spring supplies the auto-configured `JdbcTemplate` to the repository constructor. `application.properties`, the H2 dependency, and `schema.sql` determine how the database is created for this lab.
4. `CreditDemo` receives the service through its constructor. Its `run` method is called after the application context starts.

Draw the object path from `CreditWorkflowApplication` to the validator and repository. Compare where Module 4A constructed each object with where Spring constructs them here. The service still receives its dependencies through a constructor; Spring chooses and supplies the implementations.

The SQL remains visible in `JdbcCreditRequestRepository`. `JdbcTemplate` binds the values and manages JDBC resources. It also obtains connections that participate in a Spring-managed JDBC transaction.

## Trace the transaction boundary

Open `CreditSubmissionService.submit`. The service checks the business rule before writing. If the request is valid, it asks the repository to insert a request and then its initial history entry. The method is marked `@Transactional`.

Spring applies transaction behavior when a call comes through the managed service bean. `CreditSubmissionServiceTest` receives that bean with `@Autowired`, then calls `submit`. Spring's transaction interceptor starts the transaction before the method and completes it after the method returns. An unchecked data-access exception triggers rollback by default. [Spring transaction behavior](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html)

The test `rollsBackBothWritesWhenHistoryInsertFailsAndAllowsRetry` passes a blank submitter user ID. The database constraint rejects the second insert after the request row has been written. The test then queries both tables and confirms neither row remains. It retries the same request with a valid synthetic user ID and confirms both rows exist. The history records an ID so the application can choose when and how to display a person's name.

Run only that test while you investigate the failure path:

```text
../00-orientation/mvnw clean test "-Dtest=CreditSubmissionServiceTest#rollsBackBothWritesWhenHistoryInsertFailsAndAllowsRetry"
```

On Windows, replace the wrapper path with `..\00-orientation\mvnw.cmd`.

In a disposable copy, remove `@Transactional` from `submit` and rerun the focused test. Predict the table state before running it. Without the service transaction, the first insert commits on its own before the history insert fails. Restore the annotation and rerun `clean verify`.

## Extend the business rule

The current service preserves the missing-invoice check before the positive-amount check. Add the $1,000 maximum from Module 1 to this workflow.

1. Add a test for exactly `$1,000.00` and one for `$1,000.01`. Also add a case where the invoice is missing and the amount exceeds the limit. Write the expected outcome for each case before changing the validator.
2. Add a named `SubmissionOutcome` for an amount above the limit. Implement the limit with `BigDecimal.compareTo` so equality remains eligible.
3. Run the focused test class before and after the change. Confirm the request and history tables remain empty for every business rejection.

The exercise limit is fictional. In a real application, the responsible domain owner defines the rule and its precedence. The course code does not define a production credit policy.

## Completion criteria

- Build and run the application. Identify the sample output and explain when its in-memory data is lost.
- Trace how Spring supplies the validator, repository, `JdbcTemplate`, and service. Compare the service constructor with Module 4A.
- Explain why the SQL stays in the repository and why the service owns the transaction boundary.
- Show the forced history failure, confirm both writes rolled back, and retry the same request successfully.
- Remove `@Transactional` in a disposable copy, predict and observe the partial write, restore the annotation, and show the focused test passing again.
- Add and test the amount limit, including equality, one cent over, and precedence when more than one rule rejects the request.

Next, expose this service through an HTTP endpoint in [Module 6](../06-credit-http-api/README.md).
