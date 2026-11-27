package training.performtoplatform.spring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record CreditRequest(String requestId, String invoiceNumber, BigDecimal requestedAmount) {
    public CreditRequest {
        requestedAmount = Objects.requireNonNull(requestedAmount, "requestedAmount")
                .setScale(2, RoundingMode.HALF_UP);
    }
}
