package training.performtoplatform.spring;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditSubmissionService {
    private final CreditRequestValidator validator;
    private final CreditRequestRepository repository;

    CreditSubmissionService(CreditRequestValidator validator, CreditRequestRepository repository) {
        this.validator = validator;
        this.repository = repository;
    }

    @Transactional
    public SubmissionOutcome submit(CreditRequest request, String submittedByUserId) {
        SubmissionOutcome outcome = validator.validate(request);
        if (outcome != SubmissionOutcome.ACCEPTED) {
            return outcome;
        }

        repository.insertRequest(request);
        repository.insertSubmissionEvent(request.requestId(), submittedByUserId);
        return SubmissionOutcome.ACCEPTED;
    }
}
