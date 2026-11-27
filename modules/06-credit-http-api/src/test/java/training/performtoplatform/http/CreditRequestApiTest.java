package training.performtoplatform.http;

import java.math.BigDecimal;

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
        jdbcTemplate.update("DELETE FROM credit_request");
    }

    @Test
    void createsRequestAndReturnsItsStatus() {
        SubmitCreditRequest request = request("REQ-HTTP-001", "INV-1001", "12.505");

        restTestClient.post().uri("/credit-requests")
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/credit-requests/REQ-HTTP-001")
                .expectBody(SubmissionResponse.class)
                .isEqualTo(new SubmissionResponse("REQ-HTTP-001", SubmissionOutcome.ACCEPTED));

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
        assertEquals(new BigDecimal("12.51"), jdbcTemplate.queryForObject(
                "SELECT requested_amount FROM credit_request WHERE request_id = ?",
                BigDecimal.class, "REQ-HTTP-001"));
        assertEquals("USER-COURSE-001", jdbcTemplate.queryForObject(
                "SELECT submitted_by_user_id FROM credit_history WHERE request_id = ?",
                String.class, "REQ-HTTP-001"));

        restTestClient.get().uri("/credit-requests/REQ-HTTP-001")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CreditStatusResponse.class)
                .isEqualTo(new CreditStatusResponse("REQ-HTTP-001", "SUBMITTED"));
    }

    @Test
    void returnsUnprocessableEntityForBusinessRejectionWithoutWritingRows() {
        restTestClient.post().uri("/credit-requests")
                .body(request("REQ-REJECTED", " ", "0.004"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody(SubmissionResponse.class)
                .isEqualTo(new SubmissionResponse("REQ-REJECTED", SubmissionOutcome.INVOICE_REQUIRED));

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_history", Integer.class));
    }

    @Test
    void returnsBadRequestWhenRequestShapeIsInvalid() {
        restTestClient.post().uri("/credit-requests")
                .body(request("not an ID", "INV-1001", "12.50"))
                .exchange()
                .expectStatus().isBadRequest();

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
    }

    @Test
    void returnsBadRequestForMalformedJson() {
        restTestClient.post().uri("/credit-requests")
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
        restTestClient.post().uri("/credit-requests").body(request).exchange().expectStatus().isCreated();

        restTestClient.post().uri("/credit-requests").body(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_request WHERE request_id = ?", Integer.class, "REQ-DUPLICATE"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_history WHERE request_id = ?", Integer.class, "REQ-DUPLICATE"));
    }

    private static SubmitCreditRequest request(String id, String invoiceNumber, String amount) {
        return new SubmitCreditRequest(id, invoiceNumber, new BigDecimal(amount));
    }
}
