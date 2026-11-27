package training.performtoplatform.spring;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCreditRequestRepository implements CreditRequestRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcCreditRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertRequest(CreditRequest request) {
        jdbcTemplate.update("""
                INSERT INTO credit_request (request_id, invoice_number, requested_amount)
                VALUES (?, ?, ?)
                """, request.requestId(), request.invoiceNumber(), request.requestedAmount());
    }

    @Override
    public void insertSubmissionEvent(String requestId, String submittedByUserId) {
        jdbcTemplate.update("""
                INSERT INTO credit_history (request_id, event_type, submitted_by_user_id)
                VALUES (?, 'SUBMITTED', ?)
                """, requestId, submittedByUserId);
    }
}
