package com.schwab.interview.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.performance")
public class AuditPerformanceProperties {
  private boolean enabled;
  private int eventCount = 1000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getEventCount() {
    return eventCount;
  }

  public void setEventCount(int eventCount) {
    this.eventCount = eventCount;
  }
}
