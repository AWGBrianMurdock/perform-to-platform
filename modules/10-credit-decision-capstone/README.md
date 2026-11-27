# Module 10 — Capstone: record a credit decision

Bring the request workflow, authorization, transactions, and history together. Two authorized reviewers may act on the same submitted request at nearly the same time. How will the service preserve one valid outcome and leave an audit trail that identifies the acting user?

**Learning objective** — Trace and verify a decision from an authenticated HTTP request through authorization, an atomic state transition, persistence, and history.

**Prerequisites** — Complete Modules 0–8. Module 9 provides the review and delivery approach for a change of this size.

## Build and run

From the repository root, enter `modules/10-credit-decision-capstone`. Use JDK 21 and the Maven Wrapper from Module 0.

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

Look for fourteen passing tests and `BUILD SUCCESS`. They cover creation, approval, denial, authorization, repeat decisions, rollback after a forced database constraint failure, and two competing decisions.

Before running the packaged service, set a course-only user ID and password and start the application:

```powershell
$env:COURSE_USER_ID = 'USER-COURSE-001'
$env:COURSE_PASSWORD = 'choose-a-temporary-training-password'
java -jar target/module-10-credit-decision-capstone.jar
```

The service listens on port 8080 on the local machine and stores only synthetic course data in an in-memory H2 database. Its training account has submitter, reader, and decider roles. Do not expose this HTTP Basic training service on a network. Use the `/csrf` endpoint to obtain a token and retain the session cookie for POST requests. Module 8 explains the credential and CSRF setup; the integration tests show the secured decision request.

To try an approval manually, open a second PowerShell terminal, set the same temporary credentials, and retain the session returned with the CSRF token. This sample submits and then approves one synthetic request:

```powershell
$env:COURSE_USER_ID = 'USER-COURSE-001'
$env:COURSE_PASSWORD = 'choose-a-temporary-training-password'
$csrf = Invoke-RestMethod -Uri 'http://localhost:8080/csrf' -SessionVariable courseSession
$basicValue = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$($env:COURSE_USER_ID):$($env:COURSE_PASSWORD)"))
$headers = @{ Authorization = "Basic $basicValue" }
$headers[$csrf.headerName] = $csrf.token
$submission = @{ requestId = 'REQ-CAPSTONE-001'; invoiceNumber = 'INV-CAPSTONE-001'; requestedAmount = 12.50 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/credit-requests' -Headers $headers -WebSession $courseSession -ContentType 'application/json' -Body $submission
$csrf = Invoke-RestMethod -Uri 'http://localhost:8080/csrf' -Headers @{ Authorization = "Basic $basicValue" } -WebSession $courseSession
$headers[$csrf.headerName] = $csrf.token
$decision = @{ decision = 'APPROVE'; reason = 'Training exercise' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/credit-requests/REQ-CAPSTONE-001/decision' -Headers $headers -WebSession $courseSession -ContentType 'application/json' -Body $decision
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/credit-requests/REQ-CAPSTONE-001' -Headers $headers -WebSession $courseSession
```

The final response shows `APPROVED`. The database is in memory, so the sample disappears when the server stops.

## Read the decision contract

| Request | Result |
| --- | --- |
| `POST /credit-requests/{requestId}/decision` with `APPROVE` or `DENY` | `200 OK`; the current request status and a decision record are updated together. |
| Decision request by an account without `ROLE_DECIDER` | `403 Forbidden`; no state or history change. |
| Decision for an unknown request | `404 Not Found`. |
| Second decision after the request has left `SUBMITTED` | `409 Conflict`; the first status and audit row remain. |
| Database failure during a decision | `503 Service Unavailable`; the status update and decision record roll back together. |

The request body has a decision action and a reason. The reason is fictional course data; a real policy owner defines which decision evidence must be retained and for how long.

## Trace a decision

Start at `CreditRequestController.decide` and follow the flow:

1. The security filter chain requires `ROLE_DECIDER`. Spring's CSRF protection also requires a token on this POST.
2. The controller takes the actor ID from `Authentication.getName()` and passes it with the request ID, action, and reason to `CreditDecisionService`.
3. Inside one transaction, the service updates the request only when its current status is `SUBMITTED`. If no row changes, it distinguishes an unknown request (`404`) from a request that already has a decision (`409`).
4. The service writes one decision record and one history event with the actor user ID and reason. The request row, decision row, and event commit together.
5. A unique request ID in `credit_decision` and the conditional status update prevent a second decision. If the history write fails after the status update, the transaction rolls back.

The current status is stored on `credit_request`; history preserves the initial submission and the final decision event. The acting user is stored as an ID in both decision and history data. No person name is stored or accepted from the decision body.

## Inspect the test groups

`CreditApiSecurityTest` verifies the external contract and role boundary:

- Submitter creates a request; a decider approves or denies it; the stored status, actor ID, reason, and history count match the response.
- A reader may inspect the request but cannot make a decision.
- A repeated decision receives `409`, and an unknown request receives `404`.

`CreditDecisionServiceTest` verifies the transaction and concurrency behavior:

- A blank actor ID triggers a database constraint after the status update; the test confirms the request remains `SUBMITTED` and no decision row remains.
- Two decision calls are released together. One commits, the other reports a conflict, and the database contains one decision with a matching status and history event.

The tests run against H2. They establish behavior for this lab and database configuration; validate transaction isolation, locking, and constraint behavior against the intended production database before relying on this pattern there.

## Capstone review

Before you call the change complete, explain:

- Which layer authenticates the caller, which layer authorizes the action, and which layer enforces the state transition.
- Why the state update is conditional and why a database transaction covers the status, decision, and history writes.
- How `404`, `409`, and `503` differ and what state each response leaves behind.
- Why the audit stores a user ID and how an operator would find the corresponding display identity through the approved identity system.
- Which decision rules, reason-retention requirements, authorization policy, and concurrency guarantees still need confirmation from domain, security, and platform owners in a real service.

## Completion criteria

- Show a permitted approval and denial, including their persisted status, actor ID, reason, and history event.
- Show a forbidden caller, a missing request, and a repeated decision, and verify that none changes the committed decision.
- Reproduce the forced history failure and show that the status and decision record roll back.
- Run the competing-decision test and explain the database condition that allows only one transition.
- Present the reviewed diff and build evidence using the delivery checklist from [Module 9](../09-reviewed-delivery/README.md).
