package com.schwab.interview.audit.utils;

import com.schwab.interview.audit.config.AuditPerformanceProperties;
import com.schwab.interview.audit.dto.AuditDtos.AppendEventRequest;
import com.schwab.interview.audit.dto.AuditDtos.VerificationResponse;
import com.schwab.interview.audit.facade.AuditLogFacade;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
@Profile("performance")
public class AuditPerformanceProbe implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(AuditPerformanceProbe.class);

  private final AuditPerformanceProperties properties;
  private final AuditLogFacade facade;
  private final ObjectMapper json;

  public AuditPerformanceProbe(
      AuditPerformanceProperties properties, AuditLogFacade facade, ObjectMapper json) {
    this.properties = properties;
    this.facade = facade;
    this.json = json;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (!properties.isEnabled()) {
      log.info("Audit performance probe is disabled");
      return;
    }
    if (properties.getEventCount() < 1) {
      throw new IllegalArgumentException("audit.performance.event-count must be positive");
    }

    long appendStartedAt = System.nanoTime();
    for (int eventNumber = 1; eventNumber <= properties.getEventCount(); eventNumber++) {
      ObjectNode payload = json.createObjectNode();
      payload.put("eventNumber", eventNumber);
      payload.put("probe", "single-process-baseline");
      facade.append(
          new AppendEventRequest(
              "PERFORMANCE_PROBE",
              "performance-runner",
              "PERFORMANCE_RUN",
              "baseline",
              payload,
              Instant.now(),
              List.of()));
    }
    long appendElapsedNanos = System.nanoTime() - appendStartedAt;

    long verificationStartedAt = System.nanoTime();
    VerificationResponse verification = facade.verify();
    long verificationElapsedNanos = System.nanoTime() - verificationStartedAt;
    if (!verification.intact()) {
      throw new IllegalStateException(
          "Performance probe chain verification failed: " + verification);
    }

    log.info(
        "Audit performance baseline: events={}, appendTotalMs={}, appendAverageMs={}, "
            + "verificationTotalMs={}, verifiedEvents={}",
        properties.getEventCount(),
        nanosToMillis(appendElapsedNanos),
        nanosToMillis(appendElapsedNanos) / properties.getEventCount(),
        nanosToMillis(verificationElapsedNanos),
        verification.recordsChecked());
  }

  private double nanosToMillis(long nanos) {
    return nanos / 1_000_000.0;
  }
}
