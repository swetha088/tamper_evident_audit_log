package com.schwab.interview.audit;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import static com.schwab.interview.audit.ApiModels.*;

@Service
public class AuditService {
    public static final String GENESIS_HASH = "0".repeat(64);
    private final AuditEventRepository repository;
    private final ObjectMapper mapper;

    public AuditService(AuditEventRepository repository, ObjectMapper mapper) { this.repository = repository; this.mapper = mapper; }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EventResponse append(AppendEventRequest request) {
        if (!request.payload().isObject()) throw new IllegalArgumentException("payload must be a JSON object");
        var paths = request.sensitivePaths() == null ? List.<String>of() : request.sensitivePaths().stream().sorted().toList();
        if (new HashSet<>(paths).size() != paths.size()) throw new IllegalArgumentException("sensitivePaths must not contain duplicates");
        String salt = UUID.randomUUID().toString(); ObjectNode values = mapper.createObjectNode();
        for (String path : paths) values.put(path, sha256(salt + ":" + canonical(requirePath(request.payload(), path))));
        ObjectNode commitments = mapper.createObjectNode(); commitments.put("salt", salt); commitments.set("values", values);
        String previousHash = repository.findFirstByOrderBySequenceDesc().map(AuditEvent::getContentHash).orElse(GENESIS_HASH);
        Instant recordedAt = Instant.now(); String id = UUID.randomUUID().toString();
        AuditEvent event = new AuditEvent(id, request.eventType(), request.actorId(), request.resourceType(), request.resourceId(), request.occurredAt(), recordedAt,
                canonical(request.payload()), canonical(commitments), previousHash, "PENDING");
        event = repository.saveAndFlush(event);
        event.setContentHash(sha256(canonical(hashInput(event, request.payload(), values))));
        return response(event);
    }

