package com.arpanbom.ledger.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arpanbom.ledger.auth.DemoUsers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("testcontainers")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PostgresLedgerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void depositAndTransferThroughHttp() throws Exception {
        String token = login();
        String customerId = createCustomer(token);
        String fromId = openAccount(token, customerId);
        String toId = openAccount(token, customerId);

        MvcResult deposit = mvc.perform(post("/api/v1/accounts/" + fromId + "/deposits")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", "dep-" + UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("{\"amount\":\"80.00\",\"description\":\"Opening\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode depositBody = json.readTree(deposit.getResponse().getContentAsString());
        assertThat(depositBody.get("lines")).hasSize(2);

        MvcResult transfer = mvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", "trn-" + UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"fromAccountId":"%s","toAccountId":"%s","amount":"30.00","description":"Rent"}
                                """.formatted(fromId, toId)))
                .andExpect(status().isCreated())
                .andReturn();
        String transferId = json.readTree(transfer.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(post("/api/v1/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", json.readTree(transfer.getResponse().getContentAsString())
                                .get("idempotencyKey").asText())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"fromAccountId":"%s","toAccountId":"%s","amount":"30.00","description":"Rent"}
                                """.formatted(fromId, toId)))
                .andExpect(status().isOk());

        JsonNode from = account(token, fromId);
        JsonNode to = account(token, toId);
        assertThat(new BigDecimal(from.get("balance").asText())).isEqualByComparingTo("50.00");
        assertThat(new BigDecimal(to.get("balance").asText())).isEqualByComparingTo("30.00");

        mvc.perform(get("/api/v1/transfers/" + transferId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String login() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(DemoUsers.USERNAME, DemoUsers.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createCustomer(String token) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Grace Hopper","email":"grace-%s@example.com"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String openAccount(String token, String customerId) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","currency":"USD"}
                                """.formatted(customerId)))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private JsonNode account(String token, String accountId) throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/accounts/" + accountId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
}
