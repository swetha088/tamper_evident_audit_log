package com.schwab.interview.audit.dto;

import com.schwab.interview.audit.entity.AuditEventEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record AppendEventRequest(@NotBlank String eventType, @NotBlank String actorId,
                                     @NotBlank String resourceType, @NotBlank String resourceId,
                                     @NotNull JsonNode payload, Instant occurredAt,
                                     List<String> sensitivePaths) {
    }

    public record RedactionRequest(@NotEmpty List<String> paths, @NotBlank String actorId,
                                   @NotBlank String reason) {
    }

    public record RetentionRequest(@NotNull Instant cutoff, String policy) {
    }

    public record EventResponse(Long sequence, String eventId, String eventType, String actorId,
                                String resourceType, String resourceId, Instant occurredAt,
                                Instant recordedAt, JsonNode payload, List<String> sensitivePaths,
                                String previousHash, String contentHash, AuditEventEntity.Status status,
                                Instant archivedAt, String archivePolicy, JsonNode redactionCertificate) {
    }

    public record EventPageResponse(List<EventResponse> items, String nextCursor) {
    }

    public record ViolationResponse(Long sequence, String eventId, String type) {
    }

    public record VerificationResponse(boolean intact, long recordsChecked,
                                       ViolationResponse firstViolation) {
    }

    public record ExportCriteria(String actorId, String resourceId, Instant from, Instant to) {
    }

    public record ExportRecord(Long sequence, String eventId, String eventType, String actorId,
                               String resourceType, String resourceId, Instant occurredAt,
                               Instant recordedAt, JsonNode payload, JsonNode protectedPayload,
                               List<String> sensitivePaths, String previousHash, String contentHash) {
    }

    public record ChainWitness(Long sequence, String eventId, String previousHash, String contentHash) {
    }

    public record ExportBundleResponse(String formatVersion, String hashAlgorithm, String hashVersion,
                                       Instant exportedAt, ExportCriteria criteria,
                                       List<ExportRecord> records, List<ChainWitness> chainWitnesses,
                                       VerificationResponse verification) {
    }

    public record ErrorResponse(String code, String message, String traceId) {
    }
}
