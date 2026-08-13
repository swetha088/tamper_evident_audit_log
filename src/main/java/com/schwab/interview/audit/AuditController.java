package com.schwab.interview.audit;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import static com.schwab.interview.audit.ApiModels.*;

@RestController @RequestMapping("/audit")
public class AuditController {
    private final AuditService service; public AuditController(AuditService service) { this.service = service; }
    @PostMapping("/events") @ResponseStatus(HttpStatus.CREATED) public EventResponse append(@Valid @RequestBody AppendEventRequest request) { return service.append(request); }
    @GetMapping("/events") public EventPage query(@RequestParam(required=false) String actorId,@RequestParam(required=false) String resourceType,@RequestParam(required=false) String resourceId,@RequestParam(required=false) String eventType,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,@RequestParam(required=false) Long cursor,@RequestParam(defaultValue="50") int limit) { return service.query(actorId,resourceType,resourceId,eventType,from,to,cursor,limit); }
    @GetMapping("/verify") public VerificationResult verify() { return service.verify(); }
    @PostMapping("/events/{eventId}/redactions") public EventResponse redact(@PathVariable String eventId,@Valid @RequestBody RedactionRequest request) { return service.redact(eventId,request); }
    @PostMapping("/retention/run") public Map<String,Object> archive(@Valid @RequestBody RetentionRequest request) { return service.archive(request.cutoff(),request.policy()); }
    @GetMapping("/export") public ExportBundle export(@RequestParam(required=false) String actorId,@RequestParam(required=false) String resourceId) { return service.export(actorId,resourceId); }
    @GetMapping("/reports/client-account-access") public Map<String,Object> report(@RequestParam String clientAccountId,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to) { return service.clientAccountReport(clientAccountId,from,to); }
    @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.bind.MethodArgumentNotValidException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST) Map<String,String> invalid(Exception e) { return Map.of("code","INVALID_REQUEST","message",e.getMessage()); }
    @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(HttpStatus.NOT_FOUND) Map<String,String> missing(Exception e) { return Map.of("code","NOT_FOUND","message",e.getMessage()); }
}
