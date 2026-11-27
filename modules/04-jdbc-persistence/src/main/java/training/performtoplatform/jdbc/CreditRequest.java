package training.performtoplatform.jdbc;

import java.math.BigDecimal;

record CreditRequest(String requestId, String invoiceNumber, BigDecimal requestedAmount) {
}
