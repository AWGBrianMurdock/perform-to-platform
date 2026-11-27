package training.performtoplatform.http;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditDecisionService {
    private final CreditRequestRepository repository;

    CreditDecisionService(CreditRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String decide(String requestId, String decision, String actorUserId, String reason) {
        String status = switch (decision) {
            case "APPROVE" -> "APPROVED";
            case "DENY" -> "DENIED";
            default -> throw new IllegalArgumentException("Unsupported decision action.");
        };

        if (repository.transitionSubmittedTo(requestId, status) != 1) {
            String currentStatus = repository.findCurrentStatus(requestId)
                    .orElseThrow(() -> new CreditRequestNotFoundException(requestId));
            throw new CreditDecisionConflictException(currentStatus);
        }

        repository.insertDecisionEvent(requestId, status, actorUserId, reason);
        return status;
    }
}
