package training.performtoplatform.http;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(CreditRequestApiFailureTest.FailingRepositoryConfiguration.class)
class CreditRequestApiFailureTest {
    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsServiceUnavailableWithoutLeakingDatabaseDetails() {
        restTestClient.post().uri("/credit-requests")
                .body(new SubmitCreditRequest("REQ-DB-FAIL", "INV-1003", new BigDecimal("5.00")))
                .exchange()
                .expectStatus().isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class)
                .value(body -> org.junit.jupiter.api.Assertions.assertFalse(body.contains("internal database detail")));

        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_history", Integer.class));
    }

    @TestConfiguration
    static class FailingRepositoryConfiguration {
        @Bean
        @Primary
        CreditRequestRepository failingCreditRequestRepository(JdbcTemplate jdbcTemplate) {
            return new CreditRequestRepository() {
                @Override
                public void insertRequest(CreditRequest request) {
                    jdbcTemplate.update("""
                            INSERT INTO credit_request (request_id, invoice_number, requested_amount)
                            VALUES (?, ?, ?)
                            """, request.requestId(), request.invoiceNumber(), request.requestedAmount());
                }

                @Override
                public void insertSubmissionEvent(String requestId, String submittedByUserId) {
                    throw new DataAccessResourceFailureException("internal database detail");
                }

                @Override
                public Optional<String> findStatus(String requestId) {
                    return Optional.empty();
                }
            };
        }
    }
}
