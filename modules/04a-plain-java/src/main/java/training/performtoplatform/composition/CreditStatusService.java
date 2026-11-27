package training.performtoplatform.composition;

final class CreditStatusService {
    private final CreditStatusRepository repository;

    CreditStatusService(CreditStatusRepository repository) {
        this.repository = repository;
    }

    String lookupStatus(String requestId) {
        return repository.findStatus(requestId).orElse("NOT_FOUND");
    }
}
