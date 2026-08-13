package com.schwab.interview.audit.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:audit-reader-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.flyway.baseline-on-migrate=false",
      "audit.security.username=reader-user",
      "audit.security.password=reader-password",
      "audit.security.roles=AUDIT_READER",
      "audit.integrity.checkpoint-key=reader-test-checkpoint-key"
    })
@ActiveProfiles("prod")
@AutoConfigureMockMvc
class ProductionAuthorizationIntegrationTest {
  private static final String READER_CREDENTIALS = "Basic cmVhZGVyLXVzZXI6cmVhZGVyLXBhc3N3b3Jk";

  @Autowired private MockMvc mockMvc;

  @Nested
  class SuccessScenario {
    @Test
    void allowsAuditReadersToVerifyTheChain() throws Exception {
      mockMvc
          .perform(get("/audit/verify").header("Authorization", READER_CREDENTIALS))
          .andExpect(status().isOk());
    }
  }

  @Nested
  class ExceptionScenario {
    @Test
    void rejectsWritesFromAReadOnlyPrincipal() throws Exception {
      mockMvc
          .perform(
              post("/audit/events")
                  .header("Authorization", READER_CREDENTIALS)
                  .contentType("application/json")
                  .content(
                      """
                      {"eventType":"USER_LOGIN","actorId":"reader-user","resourceType":"ACCOUNT",
                      "resourceId":"one","payload":{"outcome":"SUCCESS"}}
                      """))
          .andExpect(status().isForbidden());
    }
  }
}