    @Transactional(readOnly = true)
    public EventPage query(String actorId, String resourceType, String resourceId, String eventType, Instant from, Instant to, Long cursor, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        Specification<AuditEvent> spec = (root, query, builder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (actorId != null) predicates.add(builder.equal(root.get("actorId"), actorId)); if (resourceType != null) predicates.add(builder.equal(root.get("resourceType"), resourceType));
            if (resourceId != null) predicates.add(builder.equal(root.get("resourceId"), resourceId)); if (eventType != null) predicates.add(builder.equal(root.get("eventType"), eventType));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("recordedAt"), from)); if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("recordedAt"), to));
            if (cursor != null) predicates.add(builder.greaterThan(root.get("sequence"), cursor)); return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var page = repository.findAll(spec, PageRequest.of(0, limit + 1, Sort.by("sequence").ascending())); var events = page.getContent(); boolean more = events.size() > limit;
        if (more) events = events.subList(0, limit); return new EventPage(events.stream().map(this::response).toList(), more ? String.valueOf(events.getLast().getSequence()) : null);
    }

    @Transactional(readOnly = true)
    public VerificationResult verify() {
        String previous = GENESIS_HASH; long checked = 0;
        for (AuditEvent event : repository.findAllByOrderBySequenceAsc()) {
            if (!previous.equals(event.getPreviousHash())) return broken(checked, event, "PREVIOUS_HASH_MISMATCH");
            ObjectNode commitmentData = object(event.getCommitmentsJson()); ObjectNode values = (ObjectNode) commitmentData.get("values"); JsonNode payload = json(event.getPayloadJson());
            if (event.getStatus() == AuditEvent.EventStatus.ACTIVE) for (var entry : values.properties()) { if (!sha256(commitmentData.get("salt").asText() + ":" + canonical(requirePath(payload, entry.getKey()))).equals(entry.getValue().asText())) return broken(checked, event, "SENSITIVE_VALUE_COMMITMENT_MISMATCH"); }
            if (!sha256(canonical(hashInput(event, payload, values))).equals(event.getContentHash())) return broken(checked, event, "CONTENT_HASH_MISMATCH"); previous = event.getContentHash(); checked++;
        } return new VerificationResult(true, checked, null);
    }

    @Transactional
    public EventResponse redact(String eventId, RedactionRequest request) {
        AuditEvent event = repository.findByEventId(eventId).orElseThrow(() -> new NoSuchElementException("Event not found")); ObjectNode values = (ObjectNode) object(event.getCommitmentsJson()).get("values"); JsonNode payload = json(event.getPayloadJson());
        for (String path : request.paths()) { if (!values.has(path)) throw new IllegalArgumentException("Path is not declared redaction-sensitive: " + path); payload = replacePath(payload, path, new StringNode("[REDACTED]")); }
        ObjectNode certificate = mapper.createObjectNode(); certificate.put("version", "v1"); certificate.put("actorId", request.actorId()); certificate.put("reason", request.reason()); certificate.put("redactedAt", Instant.now().toString()); certificate.set("paths", mapper.valueToTree(request.paths().stream().sorted().toList())); event.replacePayloadForRedaction(canonical(payload), canonical(certificate)); return response(event);
    }

    @Transactional
    public Map<String, Object> archive(Instant cutoff, String policy) { var records = repository.findByRecordedAtBeforeAndStatus(cutoff, AuditEvent.EventStatus.ACTIVE); records.forEach(e -> e.archive(Instant.now(), policy == null ? "default-retention-policy" : policy)); return Map.of("archived", records.size(), "cutoff", cutoff.toString(), "policy", policy == null ? "default-retention-policy" : policy); }

    @Transactional(readOnly = true)
    public ExportBundle export(String actorId, String resourceId) { if ((actorId == null) == (resourceId == null)) throw new IllegalArgumentException("Exactly one of actorId or resourceId is required"); var selected = (actorId != null ? repository.findByActorIdOrderBySequenceAsc(actorId) : repository.findByResourceIdOrderBySequenceAsc(resourceId)).stream().map(this::response).toList(); var witnesses = repository.findAllByOrderBySequenceAsc().stream().map(e -> { ObjectNode values = (ObjectNode) object(e.getCommitmentsJson()).get("values"); return Map.<String,Object>of("sequence",e.getSequence(),"eventId",e.getEventId(),"contentHash",e.getContentHash(),"hashInput",hashInput(e,json(e.getPayloadJson()),values)); }).toList(); return new ExportBundle("v1", "SHA-256", GENESIS_HASH, selected, witnesses); }
    @Transactional(readOnly = true)
    public Map<String, Object> clientAccountReport(String accountId, Instant from, Instant to) { var page = query(null, "CLIENT_ACCOUNT", accountId, null, from, to, null, 100); return Map.of("reportType", "CLIENT_ACCOUNT_ACCESS_AUDIT", "clientAccountId", accountId, "events", page.items(), "nextCursor", page.nextCursor() == null ? "" : page.nextCursor()); }
    private VerificationResult broken(long checked, AuditEvent e, String type) { return new VerificationResult(false, checked, new Violation(e.getSequence(), e.getEventId(), type)); }
    private EventResponse response(AuditEvent e) { ObjectNode commitments = object(e.getCommitmentsJson()); return new EventResponse(e.getSequence(),e.getEventId(),e.getEventType(),e.getActorId(),e.getResourceType(),e.getResourceId(),e.getOccurredAt(),e.getRecordedAt(),json(e.getPayloadJson()),commitments.get("values").properties().stream().map(Map.Entry::getKey).sorted().toList(),e.getPreviousHash(),e.getContentHash(),e.getStatus(),e.getArchivedAt(),e.getArchivePolicy(),e.getRedactionCertificateJson() == null ? null : json(e.getRedactionCertificateJson())); }
    private JsonNode json(String text) { try { return mapper.readTree(text); } catch (JacksonException e) { throw new IllegalStateException("Stored JSON is invalid", e); } }
    private ObjectNode object(String text) { return (ObjectNode) json(text); }

    private Map<String, Object> hashInput(AuditEvent e, JsonNode payload, ObjectNode commitments) {
        Map<String, Object> input = new TreeMap<>(); input.put("hashVersion", "v1"); input.put("sequence", e.getSequence()); input.put("eventId", e.getEventId());
        input.put("eventType", e.getEventType()); input.put("actorId", e.getActorId()); input.put("resourceType", e.getResourceType()); input.put("resourceId", e.getResourceId());
        input.put("occurredAt", e.getOccurredAt() == null ? null : e.getOccurredAt().toString()); input.put("recordedAt", e.getRecordedAt().toString());
        input.put("payload", protectedPayload(payload, commitments)); input.put("sensitivePaths", commitments.properties().stream().map(Map.Entry::getKey).sorted().toList()); input.put("previousHash", e.getPreviousHash()); return input;
    }
    private JsonNode protectedPayload(JsonNode payload, ObjectNode commitments) { JsonNode result = payload.deepCopy(); for (var entry : commitments.properties()) { result = replacePath(result, entry.getKey(), mapper.createObjectNode().put("commitment", entry.getValue().asText()).put("redactionProtected", true)); } return result; }
    private JsonNode requirePath(JsonNode node, String path) { JsonNode value = node.at("/" + path.replace(".", "/")); if (value.isMissingNode()) throw new IllegalArgumentException("Sensitive path does not exist: " + path); return value; }
    private JsonNode replacePath(JsonNode source, String path, JsonNode replacement) { ObjectNode copy = (ObjectNode) source.deepCopy(); String[] parts = path.split("\\."); ObjectNode parent = copy; for (int i = 0; i < parts.length - 1; i++) { JsonNode child = parent.get(parts[i]); if (!(child instanceof ObjectNode object)) throw new IllegalArgumentException("Sensitive path does not exist: " + path); parent = object; } if (!parent.has(parts[parts.length - 1])) throw new IllegalArgumentException("Sensitive path does not exist: " + path); parent.set(parts[parts.length - 1], replacement); return copy; }
    private String canonical(Object value) { try { return mapper.writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(value); } catch (JacksonException e) { throw new IllegalArgumentException("Could not serialize JSON", e); } }
    private String sha256(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
}
