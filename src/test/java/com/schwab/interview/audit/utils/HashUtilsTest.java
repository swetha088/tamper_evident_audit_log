package com.schwab.interview.audit.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HashUtilsTest {
  @Nested
  class SuccessScenario {
    @Test
    void createsStableSha256AndHmacValues() {
      assertThat(HashUtils.sha256("audit"))
          .isEqualTo("b81f37a043a6f767e7c94d105f4bd31282f3ecc20680bb9d09bd93461cf4c863");
      assertThat(HashUtils.hmacSha256("key", "audit"))
          .isEqualTo(HashUtils.hmacSha256("key", "audit"))
          .matches("[0-9a-f]{64}");
      assertThat(HashUtils.constantTimeEquals("same", "same")).isTrue();
    }
  }

  @Nested
  class ExceptionScenario {
    @Test
    void rejectsNullHashInputs() {
      assertThatNullPointerException().isThrownBy(() -> HashUtils.sha256(null));
      assertThatNullPointerException().isThrownBy(() -> HashUtils.hmacSha256(null, "audit"));
    }

    @Test
    void treatsDifferentOrMissingValuesAsNonMatching() {
      assertThat(HashUtils.constantTimeEquals("one", "two")).isFalse();
      assertThat(HashUtils.constantTimeEquals("one", null)).isFalse();
    }
  }
}
