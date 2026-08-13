package com.schwab.interview.audit.utils;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.schwab.interview.audit.config.AuditPerformanceProperties;
import com.schwab.interview.audit.dto.AuditDtos.VerificationResponse;
import com.schwab.interview.audit.facade.AuditLogFacade;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

class AuditPerformanceProbeTest {
  private final AuditPerformanceProperties properties = new AuditPerformanceProperties();
  private final AuditLogFacade facade = Mockito.mock(AuditLogFacade.class);
  private final AuditPerformanceProbe probe =
      new AuditPerformanceProbe(properties, facade, new ObjectMapper());

  @Nested
  class SuccessScenario {
    @Test
    void doesNothingWhenTheProbeIsDisabled() {
      properties.setEnabled(false);

      probe.run(null);

      verify(facade, never()).append(any());
      verify(facade, never()).verify();
    }

    @Test
    void appendsConfiguredEventsAndVerifiesTheResult() {
      properties.setEnabled(true);
      properties.setEventCount(2);
      when(facade.verify()).thenReturn(new VerificationResponse(true, 2, null));

      probe.run(null);

      verify(facade, times(2)).append(any());
      verify(facade).verify();
    }
  }

  @Nested
  class ExceptionScenario {
    @Test
    void rejectsANonPositiveEventCount() {
      properties.setEnabled(true);
      properties.setEventCount(0);

      assertThatIllegalArgumentException().isThrownBy(() -> probe.run(null));
    }

    @Test
    void failsWhenThePostRunChainCheckFails() {
      properties.setEnabled(true);
      properties.setEventCount(1);
      when(facade.verify()).thenReturn(new VerificationResponse(false, 1, null));

      assertThatIllegalStateException().isThrownBy(() -> probe.run(null));
    }
  }
}
