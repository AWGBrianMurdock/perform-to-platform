package training.performtoplatform.debugging;

import java.math.BigDecimal;

final class CreditLimit {
    static boolean allows(BigDecimal requestedAmount, BigDecimal availableCredit) {
        int comparison = requestedAmount.compareTo(availableCredit);
        return comparison <= 0;
    }
}
