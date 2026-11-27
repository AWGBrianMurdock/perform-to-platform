package training.performtoplatform.http;

final class CreditDecisionConflictException extends RuntimeException {
    CreditDecisionConflictException(String status) {
        super("A decision cannot be recorded while the request status is " + status + ".");
    }
}
