package com.schwab.interview.audit;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class ApiModels {
    private ApiModels() { }
    public record AppendEventRequest(@NotBlank String eventType, @NotBlank String actorId, @NotBlank String resourceType,
                                     @NotBlank String resourceId, @NotNull JsonNode payload, Instant occurredAt, List<String> sensitivePaths) { }
    public record RedactionRequest(@NotEmpty List<String> paths, @NotBlank String actorId, @NotBlank String reason) { }
    public record RetentionRequest(@NotNull Instant cutoff, String policy) { }
    public record EventResponse(Long sequence, String eventId, String eventType, String actorId, String resourceType, String resourceId,
                                Instant occurredAt, Instant recordedAt, JsonNode payload, List<String> sensitivePaths, String previousHash,
                                String contentHash, AuditEvent.EventStatus status, Instant archivedAt, String archivePolicy, JsonNode redactionCertificate) { }
    public record EventPage(List<EventResponse> items, String nextCursor) { }
    public record Violation(Long sequence, String eventId, String type) { }
    public record VerificationResult(boolean intact, long recordsChecked, Violation firstViolation) { }
    public record ExportBundle(String formatVersion, String algorithm, String genesisHash, List<EventResponse> selectedRecords, List<Map<String, Object>> chainWitnesses) { }
}
