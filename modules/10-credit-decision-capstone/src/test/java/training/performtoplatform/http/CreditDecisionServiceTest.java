package training.performtoplatform.http;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = { "COURSE_USER_ID=USER-PROPERTIES-TEST", "COURSE_PASSWORD=unused-test-password" })
@Import(CreditApiSecurityTest.TestUsers.class)
class CreditDecisionServiceTest {
    @Autowired
    private CreditSubmissionService submissionService;

    @Autowired
    private CreditDecisionService decisionService;

    @Autowired
    private CreditRequestRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("DELETE FROM credit_history");
        jdbcTemplate.update("DELETE FROM credit_decision");
        jdbcTemplate.update("DELETE FROM credit_request");
    }

    @Test
    void rollsBackTheStatusChangeWhenAuditInsertionFails() {
        CreditRequest request = request("REQ-ROLLBACK");
        submissionService.submit(request, "USER-SUBMITTER-001");

        assertThrows(DataAccessException.class, () -> decisionService.decide(
                request.requestId(), "APPROVE", "", "A decision reason."));

        assertEquals("SUBMITTED", repository.findCurrentStatus(request.requestId()).orElseThrow());
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_decision", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_history WHERE request_id = ?", Integer.class, request.requestId()));
    }

    @Test
    void competingDecisionsCommitOnlyOneStatusAndAuditEvent() throws Exception {
        CreditRequest request = request("REQ-COMPETING");
        submissionService.submit(request, "USER-SUBMITTER-001");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var approval = executor.submit(() -> decideTogether(
                    request.requestId(), "APPROVE", "USER-DECIDER-A", ready, start));
            var denial = executor.submit(() -> decideTogether(
                    request.requestId(), "DENY", "USER-DECIDER-B", ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            String approvalResult = approval.get(10, TimeUnit.SECONDS);
            String denialResult = denial.get(10, TimeUnit.SECONDS);
            assertEquals(1, Stream.of(approvalResult, denialResult).filter("CONFLICT"::equals).count());

            String winningStatus = Stream.of(approvalResult, denialResult)
                    .filter(status -> !status.equals("CONFLICT"))
                    .findFirst().orElseThrow();
            assertTrue(Set.of("APPROVED", "DENIED").contains(winningStatus));
            assertEquals(winningStatus, repository.findCurrentStatus(request.requestId()).orElseThrow());
        }

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_decision WHERE request_id = ?", Integer.class, request.requestId()));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_history WHERE request_id = ?", Integer.class, request.requestId()));
    }

    private String decideTogether(
            String requestId,
            String decision,
            String userId,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Competing decisions did not start.");
        }
        try {
            return decisionService.decide(requestId, decision, userId, "Concurrent decision.");
        } catch (CreditDecisionConflictException conflict) {
            return "CONFLICT";
        }
    }

    private static CreditRequest request(String requestId) {
        return new CreditRequest(requestId, "INV-" + requestId, new BigDecimal("20.00"));
    }
}
