# Glossary

Review a term and jump to the lab step where you can see it in action.

## Breakpoint

A point where a debugger pauses execution so you can inspect the program's state. In the preview lab, pause inside `previewMessage` and check which invoice number `main` passed in. [Module 0 debugger exercise](../modules/00-orientation/README.md#trace-execution-in-the-debugger)

## Bytecode

The instructions the Java compiler writes to `.class` files for the JVM to execute. Find `CreditPreview.class` in the JAR and optionally use `javap` to match a bytecode instruction to a Java method call. [Module 0 JAR inspection](../modules/00-orientation/README.md#inspect-the-jar)

## JAR

A Java archive that bundles compiled classes and related files. The lab's executable JAR includes a manifest entry naming the class to start. [Run the JAR](../modules/00-orientation/README.md#run-the-jar) · [Inspect its manifest](../modules/00-orientation/README.md#inspect-the-jar)

## JDK

The Java Development Kit includes tools to compile, package, run, and debug Java programs. The labs use JDK 21. [Module 0 version checks](../modules/00-orientation/README.md#set-up-the-environment)

## JUnit

The testing framework used in the labs. The preview test asserts a single result. The credit-validation test checks the same rule across several named inputs. [Module 0 build and test](../modules/00-orientation/README.md#build-and-test) · [Module 1 build and test](../modules/01-business-rules/README.md#build-and-test)

## JVM

The Java Virtual Machine executes compiled Java bytecode. Follow the lab from the `java -jar` command to the class named by the manifest and the `main` method that starts the program. [Module 0 JAR inspection](../modules/00-orientation/README.md#inspect-the-jar)

## Maven Wrapper

The checked-in scripts and configuration that fetch and run the course's Maven version. Use the wrapper from the lab directory so Maven builds and tests the intended lab. [Module 0 build command](../modules/00-orientation/README.md#build-and-test) · [Module 1 build command](../modules/01-business-rules/README.md#build-and-test)

## Parameterized test

A test that runs the same assertion logic with different inputs and expected outcomes. In Module 1, each named case checks the returned status. Replace the rule with `return VALID;` to see which cases catch the missing checks. [Module 1 translation exercise](../modules/01-business-rules/README.md#translate-the-paragraph)

## Short-circuit evaluation

Java's `||` skips its right operand if the left operand already evaluates to true. In the credit rule, the `null` check keeps the method from calling `isBlank()` on a missing string reference. [Module 1 translation hint](../modules/01-business-rules/README.md#translate-the-paragraph)

## Status code

A value telling the caller which business outcome occurred. In the credit rule, `04` means the invoice number is missing. Keeping the status as text preserves the leading zero. [Predict the COBOL statuses](../modules/01-business-rules/README.md#read-the-cobol-rule) · [Run the Java examples](../modules/01-business-rules/README.md#run-the-jar)

## HTTP status code

A protocol response value, such as `201 Created`, `400 Bad Request`, or `409 Conflict`. It describes how the HTTP request completed; a response body can carry the more specific business outcome. [Module 6 response contract](../modules/06-credit-http-api/README.md#read-the-http-contract)

## Idempotency key

A client-supplied value that lets a service recognize a retry of the same operation. The lab stores the key with the request and uses a database constraint to arbitrate concurrent submissions. [Module 7 retry path](../modules/07-idempotent-http-api/README.md#trace-the-retry-path)

## CSRF token

A value a client must return with an unsafe request so that a browser's automatically sent credentials cannot authorize a forged cross-site action. The service keeps CSRF protection enabled for POST requests. [Module 8 security rules](../modules/08-secure-service/README.md#trace-the-security-rules)

## Role

An authority attached to an authenticated account that controls which actions it can take. In the course service, submitters, readers, and deciders have different access. [Module 8 security rules](../modules/08-secure-service/README.md#trace-the-security-rules) · [Module 10 decision contract](../modules/10-credit-decision-capstone/README.md#read-the-decision-contract)

## Health endpoint

An operational endpoint that reports a summary of service health. Public health data should be limited to what operators and monitoring need; the course endpoint hides database component details. [Module 8 health check](../modules/08-secure-service/README.md#configure-and-run)

## Audit event

A persisted record that describes a business event and stores the acting user's ID. In the capstone, the submission and final decision are recorded in history within the transaction. [Module 10 decision flow](../modules/10-credit-decision-capstone/README.md#trace-a-decision)
