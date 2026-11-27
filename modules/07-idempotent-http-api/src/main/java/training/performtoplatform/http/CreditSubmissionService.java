package training.performtoplatform.http;

import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class CreditSubmissionService {
    private final CreditRequestValidator validator;
    private final CreditRequestRepository repository;
    private final CreditSubmissionWriter writer;

    CreditSubmissionService(CreditRequestValidator validator,
            CreditRequestRepository repository,
            CreditSubmissionWriter writer) {
        this.validator = validator;
        this.repository = repository;
        this.writer = writer;
    }

    public SubmissionResult submit(String idempotencyKey, CreditRequest request, String submittedByUserId) {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9-]{1,64}")) {
            throw new InvalidIdempotencyKeyException();
        }

        Optional<CreditRequest> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replayOrReject(existing.get(), request);
        }

        SubmissionOutcome outcome = validator.validate(request);
        if (outcome != SubmissionOutcome.ACCEPTED) {
            return new SubmissionResult(outcome, false);
        }

        try {
            writer.submit(idempotencyKey, request, submittedByUserId);
            return new SubmissionResult(SubmissionOutcome.ACCEPTED, false);
        } catch (DuplicateKeyException failure) {
            existing = repository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return replayOrReject(existing.get(), request);
            }
            throw failure;
        }
    }

    private static SubmissionResult replayOrReject(CreditRequest existing, CreditRequest requested) {
        boolean sameRequest = existing.requestId().equals(requested.requestId())
                && java.util.Objects.equals(existing.invoiceNumber(), requested.invoiceNumber())
                && existing.requestedAmount().compareTo(requested.requestedAmount()) == 0;
        if (!sameRequest) {
            throw new IdempotencyConflictException();
        }
        return new SubmissionResult(SubmissionOutcome.ACCEPTED, true);
    }

    public Optional<String> findStatus(String requestId) {
        return repository.findStatus(requestId);
    }
}
