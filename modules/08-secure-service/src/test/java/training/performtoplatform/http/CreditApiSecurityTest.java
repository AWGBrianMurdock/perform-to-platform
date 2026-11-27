package training.performtoplatform.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "COURSE_USER_ID=USER-PROPERTIES-TEST", "COURSE_PASSWORD=unused-test-password" })
@AutoConfigureMockMvc
@Import(CreditApiSecurityTest.TestUsers.class)
class CreditApiSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("DELETE FROM credit_history");
        jdbcTemplate.update("DELETE FROM credit_request");
    }

    @Test
    void exposesOnlyTheBasicHealthStatusWithoutCredentials() throws Exception {
        String response = mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(response.contains("jdbc"));
        assertFalse(response.contains("components"));
    }

    @Test
    void rejectsAnonymousAccessToCreditRequests() throws Exception {
        mockMvc.perform(get("/credit-requests/REQ-UNKNOWN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void makesTheCsrfTokenAvailableToAClient() throws Exception {
        mockMvc.perform(get("/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void requiresCsrfProtectionForAnAuthenticatedWrite() throws Exception {
        mockMvc.perform(post("/credit-requests")
                        .with(httpBasic("USER-WRITER-001", "writer-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("REQ-CSRF", "INV-CSRF", "6.00")))
                .andExpect(status().isForbidden());

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
    }

    @Test
    void rejectsAnAnonymousWriteEvenWhenItHasACsrfToken() throws Exception {
        mockMvc.perform(post("/credit-requests")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("REQ-ANONYMOUS", "INV-ANONYMOUS", "6.00")))
                .andExpect(status().isUnauthorized());

        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
    }

    @Test
    void readerCanRetrieveARequestButCannotSubmitOne() throws Exception {
        submitAsWriter("REQ-READ-ONLY", "INV-READ-ONLY", "7.00");

        mockMvc.perform(get("/credit-requests/REQ-READ-ONLY")
                        .with(httpBasic("USER-READER-001", "reader-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(post("/credit-requests")
                        .with(httpBasic("USER-READER-001", "reader-test-password"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("REQ-DENIED", "INV-DENIED", "1.00")))
                .andExpect(status().isForbidden());

        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM credit_request", Integer.class));
    }

    @Test
    void storesTheAuthenticatedUserIdAndAllowsTheSubmitterToRead() throws Exception {
        submitAsWriter("REQ-AUTHORIZED", "INV-AUTHORIZED", "9.50");

        assertEquals("USER-WRITER-001", jdbcTemplate.queryForObject(
                "SELECT submitted_by_user_id FROM credit_history WHERE request_id = ?",
                String.class, "REQ-AUTHORIZED"));

        mockMvc.perform(get("/credit-requests/REQ-AUTHORIZED")
                        .with(httpBasic("USER-WRITER-001", "writer-test-password")))
                .andExpect(status().isOk());
    }

    private void submitAsWriter(String requestId, String invoiceNumber, String amount) throws Exception {
        mockMvc.perform(post("/credit-requests")
                        .with(httpBasic("USER-WRITER-001", "writer-test-password"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(requestId, invoiceNumber, amount)))
                .andExpect(status().isCreated());
    }

    private static String requestJson(String requestId, String invoiceNumber, String amount) {
        return """
                {"requestId":"%s","invoiceNumber":"%s","requestedAmount":%s}
                """.formatted(requestId, invoiceNumber, amount);
    }

    @TestConfiguration
    static class TestUsers {
        @Bean
        PasswordEncoder testPasswordEncoder() {
            return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }

        @Bean
        UserDetailsService testUserDetailsService(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(
                    User.withUsername("USER-WRITER-001")
                            .password(encoder.encode("writer-test-password"))
                            .roles("SUBMITTER", "READER")
                            .build(),
                    User.withUsername("USER-READER-001")
                            .password(encoder.encode("reader-test-password"))
                            .roles("READER")
                            .build());
        }
    }
}
