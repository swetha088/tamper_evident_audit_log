package com.schwab.interview.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schwab.interview.audit.dto.AuditDtos.AppendEventRequest;
import com.schwab.interview.audit.dto.AuditDtos.RedactionRequest;
import com.schwab.interview.audit.facade.AuditLogFacade;
import com.schwab.interview.audit.repository.AuditEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:audit-test;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class AuditLogFacadeTest {
    @Autowired
    AuditLogFacade facade;

    @Autowired
    AuditEventRepository repository;

    @Autowired
    ObjectMapper json;

    @Autowired
    MockMvc mockMvc;

    @Test
    void detectsTampering() throws Exception {
        var event = facade.append(new AppendEventRequest("USER_LOGIN", "alice", "ACCOUNT", "1",
                json.readTree("{\"outcome\":\"SUCCESS\"}"), null, null));

        var stored = repository.findByEventId(event.eventId()).orElseThrow();
        var field = stored.getClass().getDeclaredField("actorId");
        field.setAccessible(true);
        field.set(stored, "mallory");
        repository.flush();

        assertThat(facade.verify().firstViolation().type()).isEqualTo("CONTENT_HASH_MISMATCH");
    }

    @Test
    void redactionPreservesChain() throws Exception {
        var event = facade.append(new AppendEventRequest("READ", "alice", "ACCOUNT", "1",
                json.readTree("{\"client\":{\"ssn\":\"123\"}}"), null, List.of("client.ssn")));

        facade.redact(event.eventId(), new RedactionRequest(List.of("client.ssn"), "privacy", "request"));

        assertThat(facade.verify().intact()).isTrue();
    }

    @Test
    void exportContainsSelectedRecordsAndFullChainWitness() throws Exception {
        facade.append(new AppendEventRequest("READ", "alice", "ACCOUNT", "one",
                json.readTree("{\"outcome\":\"ALLOW\"}"), null, null));
        facade.append(new AppendEventRequest("UPDATE", "bob", "ACCOUNT", "two",
                json.readTree("{\"outcome\":\"ALLOW\"}"), null, null));
        facade.append(new AppendEventRequest("READ", "alice", "ACCOUNT", "one",
                json.readTree("{\"outcome\":\"ALLOW\"}"), null, null));

        var bundle = facade.export(null, "one", null, null);

        assertThat(bundle.records()).hasSize(2);
        assertThat(bundle.records()).allSatisfy(record -> assertThat(record.resourceId()).isEqualTo("one"));
        assertThat(bundle.chainWitnesses()).hasSize(3);
        assertThat(bundle.verification().intact()).isTrue();
        assertThat(bundle.records().getFirst().previousHash()).isEqualTo("0".repeat(64));
    }

    @Test
    void exportRequiresExactlyOneSelector() {
        assertThatIllegalArgumentException().isThrownBy(() -> facade.export(null, null, null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> facade.export("alice", "one", null, null));
    }

    @Test
    void clientAccountAccessReportIncludesOnlyDocumentedAccessEvents() throws Exception {
        facade.append(new AppendEventRequest("CLIENT_DATA_READ", "advisor-7", "CLIENT_ACCOUNT", "acct-42",
                json.readTree("{\"authorizationOutcome\":\"ALLOW\"}"), null, null));
        facade.append(new AppendEventRequest("CLIENT_ACCOUNT_UPDATED", "advisor-7", "CLIENT_ACCOUNT", "acct-42",
                json.readTree("{\"authorizationOutcome\":\"ALLOW\"}"), null, null));
        facade.append(new AppendEventRequest("CLIENT_DATA_READ", "advisor-8", "CLIENT_ACCOUNT", "acct-43",
                json.readTree("{\"authorizationOutcome\":\"ALLOW\"}"), null, null));

        var report = facade.clientAccountAccessReport("acct-42", null, null);

        assertThat(report.events()).hasSize(1);
        assertThat(report.events().getFirst().eventType()).isEqualTo("CLIENT_DATA_READ");
        assertThat(report.events().getFirst().resourceId()).isEqualTo("acct-42");
        assertThat(report.verification().intact()).isTrue();
    }

    @Test
    void clientAccountAccessReportRequiresAnAccountId() {
        assertThatIllegalArgumentException().isThrownBy(() -> facade.clientAccountAccessReport(" ", null, null));
    }

    @Test
    void detectsTailDeletionFromPersistedChainHead() throws Exception {
        facade.append(new AppendEventRequest("READ", "alice", "ACCOUNT", "one",
                json.readTree("{\"outcome\":\"ALLOW\"}"), null, null));
        var tail = facade.append(new AppendEventRequest("READ", "alice", "ACCOUNT", "two",
                json.readTree("{\"outcome\":\"ALLOW\"}"), null, null));

        repository.deleteById(tail.sequence());
        repository.flush();

        assertThat(facade.verify().firstViolation().type()).isEqualTo("CHAIN_HEAD_MISMATCH");
    }

    @Test
    void archiveIncludesRedactedRecordsWithoutBreakingVerification() throws Exception {
        var event = facade.append(new AppendEventRequest("READ", "alice", "ACCOUNT", "one",
                json.readTree("{\"client\":{\"ssn\":\"123\"}}"), null, List.of("client.ssn")));
        facade.redact(event.eventId(), new RedactionRequest(List.of("client.ssn"), "privacy", "request"));

        var result = facade.archive(java.time.Instant.now().plusSeconds(1), "privacy-retention");

        assertThat(result.get("archived")).isEqualTo(1);
        assertThat(facade.query(null, null, null, null, null, null, null, 10).items().getFirst().status())
                .isEqualTo(com.schwab.interview.audit.entity.AuditEventEntity.Status.ARCHIVED);
        assertThat(facade.verify().intact()).isTrue();
    }

    @Test
    void localHttpApiAcceptsAppendAndVerificationWithoutCredentials() throws Exception {
        mockMvc.perform(post("/audit/events")
                        .contentType("application/json")
                        .content("""
                                {"eventType":"USER_LOGIN","actorId":"alice","resourceType":"ACCOUNT",
                                "resourceId":"one","payload":{"outcome":"SUCCESS"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("USER_LOGIN"));

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true));
    }

    @Test
    void exportHttpApiRejectsMissingSelector() throws Exception {
        mockMvc.perform(get("/audit/export"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
