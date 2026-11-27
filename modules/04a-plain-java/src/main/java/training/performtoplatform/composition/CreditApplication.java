package training.performtoplatform.composition;

import java.util.Map;

public final class CreditApplication {
    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Pass one request ID.");
        }

        CreditStatusRepository repository = new InMemoryCreditStatusRepository(Map.of("REQ-1001", "SUBMITTED"));
        CreditStatusService service = new CreditStatusService(repository);
        System.out.println(service.lookupStatus(args[0]));
    }
}
