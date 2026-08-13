package com.schwab.interview.audit.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.schwab.interview.audit.dto.AuditDtos.AppendEventRequest;
import com.schwab.interview.audit.dto.AuditDtos.RedactionRequest;
import com.schwab.interview.audit.entity.AuditEventEntity.Status;
import com.schwab.interview.audit.repository.AuditChainStateRepository;
import com.schwab.interview.audit.repository.AuditEventRepository;
import com.schwab.interview.audit.support.AuditIntegrationTestSupport;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

class AuditLogFacadeIntegrationTest extends AuditIntegrationTestSupport {
  @Autowired private AuditLogFacade facade;
  @Autowired private AuditEventRepository repository;
  @Autowired private AuditChainStateRepository chainStateRepository;
  @Autowired private ObjectMapper json;

  @Nested
  class SuccessScenario {
    @Test
    void appendsAndVerifiesAnUntamperedEvent() throws Exception {
      var event = append("USER_LOGIN", "alice", "ACCOUNT", "one", "{\"outcome\":\"SUCCESS\"}");

      assertThat(event.eventId()).isNotBlank();
      assertThat(facade.verify().intact()).isTrue();
      assertThat(facade.verify().recordsChecked()).isEqualTo(1);
    }

    @Test
    void redactsSensitiveDataWithoutBreakingTheChain() throws Exception {
      var event =
          facade.append(
              new AppendEventRequest(
                  "READ",
                  "alice",
                  "ACCOUNT",
                  "one",
                  json.readTree("{\"client\":{\"ssn\":\"123\"}}"),
                  null,
                  List.of("client.ssn")));

      facade.redact(
          event.eventId(), new RedactionRequest(List.of("client.ssn"), "privacy", "request"));

      assertThat(facade.verify().intact()).isTrue();
    }

    @Test
    void exportsSelectedRecordsWithTheFullChainWitness() throws Exception {
      append("READ", "alice", "ACCOUNT", "one", "{\"outcome\":\"ALLOW\"}");
      append("UPDATE", "bob", "ACCOUNT", "two", "{\"outcome\":\"ALLOW\"}");
      append("READ", "alice", "ACCOUNT", "one", "{\"outcome\":\"ALLOW\"}");

      var bundle = facade.export(null, "one", null, null);

      assertThat(bundle.records()).hasSize(2);
      assertThat(bundle.chainWitnesses()).hasSize(3);
      assertThat(bundle.verification().intact()).isTrue();
    }

    @Test
    void reportsOnlyClientAccountReadEvents() throws Exception {
      append(
          "CLIENT_DATA_READ", "advisor-7", "CLIENT_ACCOUNT", "acct-42", "{\"outcome\":\"ALLOW\"}");
      append(
          "CLIENT_ACCOUNT_UPDATED",
          "advisor-7",
          "CLIENT_ACCOUNT",
          "acct-42",
          "{\"outcome\":\"ALLOW\"}");

      var report = facade.clientAccountAccessReport("acct-42", null, null);

      assertThat(report.events())
          .singleElement()
          .extracting("eventType")
          .isEqualTo("CLIENT_DATA_READ");
      assertThat(report.verification().intact()).isTrue();
    }

    @Test
    void archivesRedactedRecordsWithoutBreakingVerification() throws Exception {
      var event =
          facade.append(
              new AppendEventRequest(
                  "READ",
                  "alice",
                  "ACCOUNT",
                  "one",
                  json.readTree("{\"client\":{\"ssn\":\"123\"}}"),
                  null,
                  List.of("client.ssn")));
      facade.redact(
          event.eventId(), new RedactionRequest(List.of("client.ssn"), "privacy", "request"));

      var result = facade.archive(Instant.now().plusSeconds(1), "privacy-retention");

      assertThat(result.get("archived")).isEqualTo(1);
      assertThat(facade.query(null, null, null, null, null, null, null, 10).items())
          .singleElement()
          .extracting("status")
          .isEqualTo(Status.ARCHIVED);
      assertThat(facade.verify().intact()).isTrue();
    }

    @Test
    void filtersAndCursorPaginatesEvents() throws Exception {
      append("READ", "alice", "ACCOUNT", "one", "{\"outcome\":\"ALLOW\"}");
      var second = append("READ", "alice", "ACCOUNT", "two", "{\"outcome\":\"ALLOW\"}");
      append("UPDATE", "bob", "ACCOUNT", "three", "{\"outcome\":\"ALLOW\"}");

      var firstPage = facade.query("alice", null, null, null, null, null, null, 1);
      var secondPage =
          facade.query("alice", null, null, null, null, null, second.sequence() - 1, 1);

      assertThat(firstPage.items()).singleElement().extracting("actorId").isEqualTo("alice");
      assertThat(firstPage.nextCursor()).isNotBlank();
      assertThat(secondPage.items()).singleElement().extracting("resourceId").isEqualTo("two");
    }

