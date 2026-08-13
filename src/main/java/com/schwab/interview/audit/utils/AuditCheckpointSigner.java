package com.schwab.interview.audit.utils;

import com.schwab.interview.audit.config.AuditIntegrityProperties;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AuditCheckpointSigner {
  private static final String CHECKPOINT_VERSION = "audit-checkpoint-v1";

  private final AuditIntegrityProperties properties;

  public AuditCheckpointSigner(AuditIntegrityProperties properties) {
    this.properties = properties;
  }

  public String sign(long sequence, String eventId, String contentHash) {
    return HashUtils.hmacSha256(
        properties.getCheckpointKey(), input(sequence, eventId, contentHash));
  }

  public boolean verifies(long sequence, String eventId, String contentHash, String signature) {
    return HashUtils.constantTimeEquals(sign(sequence, eventId, contentHash), signature);
  }

  public String keyId() {
    return properties.getKeyId();
  }

  private String input(long sequence, String eventId, String contentHash) {
    return String.join(
        "\n",
        CHECKPOINT_VERSION,
        Long.toString(sequence),
        Objects.requireNonNull(eventId),
        Objects.requireNonNull(contentHash),
        properties.getKeyId());
  }
}
