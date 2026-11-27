# Module 8 — Secure and diagnose the service

The service now accepts real HTTP requests and retries. Which requests may a caller make? Where should the submitted-by user ID come from? What can a health endpoint tell an operator without exposing database details?

**Learning objective** — Trace authentication, role checks, CSRF protection, user-ID recording, and the safe health response; distinguish what each control proves.

**Prerequisites** — Complete Modules 0–7. This standalone project retains the HTTP workflow and adds Spring Security and Actuator.

## Configure and run

From the repository root, enter `modules/08-secure-service`. Use JDK 21 and the Maven Wrapper from Module 0.

| Shell | Command |
| --- | --- |
| macOS or Linux | `../00-orientation/mvnw clean verify` |
| Windows PowerShell or Command Prompt | `..\00-orientation\mvnw.cmd clean verify` |

Look for seven passing security tests and `BUILD SUCCESS`. They exercise the Spring Security filter chain, CSRF checks, role decisions, the controller, the database, and the health endpoint with MockMvc.

Before running the packaged service, configure a course-only user ID and password in the process environment. Do not reuse a production credential.

```powershell
$env:COURSE_USER_ID = 'USER-COURSE-001'
$env:COURSE_PASSWORD = 'choose-a-temporary-training-password'
java -jar target/module-8-secure-service.jar
```

The application listens on port 8080 on the local machine and uses an in-memory H2 database. The user ID and password are externalized through environment variables. The lab uses Boot's in-memory user configuration for one local training account; it is not an identity provider or a production account store. Do not expose this HTTP Basic training service on a network.

In another terminal, check the public health response:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health'
```

It reports a basic status such as `UP`. The endpoint is public, so the configuration exposes no component details. The `GET /csrf` endpoint returns a CSRF token for a client that needs to make an unsafe request; keep the session cookie and send the token in the returned header. After a successful POST, fetch a fresh token before the next POST. The automated tests demonstrate the protected POST using Spring Security's CSRF test support.

To try an authenticated request manually, open a second PowerShell terminal and set the same temporary course credentials there. Each terminal has its own environment. Fetch the token into a web session, then send both the session cookie and token with the POST:

```powershell
$env:COURSE_USER_ID = 'USER-COURSE-001'
$env:COURSE_PASSWORD = 'choose-a-temporary-training-password'
$csrf = Invoke-RestMethod -Uri 'http://localhost:8080/csrf' -SessionVariable courseSession
$basicValue = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$($env:COURSE_USER_ID):$($env:COURSE_PASSWORD)"))
$headers = @{ Authorization = "Basic $basicValue" }
$headers[$csrf.headerName] = $csrf.token
$body = @{ requestId = 'REQ-SECURE-001'; invoiceNumber = 'INV-SECURE-001'; requestedAmount = 12.50 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/credit-requests' -Headers $headers -WebSession $courseSession -ContentType 'application/json' -Body $body
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/credit-requests/REQ-SECURE-001' -Headers $headers -WebSession $courseSession
```

The first response shows an accepted submission; the second shows its `SUBMITTED` status. Use only this temporary identity and synthetic request. Stop the server with `Ctrl+C` when finished.

## Trace the security rules

Open `SecurityConfiguration` and follow the authorization rules in order:

| Request | Required authority | Expected result without the authority |
| --- | --- | --- |
| `GET /actuator/health` | None | Public basic status only. |
| `POST /credit-requests` | `ROLE_SUBMITTER` and a valid CSRF token | `401 Unauthorized` for an unauthenticated request with a valid CSRF token; `403 Forbidden` for a reader or for a request without a valid CSRF token. |
| `GET /credit-requests/{requestId}` | `ROLE_READER` | `401 Unauthorized` when unauthenticated; `403 Forbidden` for a submitter without the reader role. |
| Any other route | No access | Denied. |

Authentication proves which configured account made the request. Authorization checks what that account may do. CSRF protection is still enabled because Basic credentials can be sent automatically by a browser; the API does not disable it just to simplify a command-line example.

The controller receives Spring Security's `Authentication` and passes `authentication.getName()` to the service. In the course configuration, the account name is a stable user ID (`COURSE_USER_ID`), never a person's display name. In a production service, integrate with the approved identity provider and map its authenticated subject to the application's user ID. Do not accept the acting user ID from the JSON body or a caller-controlled header.

## Read the evidence

Open `CreditApiSecurityTest` and group the cases:

- Anonymous health access returns `UP` without component or JDBC details.
- Anonymous credit-request access returns `401`.
- A Basic-authenticated write without CSRF protection returns `403` and inserts no request.
- The reader role can retrieve a request but cannot submit one.
- The submitter can create a request, its authenticated user ID is persisted, and its reader role permits status lookup.

The test user records and passwords exist only in a test configuration. They do not configure the packaged application. A passing security test proves the configured rule in this lab; it does not establish that production identity claims, transport encryption, account lifecycle, secret delivery, or deployment policy are correct.

The public health response reports application health, not whether a particular business request is correct. If a request fails, use its HTTP status, a correlation or request ID from the deployment's logging setup, and the protected server-side logs to follow that request. Keep credentials, tokens, and database exception details out of client responses and ordinary logs.

## Extend the exercise

In a disposable copy, change the submitter account to a reader-only account and rerun the authorized POST test. Predict the status and table state before running the test. Restore the role and verify the successful request again.

Then inspect `application.properties`. Identify which settings are safe to publish and which values arrive from the environment. Write a short runbook entry for an operator who sees a request-level `503` while `/actuator/health` reports `UP`: name the next evidence to inspect, where it should be obtained, and which sensitive values must remain out of the response.

## Completion criteria

- Explain the difference between authentication, authorization, and CSRF protection using the test cases.
- Show an anonymous `401`, an authenticated-but-forbidden `403`, and an allowed request.
- Verify that the stored actor is the authenticated user ID and point to the line where it is obtained.
- Explain what the public health status tells an operator and why its details are hidden.
- Describe how the account credential is externalized and identify the identity-provider and deployment decisions that remain outside this training configuration.

Next, prepare a reviewed, reproducible change in [Module 9](../09-reviewed-delivery/README.md).
