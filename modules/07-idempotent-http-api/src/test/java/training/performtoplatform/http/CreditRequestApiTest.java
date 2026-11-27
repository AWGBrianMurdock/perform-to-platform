package training.performtoplatform.http;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CreditRequestApiTest {
    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("DELETE FROM credit_history");
        jdbcTemplate.update("DELETE FROM idempotency_record");
        jdbcTemplate.update("DELETE FROM credit_request");
    }

    @Test
    void createsRequestAndReturnsItsStatus() {
        SubmitCreditRequest request = request("REQ-HTTP-001", "INV-1001", "12.505");

        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-001")
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/credit-requests/REQ-HTTP-001")
                .expectBody(SubmissionResponse.class)
                .isEqualTo(new SubmissionResponse("REQ-HTTP-001", SubmissionOutcome.ACCEPTED, false));

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
        assertEquals(new BigDecimal("12.51"), jdbcTemplate.queryForObject(
                "SELECT requested_amount FROM credit_request WHERE request_id = ?",
                BigDecimal.class, "REQ-HTTP-001"));
        assertEquals("USER-COURSE-001", jdbcTemplate.queryForObject(
                "SELECT submitted_by_user_id FROM credit_history WHERE request_id = ?",
                String.class, "REQ-HTTP-001"));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM idempotency_record", Integer.class));

        restTestClient.get().uri("/credit-requests/REQ-HTTP-001")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CreditStatusResponse.class)
                .isEqualTo(new CreditStatusResponse("REQ-HTTP-001", "SUBMITTED"));
    }

    @Test
    void returnsUnprocessableEntityForBusinessRejectionWithoutWritingRows() {
        restTestClient.post().uri("/credit-requests")
                .header("Idempotency-Key", "idem-rejected")
                .body(request("REQ-REJECTED", " ", "0.004"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody(SubmissionResponse.class)
                .isEqualTo(new SubmissionResponse("REQ-REJECTED", SubmissionOutcome.INVOICE_REQUIRED, false));

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_history", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM idempotency_record", Integer.class));
    }

    @Test
    void returnsBadRequestWhenRequestShapeIsInvalid() {
        restTestClient.post().uri("/credit-requests")
                .header("Idempotency-Key", "idem-invalid")
                .body(request("not an ID", "INV-1001", "12.50"))
                .exchange()
                .expectStatus().isBadRequest();

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
    }

    @Test
    void returnsBadRequestForMalformedJson() {
        restTestClient.post().uri("/credit-requests")
                .header("Idempotency-Key", "idem-json")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"requestId\":")
                .exchange()
                .expectStatus().isBadRequest();

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
    }

    @Test
    void returnsNotFoundForUnknownRequestId() {
        restTestClient.get().uri("/credit-requests/REQ-UNKNOWN")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returnsConflictWhenRequestIdAlreadyExists() {
        SubmitCreditRequest request = request("REQ-DUPLICATE", "INV-1002", "5.00");
        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-first")
                .body(request).exchange().expectStatus().isCreated();

        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-second").body(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_request WHERE request_id = ?", Integer.class, "REQ-DUPLICATE"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_history WHERE request_id = ?", Integer.class, "REQ-DUPLICATE"));
    }

    @Test
    void replaysTheOriginalResultForTheSameKeyAndPayload() {
        SubmitCreditRequest request = request("REQ-REPLAY", "INV-REPLAY", "8.00");
        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-replay")
                .body(request).exchange().expectStatus().isCreated();

        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-replay")
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(SubmissionResponse.class)
                .isEqualTo(new SubmissionResponse("REQ-REPLAY", SubmissionOutcome.ACCEPTED, true));

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_history", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM idempotency_record", Integer.class));
    }

    @Test
    void rejectsAKeyReusedForDifferentRequestData() {
        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-conflict")
                .body(request("REQ-ORIGINAL", "INV-ORIGINAL", "8.00"))
                .exchange().expectStatus().isCreated();

        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-conflict")
                .body(request("REQ-CHANGED", "INV-CHANGED", "9.00"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);

        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-conflict")
                .body(request("REQ-REJECTED", "INV-CHANGED", "0.00"))
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_history", Integer.class));
    }

    @Test
    void concurrentRequestsWithTheSameKeyCreateOnlyOneRequest() throws Exception {
        SubmitCreditRequest request = request("REQ-CONCURRENT", "INV-CONCURRENT", "4.00");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            Future<HttpResponse> first = executor.submit(() -> submitTogether(request, ready, start));
            Future<HttpResponse> second = executor.submit(() -> submitTogether(request, ready, start));
            assertEquals(true, ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            HttpResponse firstResponse = first.get(10, TimeUnit.SECONDS);
            HttpResponse secondResponse = second.get(10, TimeUnit.SECONDS);
            assertEquals(Set.of(200, 201), Set.of(firstResponse.status(), secondResponse.status()));
            assertEquals(1, Stream.of(firstResponse, secondResponse).filter(response -> response.body().replayed()).count());
        }

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_history", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM idempotency_record", Integer.class));
    }

    @Test
    void requiresAWellFormedIdempotencyKey() {
        restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "bad key")
                .body(request("REQ-BAD-KEY", "INV-BAD-KEY", "1.00"))
                .exchange().expectStatus().isBadRequest();

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
    }

    private HttpResponse submitTogether(
            SubmitCreditRequest request,
            CountDownLatch ready,
            CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent submission did not start.");
        }
        var result = restTestClient.post().uri("/credit-requests").header("Idempotency-Key", "idem-concurrent")
                .body(request).exchange().expectBody(SubmissionResponse.class).returnResult();
        return new HttpResponse(result.getStatus().value(), result.getResponseBody());
    }

    private record HttpResponse(int status, SubmissionResponse body) {
    }

    private static SubmitCreditRequest request(String id, String invoiceNumber, String amount) {
        return new SubmitCreditRequest(id, invoiceNumber, new BigDecimal(amount));
    }
}
