package com.schwab.interview.audit.controller;
import com.schwab.interview.audit.dto.AuditDtos.*; import com.schwab.interview.audit.facade.AuditLogFacade; import com.schwab.interview.audit.utils.AuditConstants; import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.time.Instant; import java.util.Map;
@RestController @RequestMapping(AuditConstants.API_ROOT) @Tag(name="Audit Log")
public class AuditLogController { private final AuditLogFacade facade; public AuditLogController(AuditLogFacade facade){this.facade=facade;}
 @PostMapping(AuditConstants.EVENTS_PATH) @ResponseStatus(HttpStatus.CREATED) @Operation(summary="Append an audit event") public EventResponse append(@Valid @RequestBody AppendEventRequest r){return facade.append(r);}
 @GetMapping(AuditConstants.EVENTS_PATH) @Operation(summary="Query audit events") public EventPageResponse query(@RequestParam(required=false)String actorId,@RequestParam(required=false)String resourceType,@RequestParam(required=false)String resourceId,@RequestParam(required=false)String eventType,@RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to,@RequestParam(required=false)Long cursor,@RequestParam(defaultValue="50")int limit){return facade.query(actorId,resourceType,resourceId,eventType,from,to,cursor,limit);}
 @GetMapping(AuditConstants.VERIFY_PATH) @Operation(summary="Verify the full hash chain") public VerificationResponse verify(){return facade.verify();}
 @PostMapping("/events/{eventId}/redactions") public EventResponse redact(@PathVariable String eventId,@Valid @RequestBody RedactionRequest r){return facade.redact(eventId,r);}
 @PostMapping("/retention/run") public Map<String,Object> archive(@Valid @RequestBody RetentionRequest r){return facade.archive(r.cutoff(),r.policy());}
}
