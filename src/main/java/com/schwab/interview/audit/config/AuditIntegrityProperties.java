package com.schwab.interview.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.integrity")
public class AuditIntegrityProperties {
  private String checkpointKey;
  private String keyId = "local-v1";

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
