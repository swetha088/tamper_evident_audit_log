package com.schwab.interview.audit.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit.retention")
public class AuditRetentionProperties {
  private Duration defaultWindow = Duration.ofDays(90);
  private Scheduler scheduler = new Scheduler();

  public Duration getDefaultWindow() {
    return defaultWindow;
  }

  public void setDefaultWindow(Duration defaultWindow) {
    this.defaultWindow = defaultWindow;
  }

  public Scheduler getScheduler() {
    return scheduler;
  }

  public void setScheduler(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  public static class Scheduler {
    private boolean enabled;
    private String cron = "0 0 * * * *";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getCron() {
      return cron;
    }

    public void setCron(String cron) {
      this.cron = cron;
    }
  }
}
