package com.schwab.interview.audit.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
      "spring.datasource.url=jdbc:h2:mem:audit-prod-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
      "spring.flyway.baseline-on-migrate=false",
      "audit.security.username=test-audit-user",
      "audit.security.password=test-audit-password",
      "audit.integrity.checkpoint-key=test-checkpoint-key"
    })
@ActiveProfiles("prod")
@AutoConfigureMockMvc
class ProductionSecurityIntegrationTest {
  @Autowired private MockMvc mockMvc;

  @Nested
  class SuccessScenario {
    @Test
    void acceptsConfiguredBasicCredentials() throws Exception {
      mockMvc
          .perform(
              get("/audit/verify")
                  .with(
                      request -> {
                        request.addHeader(
                            "Authorization",
                            "Basic dGVzdC1hdWRpdC11c2VyOnRlc3QtYXVkaXQtcGFzc3dvcmQ=");
                        return request;
                      }))
          .andExpect(status().isOk());
    }
  }

  @Nested
  class ExceptionScenario {
    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
      mockMvc.perform(get("/audit/verify")).andExpect(status().isUnauthorized());
    }
  }
}
