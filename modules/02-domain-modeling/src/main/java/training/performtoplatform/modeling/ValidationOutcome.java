package training.performtoplatform.modeling;

enum ValidationOutcome {
    VALID,
    INVOICE_REQUIRED,
    AMOUNT_NOT_POSITIVE,
    INVOICE_BEFORE_CUTOFF
}
