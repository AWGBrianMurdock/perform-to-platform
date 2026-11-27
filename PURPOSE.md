# Course purpose and design

## Purpose

Perform to Platform helps experienced COBOL developers build, test, and maintain enterprise Java and Spring Boot applications. Learners bring substantial programming and production experience. The course teaches the Java language, application model, tools, and runtime practices they need for a new platform without treating them as beginners.

The course follows one synthetic credit-request workflow from a business rule to a running service. The example gives learners a stable context for reasoning about behavior, persistence, transactions, APIs, and operational concerns. Instructors should make clear which rules are sample policy and which rules come from an organization's own domain experts.

## Learning principles

- Begin with a real task and its expected behavior. Introduce a Java or Spring concept when it helps learners complete or explain that task.
- Treat existing business knowledge as an asset. Ask learners to state invariants, boundary cases, failure modes, and expected outcomes before changing code.
- Compare COBOL and Java precisely. Explain useful parallels, then identify where data lifetime, control flow, type systems, error handling, build, and runtime behavior differ.
- Keep business intent separate from implementation choices. Tests can demonstrate observed behavior; learners still need a domain owner to confirm the rule.
- Store the stable user ID that performed an action. Resolve a display name separately when the application needs to show one.
- Make reasoning visible. Learners predict outcomes, inspect code, run focused checks, explain the evidence, and review the change.
- Build toward a coherent application in small, runnable steps. Revisit earlier guarantees as the implementation moves from plain Java to Spring-managed components and an HTTP service.
- Use synthetic data and explicit exercise boundaries. Exercises should be safe to run repeatedly and should not depend on confidential production data.
- Respect different levels of Java and tooling experience. State prerequisites, provide recovery steps, and offer optional depth without making core concepts optional.

## Audience and prerequisites

The primary audience is developers with production COBOL experience who are moving into Java application development. Learners should be comfortable reading business logic, following a data flow, using a command line, and discussing tests and failure cases. The course teaches Java and Spring; it does not assume prior Java experience. Each module states its software and setup prerequisites.

## Scope

The target course covers the Java execution model and language, business-rule translation, domain modeling, tests and debugging, SQL and JDBC, dependency injection, Spring Boot, HTTP APIs, transactions, retries and concurrency, security, diagnostics, and delivery through review and continuous integration. The current module status is tracked in [the curriculum](CURRICULUM.md).

The course uses the credit-request workflow as a teaching example, not as a production credit policy or reference architecture. It does not replace organization-specific security, privacy, regulatory, deployment, or operational standards.

## Teaching format

Each lab should give learners a concrete outcome, explain the starting state, and provide a short path to a working result. A consistent lesson includes:

1. A realistic scenario and the behavior to preserve or add.
2. A prediction or small investigation before the implementation is revealed.
3. A runnable starting point and one focused change.
4. Checks that connect the expected behavior to evidence.
5. A short explanation of the Java or Spring concepts used.
6. Recovery guidance and optional exploration where useful.
7. Completion criteria stated as observable learner actions.

Use this structure as a guide, not a form. Keep only the sections that help the specific lesson.
