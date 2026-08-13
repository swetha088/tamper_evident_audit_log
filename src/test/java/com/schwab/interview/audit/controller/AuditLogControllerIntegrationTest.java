package com.schwab.interview.audit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schwab.interview.audit.support.AuditIntegrationTestSupport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AuditLogControllerIntegrationTest extends AuditIntegrationTestSupport {
  @Autowired private MockMvc mockMvc;

  @Nested
  class SuccessScenario {
    @Test
    void appendsAnEventAndVerifiesItOverHttp() throws Exception {
      mockMvc
          .perform(
              post("/audit/events")
                  .contentType("application/json")
                  .content(
                      """
                      {"eventType":"USER_LOGIN","actorId":"alice","resourceType":"ACCOUNT",
                      "resourceId":"one","payload":{"outcome":"SUCCESS"}}
                      """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.eventType").value("USER_LOGIN"));

      mockMvc
          .perform(get("/audit/verify"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.intact").value(true));
    }
  }

  @Nested
  class ExceptionScenario {
    @Test
    void rejectsAnExportWithoutTheRequiredSelector() throws Exception {
      mockMvc
          .perform(get("/audit/export"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsAnInvalidAppendRequest() throws Exception {
      mockMvc
          .perform(
              post("/audit/events").contentType("application/json").content("{\"payload\":{}}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
  }
}
