package com.schwab.interview.audit.dto;

import com.schwab.interview.audit.entity.AuditEventEntity; import jakarta.validation.constraints.*; import tools.jackson.databind.JsonNode; import java.time.Instant; import java.util.*;
public final class AuditDtos {
 private AuditDtos() { }
 public record AppendEventRequest(@NotBlank String eventType,@NotBlank String actorId,@NotBlank String resourceType,@NotBlank String resourceId,@NotNull JsonNode payload,Instant occurredAt,List<String> sensitivePaths) { }
 public record RedactionRequest(@NotEmpty List<String> paths,@NotBlank String actorId,@NotBlank String reason) { }
 public record RetentionRequest(@NotNull Instant cutoff,String policy) { }
 public record EventResponse(Long sequence,String eventId,String eventType,String actorId,String resourceType,String resourceId,Instant occurredAt,Instant recordedAt,JsonNode payload,List<String> sensitivePaths,String previousHash,String contentHash,AuditEventEntity.Status status,Instant archivedAt,String archivePolicy,JsonNode redactionCertificate) { }
 public record EventPageResponse(List<EventResponse> items,String nextCursor) { }
 public record ViolationResponse(Long sequence,String eventId,String type) { }
 public record VerificationResponse(boolean intact,long recordsChecked,ViolationResponse firstViolation) { }
 public record ErrorResponse(String code,String message,String traceId) { }
}
