package training.performtoplatform.http;

import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<String> findStatus(String requestId) {
        List<String> statuses = jdbcTemplate.query("""
                SELECT event_type
                FROM credit_history
                WHERE request_id = ?
                ORDER BY history_id DESC
                FETCH FIRST 1 ROW ONLY
                """, (result, rowNumber) -> result.getString("event_type"), requestId);
        return statuses.stream().findFirst();
    }
}
