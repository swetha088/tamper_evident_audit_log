package com.schwab.interview.audit.facade;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the configured retention policy in bounded, explicit production deployments. */
@Component
@ConditionalOnProperty(prefix = "audit.retention.scheduler", name = "enabled", havingValue = "true")
public class AuditRetentionScheduler {
  private final AuditLogFacade facade;

  public AuditRetentionScheduler(AuditLogFacade facade) {
    this.facade = facade;
  }

  @Scheduled(cron = "${audit.retention.scheduler.cron:0 0 * * * *}")
  public void archiveExpiredEvents() {
    facade.archive(null, "scheduled-retention-policy");
  }
}
