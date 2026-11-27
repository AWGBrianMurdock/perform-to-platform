package training.performtoplatform.debugging;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CreditLimitCheck {
    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Pass the requested amount and available credit in decimal units.");
        }

        BigDecimal requestedAmount = new BigDecimal(args[0]).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal availableCredit = new BigDecimal(args[1]).setScale(2, RoundingMode.UNNECESSARY);
        if (requestedAmount.signum() <= 0 || availableCredit.signum() < 0) {
            throw new IllegalArgumentException("The requested amount must be positive and available credit must be nonnegative.");
        }

        System.out.println(CreditLimit.allows(requestedAmount, availableCredit) ? "WITHIN_LIMIT" : "EXCEEDS_LIMIT");
    }
}
