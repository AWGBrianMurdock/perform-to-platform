package training.performtoplatform.spring;

public interface CreditRequestRepository {
    void insertRequest(CreditRequest request);

    void insertSubmissionEvent(String requestId, String submittedByUserId);
}
