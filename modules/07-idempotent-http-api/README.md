# Module 7 — Make request retries safe

A client can lose the response after the service commits a request. If it sends the same operation again, the server needs to tell a retry from a new operation. A request ID alone rejects a duplicate, but it does not replay the original result. What needs to be stored so the server can recognize the same operation, even when two copies arrive together?

**Learning objective** — Explain how a client-supplied idempotency key and a database uniqueness constraint make repeated submissions safe, and verify sequential and concurrent retries over HTTP.

**Prerequisites** — Complete Modules 0–6. This lab repeats the Module 6 workflow in a separate project and adds an idempotency key.

## Build and run the lab

From the repository root, enter `modules/07-idempotent-http-api`. Use JDK 21 and the Maven Wrapper from Module 0.

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

Look for eleven passing tests and `BUILD SUCCESS`. The integration tests send requests to a server on a random port. One test sends two concurrent copies and checks the persisted rows as well as both responses.

Run the packaged service with `java -jar target/module-7-idempotent-http-api.jar`. It listens on port 8080 on the local machine and uses an in-memory H2 database. Stop it when you finish; the sample data is discarded. Use only synthetic data and do not expose this training service on a network.

Submit a request with an idempotency key:

```powershell
$headers = @{ 'Idempotency-Key' = 'course-key-001' }
$body = @{ requestId = 'REQ-RETRY-001'; invoiceNumber = 'INV-2001'; requestedAmount = 18.25 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/credit-requests' -Headers $headers -ContentType 'application/json' -Body $body
```

The first accepted call returns `201 Created` with `replayed: false`. Submit the same request again with the same key. The server returns `200 OK` with `replayed: true`; it does not add a second request or history row. A key used with different request data returns `409 Conflict`.

## Trace the retry path

Start at `CreditRequestController.submit` and follow the `Idempotency-Key` header into `CreditSubmissionService`.

1. The service validates the key format and looks for a previously stored key before applying business validation. If it finds the same key and matching request data, it reports a replay. If the request data differs, it returns `409`, even when the changed payload would also be rejected by a business rule.
2. If the key is not stored, the service validates the business request. Only accepted submissions claim a key; a first-time business rejection writes no data and leaves the key available for a corrected request.
3. For an accepted new request, `CreditSubmissionWriter` inserts the request, history event, and idempotency record in one transaction. The table's primary key is the concurrency control: if two valid requests race, only one transaction can commit that key.
4. A uniqueness error can be raised only after the winning transaction commits. The service then reads the stored key again. Matching data becomes a replay; different data becomes a conflict. If the duplicate came from a request ID used with a different key, there is no matching key record, so the existing request ID conflict is returned.

The initial lookup helps the common retry path, but it cannot prevent two requests from both observing that a key is absent. The unique database constraint is the part that arbitrates that race. The request, history row, and key record must stay in one transaction so a failure cannot reserve a key without its request or leave a request without the key.

The comparison uses the request ID, invoice number, and normalized amount stored for the original request. In a real service, define which fields make two operations equivalent, how long keys remain available, how clients generate them, and whether rejected outcomes are also remembered. This lab's successful-only policy is an exercise choice.

## Read the test evidence

Open `CreditRequestApiTest` and group its cases:

- The replay test checks the original and repeated status codes, the `replayed` value, and that each table still contains one row.
- The key-reuse test checks that a different payload receives `409` without adding rows, including when its amount would otherwise be rejected.
- The concurrency test releases two HTTP requests together, checks for one `201` and one replay `200`, then checks one request, history event, and key record.
- The duplicate request ID test uses two different keys and confirms that the request and history counts remain one.

These tests exercise H2 and the checked-in service. They show how the database constraint handles this scenario in the lab; they do not establish behavior for a different production database or prove retry retention and client behavior in a deployed system.

## Extend the exercise

Change the original request amount while keeping the same idempotency key. Add a test that expects `409` and proves the stored amount is unchanged. Then change only the scale, for example from `18.25` to `18.250`, and decide whether those requests represent the same operation. Record the rule before implementing it; the domain model normalizes amounts to cents before persistence.

Also test a missing key and a key containing a space. Both are malformed API input and receive `400`; they are different from a key that is valid but conflicts with a previously submitted payload.

## Completion criteria

- Explain why the server needs a key in addition to the request ID.
- Show a successful first call and a replay, then verify both the HTTP responses and the persisted row counts.
- Explain why a read-before-write check alone cannot make concurrent requests safe and identify the database uniqueness constraint that arbitrates the race.
- Show that a key reused for a different payload returns `409` and leaves the first request unchanged, even when the changed amount is nonpositive.
- State this lab's successful-only key policy and identify retention, request equivalence, and production-database concurrency as decisions to verify for a real service.

Next, add access rules and operational diagnostics in [Module 8](../08-secure-service/README.md).
