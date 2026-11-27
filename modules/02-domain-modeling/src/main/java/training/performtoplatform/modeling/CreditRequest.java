package training.performtoplatform.modeling;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

record CreditRequest(String invoiceNumber, BigDecimal requestedAmount, LocalDate invoiceDate) {
    CreditRequest {
        requestedAmount = Objects.requireNonNull(requestedAmount, "requestedAmount")
                .setScale(2, RoundingMode.HALF_UP);
        Objects.requireNonNull(invoiceDate, "invoiceDate");
    }
}
