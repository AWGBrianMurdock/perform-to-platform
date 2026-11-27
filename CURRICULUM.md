# Curriculum

## Course outcome

By the end of the course, you can make a reviewed change to a small Spring Boot service, explain how a request moves through the application, and show evidence that its business behavior and persistence guarantees hold.

The course uses a synthetic credit-request workflow. Treat the example rules as exercise policy. Before applying the techniques to a real system, confirm its rules and operational requirements with the responsible domain and platform owners.

## Learning sequence and material status

Modules 0–8 and 10 now have Maven projects and learner guides in this checkout; Module 9 is a delivery exercise over Module 8. The current Windows/JDK 21 builds pass in the individual modules. Learner-pilot feedback and hands-on accessibility review remain outstanding. The sequence below describes the intended learning outcomes for all modules.

| Module | Learning outcome | Applied work | Material status |
| --- | --- | --- | --- |
| [0. Set up and trace a Java program](modules/00-orientation/README.md) | Build and launch a Java program; explain what the compiler, Maven, JAR, and JVM do. | Follow an invoice ID from command-line input to program output and inspect it in a debugger. | Runnable lab and guide |
| [1. Carry a business rule into Java](modules/01-business-rules/README.md) | Preserve business behavior while moving a rule from a COBOL paragraph into a Java method. | Translate the validation order, test named cases, and add an amount limit with boundary tests. | Runnable lab and guide |
| [2. Model business data and outcomes](modules/02-domain-modeling/README.md) | Choose Java types that express money, dates, and business outcomes. | Exercise decimal scale and rounding, date cutoffs, and the distinction between business rejection and malformed input. | Runnable lab and guide |
| [3. Diagnose and review a Java change](modules/03-build-debug-review/README.md) | Reproduce a defect, use a debugger to locate it, and verify a focused fix. | Add a regression test for numerically equal `BigDecimal` values with different scales; inspect the change. | Runnable lab and guide |
| [4. Persist a request with SQL and JDBC](modules/04-jdbc-persistence/README.md) | Use parameterized SQL and a transaction to persist related rows atomically. | Force the history insert to fail, verify rollback, retry, and add a history lookup. | Runnable lab and guide |
| [4A. Assemble a service with plain Java](modules/04a-plain-java/README.md) | Explain interfaces, constructor injection, and the choice of an implementation. | Construct a service with a map-backed repository and substitute a second implementation in a test. | Runnable lab and guide |
| [5. Wire the workflow with Spring Boot](modules/05-spring-boot-workflow/README.md) | Explain component scanning, bean creation, repository configuration, and Spring-managed transactions. | Combine validation and persistence in a service; prove that a failure in the Spring transaction rolls back both writes. | Runnable lab and guide; build and service tests pass on Windows with JDK 21 |
| [6. Expose the service through HTTP](modules/06-credit-http-api/README.md) | Map an HTTP request through deserialization, validation, service behavior, and a response. | Exercise submit and status endpoints; verify successful, malformed, rejected, missing, duplicate, and persistence-failure outcomes over HTTP. | Runnable lab and guide; seven HTTP integration tests pass on Windows with JDK 21 |
| [7. Make retries safe](modules/07-idempotent-http-api/README.md) | Explain why request state stays local and how database constraints protect operations under retries and concurrency. | Replay a request with an idempotency key; submit concurrent duplicates; reject a key reused with a different payload. | Runnable lab and guide; eleven integration tests pass on Windows with JDK 21 |
| [8. Secure and diagnose the service](modules/08-secure-service/README.md) | Apply access rules and use health and request-level diagnostics to investigate failures. | Test permitted and denied actions, externalize credentials, and inspect the public health response without exposing sensitive values. | Runnable lab and guide; seven security tests pass on Windows with JDK 21 |
| [9. Deliver a reviewed change](modules/09-reviewed-delivery/README.md) | Explain how a regression test, code review, and continuous integration contribute to a reproducible change. | Add a fictional amount cap test-first, observe the regression gate fail, implement it, and review the build, workflow, and smoke evidence. | Delivery exercise guide over Module 8; no separate Maven project |
| [10. Capstone: record a credit decision](modules/10-credit-decision-capstone/README.md) | Preserve allowed state transitions across API, authorization, service logic, database, and history. | Approve or deny with decision data; verify normal, forbidden, repeated, rollback, and competing-decision cases. | Runnable capstone and guide; fourteen tests pass on Windows with JDK 21 |

## How the sequence builds

The early labs establish the behavior and types of the example before persistence. Module 4 proves the atomic write directly with JDBC. Module 4A introduces the object-construction pattern without a framework. Module 5 carries that workflow into Spring, where Spring constructs the components and manages the transaction. Modules 6–8 add HTTP, retries, access control, and safe health reporting. Module 9 rehearses a reviewed change. Module 10 combines the request flow with a single-winner decision transition and audit history.

Avoid introducing framework annotations as vocabulary to memorize. Have learners trace the object or request at runtime, identify which component owns each decision, and verify the behavior with a focused test.

## Pilot and assessment

Use the current Modules 0 and 1 pilot to learn whether the setup, pacing, and explanations work for the intended audience. Record where learners need help and what evidence shows they can explain the behavior. Revise time estimates from observations rather than assuming the published estimates fit every workstation or learner.

For each new module, define completion criteria as observable behavior and explanation. A passing build is useful evidence, but it does not by itself establish that learners understand the rule, the failure mode, or the limits of the test.
