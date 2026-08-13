package com.schwab.interview.audit.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.schwab.interview.audit.facade.AuditLogFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:performance-profile-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.flyway.baseline-on-migrate=false",
      "audit.performance.event-count=1"
    })
@ActiveProfiles("performance")
class PerformanceProfileIntegrationTest {
  @Autowired private AuditLogFacade facade;

  @Test
  void runsThePerformanceProfileWithoutServletSecurityBeans() {
    assertThat(facade.verify().intact()).isTrue();
    assertThat(facade.verify().recordsChecked()).isEqualTo(1);
  }
}
