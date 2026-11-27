package training.performtoplatform.http;

import java.util.Optional;

public interface CreditRequestRepository {
    void insertRequest(CreditRequest request);

    void insertSubmissionEvent(String requestId, String submittedByUserId);

    Optional<String> findStatus(String requestId);
}
