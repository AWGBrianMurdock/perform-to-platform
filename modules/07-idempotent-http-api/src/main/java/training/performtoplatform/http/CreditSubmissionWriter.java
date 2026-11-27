package training.performtoplatform.http;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditSubmissionWriter {
    private final CreditRequestRepository repository;

    CreditSubmissionWriter(CreditRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void submit(String idempotencyKey, CreditRequest request, String submittedByUserId) {
        repository.insertRequest(request);
        repository.insertSubmissionEvent(request.requestId(), submittedByUserId);
        repository.insertIdempotencyRecord(idempotencyKey, request.requestId());
    }
}
