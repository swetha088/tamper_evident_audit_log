package com.schwab.interview.audit.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.schwab.interview.audit.dto.AuditDtos.AppendEventRequest;
import com.schwab.interview.audit.dto.AuditDtos.ExportBundleResponse;
import com.schwab.interview.audit.dto.AuditDtos.ExportRecord;
import com.schwab.interview.audit.facade.AuditLogFacade;
import com.schwab.interview.audit.support.AuditIntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

class AuditExportVerifierIntegrationTest extends AuditIntegrationTestSupport {
  @Autowired private AuditLogFacade facade;
  @Autowired private AuditExportVerifier verifier;
  @Autowired private ObjectMapper json;

  @Nested
  class SuccessScenario {
    @Test
    void verifiesASerializedBundleWithoutDatabaseAccess() throws Exception {
      facade.append(
          new AppendEventRequest(
              "READ",
              "alice",
              "ACCOUNT",
              "one",
              json.readTree("{\"secret\":\"value\"}"),
              null,
              List.of("secret")));
      ExportBundleResponse bundle = facade.export("alice", null, null, null);
      ExportBundleResponse downloadedBundle =
          json.readValue(json.writeValueAsString(bundle), ExportBundleResponse.class);

      assertThat(verifier.verify(downloadedBundle).intact()).isTrue();
    }
  }

  @Nested
  class ExceptionScenario {
    @Test
    void detectsAChangedExportedRecord() throws Exception {
      facade.append(
          new AppendEventRequest(
              "READ",
              "alice",
              "ACCOUNT",
              "one",
              json.readTree("{\"outcome\":\"ALLOW\"}"),
              null,
              null));
      ExportBundleResponse bundle = facade.export("alice", null, null, null);
      ExportRecord original = bundle.records().getFirst();
      ExportRecord changed =
          new ExportRecord(
              original.sequence(),
              original.eventId(),
              original.eventType(),
              "mallory",
              original.resourceType(),
              original.resourceId(),
              original.occurredAt(),
              original.recordedAt(),
              original.payload(),
              original.protectedPayload(),
              original.sensitivePaths(),
              original.previousHash(),
              original.contentHash());
      ExportBundleResponse tampered =
          new ExportBundleResponse(
              bundle.formatVersion(),
              bundle.hashAlgorithm(),
              bundle.hashVersion(),
              bundle.exportedAt(),
              bundle.criteria(),
              List.of(changed),
              bundle.chainWitnesses(),
              bundle.verification());

      assertThat(verifier.verify(tampered).firstViolation().type())
          .isEqualTo("EXPORT_CONTENT_HASH_MISMATCH");
    }

    @Test
    void detectsABrokenExportWitnessChain() throws Exception {
      facade.append(
          new AppendEventRequest(
              "READ", "alice", "ACCOUNT", "one", json.readTree("{}"), null, null));
      ExportBundleResponse bundle = facade.export("alice", null, null, null);
      var witness = bundle.chainWitnesses().getFirst();
      var brokenWitness =
          new com.schwab.interview.audit.dto.AuditDtos.ChainWitness(
              witness.sequence(), witness.eventId(), "not-a-genesis-hash", witness.contentHash());
      ExportBundleResponse tampered =
          new ExportBundleResponse(
              bundle.formatVersion(),
              bundle.hashAlgorithm(),
              bundle.hashVersion(),
              bundle.exportedAt(),
              bundle.criteria(),
              bundle.records(),
              List.of(brokenWitness),
              bundle.verification());

      assertThat(verifier.verify(tampered).firstViolation().type())
          .isEqualTo("EXPORT_CHAIN_WITNESS_MISMATCH");
    }
  }
}
