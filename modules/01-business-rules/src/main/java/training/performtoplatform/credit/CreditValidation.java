package training.performtoplatform.credit;

public final class CreditValidation {
    private static final String VALID = "00";
    private static final String INVOICE_REQUIRED = "04";
    private static final String AMOUNT_NOT_POSITIVE = "08";

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Pass an invoice number and a credit amount in whole cents.");
        }
        long requestedCents = Long.parseLong(args[1]);
        System.out.println(validate(args[0], requestedCents));
    }

    static String validate(String invoiceNumber, long requestedCents) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            return INVOICE_REQUIRED;
        }
        if (requestedCents <= 0) {
            return AMOUNT_NOT_POSITIVE;
        }
        return VALID;
    }
}
