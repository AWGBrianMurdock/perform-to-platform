package training.performtoplatform.orientation;

public final class CreditPreview {
    public static void main(String[] args) {
        String invoiceId = args.length == 0 ? "INV-1001" : args[0];
        System.out.println(previewMessage(invoiceId));
    }

    static String previewMessage(String invoiceId) {
        return "Credit preview for invoice " + invoiceId;
    }
}
