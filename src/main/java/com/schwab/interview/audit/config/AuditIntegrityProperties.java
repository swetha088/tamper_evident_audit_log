package com.schwab.interview.audit.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "audit.integrity")
public class AuditIntegrityProperties {
  @NotBlank private String checkpointKey;

  @NotBlank private String keyId = "local-v1";

  public String getCheckpointKey() {
    return checkpointKey;
  }

  public void setCheckpointKey(String checkpointKey) {
    this.checkpointKey = checkpointKey;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }
}
