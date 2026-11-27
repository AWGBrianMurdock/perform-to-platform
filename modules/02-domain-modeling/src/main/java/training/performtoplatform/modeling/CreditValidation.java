package training.performtoplatform.modeling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class CreditValidation {
    public static void main(String[] args) {
        if (args.length != 4) {
            throw new IllegalArgumentException("Pass invoice number, decimal amount, invoice date, and cutoff date.");
        }
        CreditRequest request = new CreditRequest(args[0], new BigDecimal(args[1]), LocalDate.parse(args[2]));
        LocalDate cutoffDate = LocalDate.parse(args[3]);
        System.out.println(validate(request, cutoffDate) + " " + request.requestedAmount().toPlainString());
    }

    static ValidationOutcome validate(CreditRequest request, LocalDate cutoffDate) {
        Objects.requireNonNull(cutoffDate, "cutoffDate");
        if (request.invoiceNumber() == null || request.invoiceNumber().isBlank()) {
            return ValidationOutcome.INVOICE_REQUIRED;
        }
        if (request.requestedAmount().signum() <= 0) {
            return ValidationOutcome.AMOUNT_NOT_POSITIVE;
        }
        if (request.invoiceDate().isBefore(cutoffDate)) {
            return ValidationOutcome.INVOICE_BEFORE_CUTOFF;
        }
        return ValidationOutcome.VALID;
    }
}
