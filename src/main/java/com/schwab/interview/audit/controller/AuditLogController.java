package com.schwab.interview.audit.controller;

import com.schwab.interview.audit.dto.AuditDtos.AppendEventRequest;
import com.schwab.interview.audit.dto.AuditDtos.ClientAccountAccessReport;
import com.schwab.interview.audit.dto.AuditDtos.EventPageResponse;
import com.schwab.interview.audit.dto.AuditDtos.EventResponse;
import com.schwab.interview.audit.dto.AuditDtos.ExportBundleResponse;
import com.schwab.interview.audit.dto.AuditDtos.RedactionRequest;
import com.schwab.interview.audit.dto.AuditDtos.RetentionRequest;
import com.schwab.interview.audit.dto.AuditDtos.VerificationResponse;
import com.schwab.interview.audit.facade.AuditLogFacade;
import com.schwab.interview.audit.utils.AuditConstants;
import com.schwab.interview.audit.utils.AuditExportVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AuditConstants.API_ROOT)
@Tag(name = "Audit Log")
public class AuditLogController {
  private final AuditLogFacade facade;
  private final AuditExportVerifier exportVerifier;

  public AuditLogController(AuditLogFacade facade, AuditExportVerifier exportVerifier) {
    this.facade = facade;
    this.exportVerifier = exportVerifier;
  }

  @PostMapping(AuditConstants.EVENTS_PATH)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Append an audit event")
  public EventResponse append(@Valid @RequestBody AppendEventRequest request) {
    return facade.append(request);
  }

  @GetMapping(AuditConstants.EVENTS_PATH)
  @Operation(summary = "Query audit events")
  public EventPageResponse query(
      @RequestParam(required = false) String actorId,
      @RequestParam(required = false) String resourceType,
      @RequestParam(required = false) String resourceId,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "50") int limit) {
    return facade.query(actorId, resourceType, resourceId, eventType, from, to, cursor, limit);
  }

  @GetMapping(AuditConstants.VERIFY_PATH)
  @Operation(summary = "Verify the full hash chain")
  public VerificationResponse verify() {
    return facade.verify();
  }

  @GetMapping(AuditConstants.EXPORT_PATH)
  @Operation(summary = "Export records with a verifiable full-chain witness")
  @ApiResponse(responseCode = "200", description = "Verifiable export bundle")
  public ExportBundleResponse export(
      @RequestParam(required = false) String actorId,
      @RequestParam(required = false) String resourceId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    return facade.export(actorId, resourceId, from, to);
  }

  @PostMapping("/export/verify")
  @Operation(summary = "Verify an exported bundle without accessing the audit database")
  public VerificationResponse verifyExport(@RequestBody ExportBundleResponse bundle) {
    return exportVerifier.verify(bundle);
  }

  @GetMapping(AuditConstants.CLIENT_ACCOUNT_ACCESS_REPORT_PATH)
  @Operation(summary = "Report client-account data access events")
  public ClientAccountAccessReport clientAccountAccessReport(
      @RequestParam String clientAccountId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    return facade.clientAccountAccessReport(clientAccountId, from, to);
  }

  @PostMapping("/events/{eventId}/redactions")
  public EventResponse redact(
      @PathVariable String eventId, @Valid @RequestBody RedactionRequest request) {
    return facade.redact(eventId, request);
  }

  @PostMapping("/retention/run")
  public Map<String, Object> archive(@Valid @RequestBody RetentionRequest request) {
    return facade.archive(request.cutoff(), request.policy());
  }
}
