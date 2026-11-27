package training.performtoplatform.composition;

import java.util.Optional;

interface CreditStatusRepository {
    Optional<String> findStatus(String requestId);
}
