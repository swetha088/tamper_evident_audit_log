package com.schwab.interview.audit;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static com.schwab.interview.audit.ApiModels.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:audit-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@Transactional
class AuditServiceTest {
    @Autowired AuditService service;
    @Autowired AuditEventRepository repository;
    @Autowired ObjectMapper mapper;

    @Test
    void detectsDirectDatabaseModification() throws Exception {
        var event = service.append(new AppendEventRequest("USER_LOGIN", "alice", "CLIENT_ACCOUNT", "acct-1", mapper.readTree("{\"outcome\":\"SUCCESS\"}"), null, null));
        repository.findByEventId(event.eventId()).orElseThrow();
        var managed = repository.findByEventId(event.eventId()).orElseThrow();
        var field = AuditEvent.class.getDeclaredField("actorId"); field.setAccessible(true); field.set(managed, "mallory");
        repository.flush();
        assertThat(service.verify().intact()).isFalse();
        assertThat(service.verify().firstViolation().type()).isEqualTo("CONTENT_HASH_MISMATCH");
    }

    @Test
    void redactionAndArchiveDoNotBreakVerification() throws Exception {
        var event = service.append(new AppendEventRequest("RECORD_UPDATED", "alice", "CLIENT_ACCOUNT", "acct-1", mapper.readTree("{\"holder\":{\"accountNumber\":\"123\"}}"), null, java.util.List.of("holder.accountNumber")));
        service.redact(event.eventId(), new RedactionRequest(java.util.List.of("holder.accountNumber"), "privacy-officer", "request"));
        service.archive(java.time.Instant.now().plusSeconds(60), "test");
        assertThat(service.verify().intact()).isTrue();
    }
}
