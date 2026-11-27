# Module 6 — Expose the credit workflow through HTTP

Modules 4 and 5 proved that the request and its initial history row belong in one transaction. Now follow a client request through Spring MVC: how does JSON become a Java value, where does field validation stop and business validation begin, and how should each outcome appear over HTTP?

**Learning objective** — Trace an HTTP request through deserialization, request validation, business logic, persistence, and response handling; verify the contract with requests to a running server.

**Prerequisites** — Complete Modules 0, 1, 2, 4, and 5. Module 3 provides useful practice writing and reading tests.

## Build and run the API

From the repository root, enter `modules/06-credit-http-api`. Use JDK 21 and the Maven Wrapper from Module 0.

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

The build starts a Spring Boot server on a random port for HTTP integration tests. Look for seven passing tests and `BUILD SUCCESS`. They exercise the API, database effects, and a controlled repository failure after the request row has been inserted.

Run the packaged application:

```text
java -jar target/module-6-credit-http-api.jar
```

The service listens on port 8080 on the local machine. Its H2 database is in memory and disappears when the process exits. Submit only the synthetic course data shown here, and do not expose this unauthenticated training service on a network.

In a second terminal, submit a request:

```json
{
  "requestId": "REQ-HTTP-001",
  "invoiceNumber": "INV-1001",
  "requestedAmount": 12.505
}
```

On macOS or Linux, send it with:

```text
curl -i -X POST http://localhost:8080/credit-requests -H 'Content-Type: application/json' -d '{"requestId":"REQ-HTTP-001","invoiceNumber":"INV-1001","requestedAmount":12.505}'
```

In PowerShell, send it with:

```powershell
$body = @{ requestId = 'REQ-HTTP-001'; invoiceNumber = 'INV-1001'; requestedAmount = 12.505 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/credit-requests' -ContentType 'application/json' -Body $body
```

An accepted request returns `201 Created`, a `Location` header for the new request, and a response with outcome `ACCEPTED`. The service stores the amount as `$12.51` under Module 2's exercise rounding rule. Retrieve its status:

```text
curl -i http://localhost:8080/credit-requests/REQ-HTTP-001
```

```powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/credit-requests/REQ-HTTP-001'
```

The status response contains `SUBMITTED`. Stop the server before repeating the exercise; the database is deliberately not durable.

## Read the HTTP contract

| Request | Outcome | HTTP response | Persistence |
| --- | --- | --- | --- |
| `POST /credit-requests` with a valid request | `ACCEPTED` | `201 Created`, response body, and `Location` header | Request and history rows are committed together. |
| `POST /credit-requests` with a missing or blank invoice | `INVOICE_REQUIRED` | `422 Unprocessable Content` | No rows are written. |
| `POST /credit-requests` with malformed JSON or invalid request fields | — | `400 Bad Request` with a problem response | No rows are written. |
| `GET /credit-requests/{requestId}` for an unknown ID | — | `404 Not Found` | No change. |
| `POST /credit-requests` with an ID already in use | — | `409 Conflict` | The existing request remains unchanged. |
| A persistence operation fails unexpectedly | — | `503 Service Unavailable` with a generic problem response | The service transaction rolls back; database details are not returned. |

These statuses are the contract for this exercise. Confirm status codes, error details, retry rules, and information-disclosure requirements with the owners when designing a real API.

## Trace one request

Use `CreditRequestController.submit` as the entry point and follow the calls:

1. Spring MVC reads the JSON body into `SubmitCreditRequest`. Jakarta Bean Validation checks the request ID format, invoice length, and presence of an amount. These checks describe whether the supplied fields have an acceptable API shape.
2. The controller creates the domain `CreditRequest` and calls `CreditSubmissionService`. The validator applies the exercise rules: an invoice must be present, then the amount must be positive. A business rejection is a validly shaped request whose rule outcome is not accepted.
3. The service writes the request and its initial history event in one Spring-managed transaction. `JdbcCreditRequestRepository` owns the SQL; the service owns the workflow and transaction boundary.
4. The controller maps the outcome to `201` or `422`. `CreditApiExceptionHandler` maps malformed input, missing IDs, duplicate IDs, and data-access failures to HTTP responses.

Compare the HTTP route to Module 5's command-line demo. The workflow and transaction stay in the service; the controller handles transport concerns. Read `CreditRequestApiTest` to see the API test send a real request to a server running on a random port, then query the database for the effects. `CreditRequestApiFailureTest` substitutes a repository that throws a controlled failure and checks that its internal message is absent from the response.

The request body does not contain a submitter identity. `CourseUserIdentity` supplies the synthetic `USER-COURSE-001` value for this exercise. It is not authentication or authorization; later security work must obtain the acting user ID from an authenticated principal and enforce access rules.

## Extend the contract

Add an HTTP integration test for a non-positive amount.

1. Before editing, write down the expected status, response outcome, and number of request and history rows.
2. Send a request with an invoice and an amount of `0.00`. Expect a business rejection with `422`; verify neither table gained a row.
3. Change the amount to `0.01` and show the accepted path still returns `201`.
4. Run `clean verify` and explain how this differs from a malformed request body that receives `400`.

The sample request ID pattern is an API exercise rule. It is not inferred from the COBOL field layout or a production interface. When adapting a legacy interface, confirm its length, character set, numeric range, and rejection behavior before defining the Java request contract.

## Completion criteria

- Build the lab and send a request to the running service. Explain the response status, body, and `Location` header.
- Trace JSON deserialization, request-field validation, business validation, the service transaction, SQL, and response mapping.
- Show evidence for accepted, rejected, malformed, missing, duplicate, and persistence-failure outcomes. Distinguish response evidence from the database checks that establish whether rows were written.
- Explain why the request does not choose its submitter user ID and why the course placeholder cannot be used as a security control.
- Add and run the non-positive amount test, then explain the difference between `400` and `422` in this API.

Next, make repeated requests safe in [Module 7](../07-idempotent-http-api/README.md).
