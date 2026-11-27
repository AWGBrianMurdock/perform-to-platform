package training.performtoplatform.jdbc;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditRepositoryIntegrationTest {
    @TempDir
    Path databaseDirectory;

    private String jdbcUrl;
    private CreditRepository repository;

    @BeforeEach
    void createDatabase() throws SQLException {
        jdbcUrl = "jdbc:h2:file:" + databaseDirectory.resolve("credit-test");
        repository = new CreditRepository(jdbcUrl);
        repository.initializeSchema();
    }

    @Test
    void commitsRequestAndInitialHistory() throws SQLException {
        CreditRequest request = new CreditRequest("REQ-1001", "INV-O'NEIL", new BigDecimal("12.50"));

        repository.submit(request, "USER-TRAINEE-001");

        assertEquals(Optional.of(request), repository.findById(request.requestId()));
        assertEquals(List.of(new HistoryEntry("SUBMITTED", "USER-TRAINEE-001")), historyFor(request.requestId()));
    }

    @Test
    void rollsBackRequestWhenHistoryInsertFailsAndAllowsRetry() throws SQLException {
        CreditRequest request = new CreditRequest("REQ-ROLLBACK", "INV-1002", new BigDecimal("5.00"));

        assertHistoryInsertFails(request);

        assertAll("state after failed history insert",
                () -> assertEquals(Optional.empty(), repository.findById(request.requestId()), "request"),
                () -> assertEquals(List.of(), historyFor(request.requestId()), "history"));

        repository.submit(request, "USER-TRAINEE-001");

        assertEquals(Optional.of(request), repository.findById(request.requestId()));
        assertEquals(List.of(new HistoryEntry("SUBMITTED", "USER-TRAINEE-001")), historyFor(request.requestId()));
    }

    @Test
    void rollbackPreservesPreviouslyCommittedRequestAndHistory() throws SQLException {
        CreditRequest existing = new CreditRequest("REQ-EXISTING", "INV-1001", new BigDecimal("7.50"));
        CreditRequest rejected = new CreditRequest("REQ-REJECTED", "INV-1002", new BigDecimal("5.00"));
        repository.submit(existing, "USER-TRAINEE-002");

        assertHistoryInsertFails(rejected);

        assertAll("state after rolling back a different request",
                () -> assertEquals(Optional.of(existing), repository.findById(existing.requestId())),
                () -> assertEquals(List.of(new HistoryEntry("SUBMITTED", "USER-TRAINEE-002")),
                        historyFor(existing.requestId())),
                () -> assertEquals(Optional.empty(), repository.findById(rejected.requestId())),
                () -> assertEquals(List.of(), historyFor(rejected.requestId())));
    }

    private void assertHistoryInsertFails(CreditRequest request) {
        SQLException failure = assertThrows(SQLException.class, () -> repository.submit(request, " "));
        assertEquals("23513", failure.getSQLState());
        assertTrue(failure.getMessage().contains("NONBLANK_HISTORY_USER_ID"), failure.getMessage());
    }

    private List<HistoryEntry> historyFor(String requestId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT event_type, submitted_by_user_id
                        FROM credit_history
                        WHERE request_id = ?
                        """)) {
            statement.setString(1, requestId);
            try (ResultSet result = statement.executeQuery()) {
                List<HistoryEntry> entries = new ArrayList<>();
                while (result.next()) {
                    entries.add(new HistoryEntry(
                            result.getString("event_type"), result.getString("submitted_by_user_id")));
                }
                return entries;
            }
        }
    }

    private record HistoryEntry(String eventType, String submittedByUserId) {
    }
}