    @Test
    void normalizesCallerTimestampPrecisionBeforeHashing() throws Exception {
      var originalTimestamp = java.time.Instant.parse("2026-08-13T12:34:56.123456789Z");
      facade.append(
          new AppendEventRequest(
              "READ", "alice", "ACCOUNT", "one", json.readTree("{}"), originalTimestamp, null));

      assertThat(facade.query(null, null, null, null, null, null, null, 10).items())
          .singleElement()
          .extracting("occurredAt")
          .isEqualTo(java.time.Instant.parse("2026-08-13T12:34:56.123456Z"));
      assertThat(facade.verify().intact()).isTrue();
    }
  }

  @Nested
  class ExceptionScenario {
    @Test
    void detectsAChangedPersistedEvent() throws Exception {
      var event = append("USER_LOGIN", "alice", "ACCOUNT", "one", "{\"outcome\":\"SUCCESS\"}");
      var stored = repository.findByEventId(event.eventId()).orElseThrow();
      var field = stored.getClass().getDeclaredField("actorId");
      field.setAccessible(true);
      field.set(stored, "mallory");
      repository.flush();

      assertThat(facade.verify().firstViolation().type()).isEqualTo("CONTENT_HASH_MISMATCH");
    }

    @Test
    void detectsDeletionOfTheCurrentChainTail() throws Exception {
      append("READ", "alice", "ACCOUNT", "one", "{\"outcome\":\"ALLOW\"}");
      var tail = append("READ", "alice", "ACCOUNT", "two", "{\"outcome\":\"ALLOW\"}");
      repository.deleteById(tail.sequence());
      repository.flush();

      assertThat(facade.verify().firstViolation().type()).isEqualTo("CHAIN_HEAD_MISMATCH");
    }

    @Test
    void detectsMissingAndInvalidChainCheckpointSignatures() throws Exception {
      append("READ", "alice", "ACCOUNT", "one", "{\"outcome\":\"ALLOW\"}");
      var state = chainStateRepository.findById(1L).orElseThrow();
      var signature = state.getClass().getDeclaredField("headSignature");
      signature.setAccessible(true);
      signature.set(state, null);
      chainStateRepository.saveAndFlush(state);
      assertThat(facade.verify().firstViolation().type()).isEqualTo("CHAIN_HEAD_SIGNATURE_MISSING");

      signature.set(state, "0".repeat(64));
      chainStateRepository.saveAndFlush(state);
      assertThat(facade.verify().firstViolation().type())
          .isEqualTo("CHAIN_HEAD_SIGNATURE_MISMATCH");
    }

    @Test
    void detectsModifiedLifecycleEvidence() throws Exception {
      var event = append("READ", "alice", "ACCOUNT", "one", "{\"outcome\":\"ALLOW\"}");
      var stored = repository.findByEventId(event.eventId()).orElseThrow();
      var archivePolicy = stored.getClass().getDeclaredField("archivePolicy");
      archivePolicy.setAccessible(true);
      archivePolicy.set(stored, "forged-policy");
      repository.flush();

      assertThat(facade.verify().firstViolation().type()).isEqualTo("LIFECYCLE_SIGNATURE_MISMATCH");
    }

    @Test
    void rejectsMissingOrAmbiguousExportSelectors() {
      assertThatIllegalArgumentException().isThrownBy(() -> facade.export(null, null, null, null));
      assertThatIllegalArgumentException()
          .isThrownBy(() -> facade.export("alice", "one", null, null));
    }

    @Test
    void rejectsInvalidFacadeInputs() throws Exception {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  facade.append(
                      new AppendEventRequest(
                          "READ", "alice", "ACCOUNT", "one", json.readTree("[]"), null, null)));
      assertThatIllegalArgumentException()
          .isThrownBy(() -> facade.clientAccountAccessReport(" ", null, null));
      assertThatIllegalArgumentException()
          .isThrownBy(() -> facade.query(null, null, null, null, null, null, null, 101));
    }

    @Test
    void rejectsInvalidSensitivePathsAndRedactionRequests() throws Exception {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  facade.append(
                      new AppendEventRequest(
                          "READ",
                          "alice",
                          "ACCOUNT",
                          "one",
                          json.readTree("{\"secret\":\"x\"}"),
                          null,
                          List.of("secret", "secret"))));
      var event = append("READ", "alice", "ACCOUNT", "one", "{\"public\":\"value\"}");
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  facade.redact(
                      event.eventId(),
                      new RedactionRequest(List.of("missing"), "privacy", "request")));
    }
  }

  private com.schwab.interview.audit.dto.AuditDtos.EventResponse append(
      String eventType, String actorId, String resourceType, String resourceId, String payload)
      throws Exception {
    return facade.append(
        new AppendEventRequest(
            eventType, actorId, resourceType, resourceId, json.readTree(payload), null, null));
  }
}
