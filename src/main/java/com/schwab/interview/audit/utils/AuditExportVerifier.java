package com.schwab.interview.audit.utils;

import com.schwab.interview.audit.dto.AuditDtos.ChainWitness;
import com.schwab.interview.audit.dto.AuditDtos.ExportBundleResponse;
import com.schwab.interview.audit.dto.AuditDtos.ExportRecord;
import com.schwab.interview.audit.dto.AuditDtos.VerificationResponse;
import com.schwab.interview.audit.dto.AuditDtos.ViolationResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

/**
 * Verifies an export bundle without querying the audit database. Consumers can use this component
 * in a separate process after deserializing a bundle.
 */
@Component
public class AuditExportVerifier {
  private static final String EXPORT_FORMAT_VERSION = "audit-export-v1";

  private final ObjectMapper json;

  public AuditExportVerifier(ObjectMapper json) {
    this.json = json;
  }

  public VerificationResponse verify(ExportBundleResponse bundle) {
    if (bundle == null
        || !EXPORT_FORMAT_VERSION.equals(bundle.formatVersion())
        || !AuditConstants.SHA_256.equals(bundle.hashAlgorithm())
        || !AuditConstants.HASH_VERSION.equals(bundle.hashVersion())) {
      return broken(null, null, "EXPORT_FORMAT_MISMATCH", 0);
    }

    List<ChainWitness> witnesses =
        bundle.chainWitnesses() == null ? List.of() : bundle.chainWitnesses();
    Map<Long, ChainWitness> witnessesBySequence = new HashMap<>();
    String previousHash = AuditConstants.GENESIS_HASH;
    Long previousSequence = null;
    long checked = 0;
    for (ChainWitness witness : witnesses) {
      if (witness == null
          || witness.sequence() == null
          || witness.eventId() == null
          || witness.contentHash() == null
          || !previousHash.equals(witness.previousHash())
          || (previousSequence != null && witness.sequence() <= previousSequence)
          || witnessesBySequence.putIfAbsent(witness.sequence(), witness) != null) {
        return broken(
            witness == null ? null : witness.sequence(),
            witness == null ? null : witness.eventId(),
            "EXPORT_CHAIN_WITNESS_MISMATCH",
            checked);
      }
      previousHash = witness.contentHash();
      previousSequence = witness.sequence();
      checked++;
    }

    List<ExportRecord> records = bundle.records() == null ? List.of() : bundle.records();
    for (ExportRecord record : records) {
      ChainWitness witness = record == null ? null : witnessesBySequence.get(record.sequence());
      if (record == null
          || witness == null
          || !record.eventId().equals(witness.eventId())
          || !record.previousHash().equals(witness.previousHash())
          || !record.contentHash().equals(witness.contentHash())) {
        return broken(
            record == null ? null : record.sequence(),
            record == null ? null : record.eventId(),
            "EXPORT_RECORD_WITNESS_MISMATCH",
            checked);
      }
      if (!HashUtils.sha256(canonical(hashInput(record))).equals(record.contentHash())) {
        return broken(record.sequence(), record.eventId(), "EXPORT_CONTENT_HASH_MISMATCH", checked);
      }
    }
    return new VerificationResponse(true, checked, null);
  }

  private Map<String, Object> hashInput(ExportRecord record) {
    Map<String, Object> input = new TreeMap<>();
    input.put("hashVersion", AuditConstants.HASH_VERSION);
    input.put("sequence", record.sequence());
    input.put("eventId", record.eventId());
    input.put("eventType", record.eventType());
    input.put("actorId", record.actorId());
    input.put("resourceType", record.resourceType());
    input.put("resourceId", record.resourceId());
    input.put("occurredAt", record.occurredAt());
    input.put("recordedAt", record.recordedAt());
    input.put("payload", record.protectedPayload());
    input.put(
        "sensitivePaths",
        (record.sensitivePaths() == null ? List.<String>of() : record.sensitivePaths())
            .stream().sorted(Comparator.naturalOrder()).toList());
    input.put("previousHash", record.previousHash());
    return input;
  }

  private VerificationResponse broken(Long sequence, String eventId, String type, long checked) {
    return new VerificationResponse(false, checked, new ViolationResponse(sequence, eventId, type));
  }

  private String canonical(Object value) {
    try {
      return json.writer()
          .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
          .writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Unable to canonicalize audit export", exception);
    }
  }
}
