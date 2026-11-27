package training.performtoplatform.composition;

import java.util.Map;
import java.util.Optional;

final class InMemoryCreditStatusRepository implements CreditStatusRepository {
    private final Map<String, String> statuses;

    InMemoryCreditStatusRepository(Map<String, String> statuses) {
        this.statuses = Map.copyOf(statuses);
    }

    @Override
    public Optional<String> findStatus(String requestId) {
        return Optional.ofNullable(statuses.get(requestId));
    }
}
