package training.performtoplatform.spring;

import org.springframework.stereotype.Component;

@Component
final class CreditRequestValidator {
    SubmissionOutcome validate(CreditRequest request) {
        if (request.invoiceNumber() == null || request.invoiceNumber().isBlank()) {
            return SubmissionOutcome.INVOICE_REQUIRED;
        }
        if (request.requestedAmount().signum() <= 0) {
            return SubmissionOutcome.AMOUNT_NOT_POSITIVE;
        }
        return SubmissionOutcome.ACCEPTED;
    }
}
