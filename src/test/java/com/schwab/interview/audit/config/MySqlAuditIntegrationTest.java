package com.schwab.interview.audit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.schwab.interview.audit.dto.AuditDtos.AppendEventRequest;
import com.schwab.interview.audit.facade.AuditLogFacade;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Runs only when a caller explicitly provides a real MySQL test database. */
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION", matches = "true")
@SpringBootTest(
    properties = {
      "spring.flyway.baseline-on-migrate=false",
      "springdoc.api-docs.enabled=false",
      "springdoc.swagger-ui.enabled=false"
    })
@ActiveProfiles("prod")
@Transactional
class MySqlAuditIntegrationTest {
  @Autowired private AuditLogFacade facade;
  @Autowired private ObjectMapper json;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> requiredEnvironment("AUDIT_TEST_DB_URL"));
    registry.add("spring.datasource.username", () -> requiredEnvironment("AUDIT_TEST_DB_USERNAME"));
    registry.add("spring.datasource.password", () -> requiredEnvironment("AUDIT_TEST_DB_PASSWORD"));
    registry.add("audit.security.username", () -> "mysql-integration-user");
    registry.add("audit.security.password", () -> "mysql-integration-password");
    registry.add("audit.integrity.checkpoint-key", () -> "mysql-integration-checkpoint-key");
  }

  @Nested
  class SuccessScenario {
    @Test
    void appliesFlywaySchemaAndVerifiesARealMySqlAuditChain() throws Exception {
      var event =
          facade.append(
              new AppendEventRequest(
                  "USER_LOGIN",
                  "mysql-tester",
                  "ACCOUNT",
                  "account-1",
                  json.readTree("{\"outcome\":\"SUCCESS\"}"),
                  null,
                  null));

      assertThat(event.sequence()).isPositive();
      assertThat(facade.verify().intact()).isTrue();
    }
  }

  @Nested
  class ExceptionScenario {
    @Test
    void rejectsAnInvalidRangeAgainstRealMySql() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  facade.query(
                      null,
                      null,
                      null,
                      null,
                      java.time.Instant.parse("2026-01-02T00:00:00Z"),
                      java.time.Instant.parse("2026-01-01T00:00:00Z"),
                      null,
                      10));
    }
  }

  private static String requiredEnvironment(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " must be configured when RUN_MYSQL_INTEGRATION=true");
    }
    return value;
  }
}
