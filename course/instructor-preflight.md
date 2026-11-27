# Prepare the workstation and observe the pilot

Use this guide before a learner session. Check the actual workstation, learner account, shell, and network configuration. A successful build in a maintainer's environment does not establish that the pilot works in a managed training environment.

## Check the pilot environment

1. Confirm the learner can open the pilot, access the matching course checkout, and write to the checkout directory.
2. In the learner's intended shell, check `java -version`, `javac -version`, `jdb -version`, and `git --version`. The Java tools should report major version 21.
3. From `modules/00-orientation`, run `.\mvnw.cmd -v` in PowerShell or Command Prompt. Confirm the wrapper starts and reports the intended JDK.
4. Use an approved learner profile with an empty Maven cache to check the first download path. Confirm the workstation can reach its approved Maven repository or mirror.
5. Run `clean verify` in Modules 0 and 1. Compare the test counts with the guides and check the documented JAR output.

If the workstation blocks a wrapper process, download, or debugger command, record the exact policy or error and resolve it with the workstation owner before the session. Do not ask learners to bypass managed-device controls.

## Check the current runnable labs

When preparing the full lesson sequence rather than the initial learner pilot, check each runnable lab that will be used. The HTML course includes all guides, while learners still need a matching checkout to run or change the projects. Use a disposable checkout for exercises that replace source files or create a database.

| Lab | Workstation check |
| --- | --- |
| [0. Orientation](../modules/00-orientation/README.md#run-the-jar) | Build and run the JAR, inspect its manifest, and hit the `jdb` breakpoint. |
| [1. Business rules](../modules/01-business-rules/README.md#run-the-jar) | Run the status examples, including the quoted blank invoice argument; complete the [amount-limit exercise](../modules/01-business-rules/README.md#add-a-maximum-request-rule). |
| [2. Domain modeling](../modules/02-domain-modeling/README.md) | Run amount and date boundary examples and observe the malformed-date error. |
| [3. Build, debug, and review](../modules/03-build-debug-review/README.md) | Copy the reported source, write and run the regression test, and inspect `comparison = 0` in `jdb`. |
| [4. SQL and JDBC](../modules/04-jdbc-persistence/README.md) | Initialize the H2 database, submit and retrieve a request, then verify the forced rollback and retry. |
| [4A. Plain Java](../modules/04a-plain-java/README.md) | Run both status lookups and substitute the repository implementation in a unit test. |
| [5. Spring Boot workflow](../modules/05-spring-boot-workflow/README.md) | Build and run the application, then confirm the Spring-managed transaction test rolls back both rows after the history insert fails. |
| [6. HTTP API](../modules/06-credit-http-api/README.md) | Build and run the service, submit and retrieve a synthetic request, and confirm the integration tests cover response codes and database effects. |
| [7. Idempotent HTTP API](../modules/07-idempotent-http-api/README.md) | Confirm same-key replays return the original outcome and the concurrent duplicate test leaves one request, event, and key record. |
| [8. Secure service](../modules/08-secure-service/README.md) | Configure a temporary training credential, check the public health response, and confirm the security tests distinguish authentication, authorization, and CSRF outcomes. |
| [9. Reviewed delivery](../modules/09-reviewed-delivery/README.md) | Rehearse the test-first amount-cap change on a disposable branch and review the workflow result for that revision. |
| [10. Decision capstone](../modules/10-credit-decision-capstone/README.md) | Verify approval, denial, forbidden and repeated decisions, rollback after an audit failure, and one winner under competing decisions. |

Record the checkout revision, workstation image, shell, Java and Maven versions, results for each lab, and any setup steps learners need. Do not record learner credentials or production data.

## Observe a learner pilot

Invite a small group of developers with production COBOL experience and different levels of Java tooling experience. Give them the pilot and its matching checkout. Ask them to work through Modules 0 and 1 independently first; intervene when they are stuck, and note which instruction or recovery step they tried.

Capture observations without unnecessary personal details:

| Observation | What to record |
| --- | --- |
| First successful JAR run | Elapsed time, with tool setup and downloads noted separately. |
| Points of hesitation | The step, explanation, glossary entry, or hint involved; note any instructor help. |
| Evidence of understanding | Whether the learner traces the invoice argument and implements the new rule with boundary and precedence tests. |
| Pacing and confidence | Actual time per lab and whether the learner feels ready to continue. |

After each lab, ask which parts were new because of Java and which were difficult because of the instructions. Use the answers to revise explanations and estimates. Treat estimates as provisional until they reflect observed sessions.
