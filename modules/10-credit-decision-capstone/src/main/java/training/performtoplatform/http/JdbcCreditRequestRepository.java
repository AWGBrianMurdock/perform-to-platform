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
                INSERT INTO credit_history (request_id, event_type, actor_user_id)
                VALUES (?, 'SUBMITTED', ?)
                """, requestId, submittedByUserId);
    }

    @Override
    public int transitionSubmittedTo(String requestId, String decision) {
        return jdbcTemplate.update("""
                UPDATE credit_request
                SET status = ?
                WHERE request_id = ? AND status = 'SUBMITTED'
                """, decision, requestId);
    }

    @Override
    public void insertDecisionEvent(String requestId, String decision, String actorUserId, String reason) {
        jdbcTemplate.update("""
                INSERT INTO credit_history (request_id, event_type, actor_user_id, event_reason)
                VALUES (?, ?, ?, ?)
                """, requestId, decision, actorUserId, reason);
        jdbcTemplate.update("""
                INSERT INTO credit_decision (request_id, decision, decided_by_user_id, reason)
                VALUES (?, ?, ?, ?)
                """, requestId, decision, actorUserId, reason);
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

    @Override
    public Optional<String> findCurrentStatus(String requestId) {
        List<String> statuses = jdbcTemplate.query("""
                SELECT status FROM credit_request WHERE request_id = ?
                """, (result, rowNumber) -> result.getString("status"), requestId);
        return statuses.stream().findFirst();
    }
}
