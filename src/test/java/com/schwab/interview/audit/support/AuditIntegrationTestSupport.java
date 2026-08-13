package com.schwab.interview.audit.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:audit-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.flyway.baseline-on-migrate=false"
    })
@AutoConfigureMockMvc
@Transactional
public abstract class AuditIntegrationTestSupport {}
