package training.performtoplatform.http;

import java.util.Optional;

public interface CreditRequestRepository {
    void insertRequest(CreditRequest request);

    void insertSubmissionEvent(String requestId, String submittedByUserId);

    void insertIdempotencyRecord(String idempotencyKey, String requestId);

    Optional<CreditRequest> findByIdempotencyKey(String idempotencyKey);

    Optional<String> findStatus(String requestId);
}
