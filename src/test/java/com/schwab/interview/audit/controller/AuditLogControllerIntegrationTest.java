package com.schwab.interview.audit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schwab.interview.audit.dto.AuditDtos.AppendEventRequest;
import com.schwab.interview.audit.facade.AuditLogFacade;
import com.schwab.interview.audit.support.AuditIntegrationTestSupport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

class AuditLogControllerIntegrationTest extends AuditIntegrationTestSupport {
  @Autowired private MockMvc mockMvc;
  @Autowired private AuditLogFacade facade;
  @Autowired private ObjectMapper json;

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

    @Test
    void exposesQueryExportReportRedactionAndRetentionEndpoints() throws Exception {
      var event =
          facade.append(
              new AppendEventRequest(
                  "CLIENT_DATA_READ",
                  "advisor-7",
                  "CLIENT_ACCOUNT",
                  "acct-42",
                  json.readTree("{\"secret\":\"value\"}"),
                  null,
                  java.util.List.of("secret")));

      mockMvc
          .perform(get("/audit/events").param("actorId", "advisor-7"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items[0].eventId").value(event.eventId()));
      mockMvc
          .perform(get("/audit/export").param("actorId", "advisor-7"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.records[0].eventId").value(event.eventId()));
      mockMvc
          .perform(get("/audit/reports/client-account-access").param("clientAccountId", "acct-42"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.events[0].eventId").value(event.eventId()));
      mockMvc
          .perform(
              post("/audit/events/{eventId}/redactions", event.eventId())
                  .contentType("application/json")
                  .content(
                      "{\"paths\":[\"secret\"],\"actorId\":\"privacy\",\"reason\":\"request\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("REDACTED"));
      mockMvc
          .perform(
              post("/audit/retention/run")
                  .contentType("application/json")
                  .content("{\"cutoff\":\"2100-01-01T00:00:00Z\",\"policy\":\"test\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.archived").value(1));
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

    @Test
    void returnsNotFoundForAnUnknownRedactionEvent() throws Exception {
      mockMvc
          .perform(
              post("/audit/events/not-found/redactions")
                  .contentType("application/json")
                  .content(
                      "{\"paths\":[\"secret\"],\"actorId\":\"privacy\",\"reason\":\"request\"}"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
  }
}
