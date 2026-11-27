# Assemble a Java service by hand

A caller asks for the status of a credit request. The service has to report the same answer whether it receives status data from the application or from a small test fixture. How does the caller put those objects together?

This short exercise uses a map-backed status lookup to isolate object construction and substitution. It is a bridge to the framework-managed service in Module 5; the next lab applies the same constructor pattern to the persisted credit workflow.

**Learning objective** — Construct a service and its repository, then give the same service a second repository implementation in a test.

**Pilot estimate** — 20–30 minutes after Module 4. Confirm the pacing during a learner session.

## Run the lookup

From the repository root, run `cd modules/04a-plain-java`. Use the JDK and Maven Wrapper from [Module 0](../00-orientation/README.md#set-up-the-environment).

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

Look for one passing test. From the same directory, run these commands and predict the result for each ID.

```text
java -cp target/classes training.performtoplatform.composition.CreditApplication REQ-1001
java -cp target/classes training.performtoplatform.composition.CreditApplication REQ-9999
```

The commands print `SUBMITTED` and `NOT_FOUND`, respectively.

## Follow the objects

1. In `main`, find where `new InMemoryCreditStatusRepository(...)` creates the repository and `new CreditStatusService(repository)` passes it into the service. The service constructor saves that object in an instance field. `private` limits direct access to the field, and `final` keeps the service from reassigning it. Supplying a dependency through a constructor is called constructor injection. [CreditApplication.java](src/main/java/training/performtoplatform/composition/CreditApplication.java) · [CreditStatusService.java](src/main/java/training/performtoplatform/composition/CreditStatusService.java)
2. The constructor parameter is typed as `CreditStatusRepository`. The interface declares the lookup operation and lets the caller supply any object that implements it. The implementing method is `public` because the interface method is public. `@Override` asks the compiler to check that the signature matches. [CreditStatusRepository.java](src/main/java/training/performtoplatform/composition/CreditStatusRepository.java)
3. The in-memory repository holds a `Map<String, String>`. Its keys are request IDs and its values are statuses. `Map.copyOf` gives the repository an unmodifiable map of the supplied entries. A missing key returns `null` from `get`, and `Optional.ofNullable` turns that into an empty `Optional`. The service supplies `NOT_FOUND` for an empty result. [InMemoryCreditStatusRepository.java](src/main/java/training/performtoplatform/composition/InMemoryCreditStatusRepository.java)

## Substitute a repository in a test

1. In `CreditStatusServiceTest.java`, add a class named `FixedCreditStatusRepository` that declares `implements CreditStatusRepository`. Use a plain `if` in its `findStatus` method to return `Optional.of("APPROVED")` for `REQ-TEST` and `Optional.empty()` for every other ID. Import `java.util.Optional`.
2. Write a second test in `CreditStatusServiceTest` and construct a new `CreditStatusService` with the repository you wrote. Use `assertEquals` to check the result for `REQ-TEST` and for an unknown ID. Run `clean verify` and show two passing tests. [CreditStatusServiceTest.java](src/test/java/training/performtoplatform/composition/CreditStatusServiceTest.java)
3. Point to the line where you choose the implementation in each test. Explain which object decides whether a request exists and which supplies the caller's fallback message.

In [Module 5](../05-spring-boot-workflow/README.md), Spring will construct and supply the dependencies. The service still receives them through its constructor.

## Completion criteria

- Run the packaged lookup for a known request and an unknown request; explain both results.
- Show two passing tests, including the test that uses your second repository implementation.
- Point to where each repository is selected, where the service calls the interface, and where the unknown-request result is chosen.
