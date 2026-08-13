package com.schwab.interview.audit.facade;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuditRetentionSchedulerTest {
  @Test
  void appliesTheDefaultRetentionPolicyWhenTriggered() {
    AuditLogFacade facade = Mockito.mock(AuditLogFacade.class);

    new AuditRetentionScheduler(facade).archiveExpiredEvents();

    verify(facade).archive(null, "scheduled-retention-policy");
  }
}
