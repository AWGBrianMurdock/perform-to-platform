package training.performtoplatform.http;

import java.util.Optional;

public interface CreditRequestRepository {
    void insertRequest(CreditRequest request);

    void insertSubmissionEvent(String requestId, String submittedByUserId);

    int transitionSubmittedTo(String requestId, String decision);

    void insertDecisionEvent(String requestId, String decision, String actorUserId, String reason);

    Optional<String> findCurrentStatus(String requestId);

    Optional<String> findStatus(String requestId);
}
