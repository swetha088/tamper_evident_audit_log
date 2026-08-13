package com.schwab.interview.audit.facade;

import com.schwab.interview.audit.config.AuditRetentionProperties;
import com.schwab.interview.audit.dto.AuditDtos.AppendEventRequest;
import com.schwab.interview.audit.dto.AuditDtos.ChainWitness;
import com.schwab.interview.audit.dto.AuditDtos.ClientAccountAccessReport;
import com.schwab.interview.audit.dto.AuditDtos.EventPageResponse;
import com.schwab.interview.audit.dto.AuditDtos.EventResponse;
import com.schwab.interview.audit.dto.AuditDtos.ExportBundleResponse;
import com.schwab.interview.audit.dto.AuditDtos.ExportCriteria;
import com.schwab.interview.audit.dto.AuditDtos.ExportRecord;
import com.schwab.interview.audit.dto.AuditDtos.RedactionRequest;
import com.schwab.interview.audit.dto.AuditDtos.VerificationResponse;
import com.schwab.interview.audit.dto.AuditDtos.ViolationResponse;
import com.schwab.interview.audit.entity.AuditEventEntity;
import com.schwab.interview.audit.entity.AuditChainStateEntity;
import com.schwab.interview.audit.exception.AuditNotFoundException;
import com.schwab.interview.audit.mapper.AuditEventMapper;
import com.schwab.interview.audit.repository.AuditEventRepository;
import com.schwab.interview.audit.repository.AuditChainStateRepository;
import com.schwab.interview.audit.utils.AuditConstants;
import com.schwab.interview.audit.utils.HashUtils;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

@Component
public class AuditLogFacade {
    private final AuditEventRepository repository;
    private final AuditChainStateRepository chainStateRepository;
    private final AuditEventMapper mapper;
    private final ObjectMapper json;
    private final AuditRetentionProperties retentionProperties;

    public AuditLogFacade(AuditEventRepository repository, AuditChainStateRepository chainStateRepository,
                          AuditEventMapper mapper, ObjectMapper json,
                          AuditRetentionProperties retentionProperties) {
        this.repository = repository;
        this.chainStateRepository = chainStateRepository;
        this.mapper = mapper;
        this.json = json;
        this.retentionProperties = retentionProperties;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EventResponse append(AppendEventRequest request) {
        if (!request.payload().isObject()) {
            throw new IllegalArgumentException("payload must be a JSON object");
        }
        List<String> paths = request.sensitivePaths() == null ? List.of()
                : request.sensitivePaths().stream().sorted().toList();
        if (new HashSet<>(paths).size() != paths.size()) {
            throw new IllegalArgumentException("sensitivePaths must not contain duplicates");
        }

        String salt = UUID.randomUUID().toString();
        ObjectNode values = json.createObjectNode();
        for (String path : paths) {
            values.put(path, HashUtils.sha256(salt + ":" + canonical(path(request.payload(), path))));
        }
        ObjectNode commitments = json.createObjectNode();
        commitments.put("salt", salt);
        commitments.set("values", values);

        String previous = repository.findFirstByOrderBySequenceDesc()
                .map(AuditEventEntity::getContentHash)
                .orElse(AuditConstants.GENESIS_HASH);
        AuditEventEntity event = repository.saveAndFlush(new AuditEventEntity(
                UUID.randomUUID().toString(), request.eventType(), request.actorId(), request.resourceType(),
                request.resourceId(), request.occurredAt(), Instant.now(), canonical(request.payload()),
                canonical(commitments), previous));
        event.setContentHash(HashUtils.sha256(canonical(hashInput(event, request.payload(), values))));
        AuditChainStateEntity chainState = chainStateRepository.findSingletonForUpdate()
                .orElseGet(() -> new AuditChainStateEntity(event));
        chainState.advance(event);
        chainStateRepository.save(chainState);
        return response(event);
    }

    @Transactional(readOnly = true)
    public EventPageResponse query(String actorId, String resourceType, String resourceId, String eventType,
                                   Instant from, Instant to, Long cursor, int limit) {
        validateTimeRange(from, to);
        if (limit < 1 || limit > AuditConstants.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("limit must be between 1 and " + AuditConstants.MAX_PAGE_SIZE);
        }
        List<AuditEventEntity> rows = repository.findAll(
                eventSpecification(actorId, resourceType, resourceId, eventType, from, to, cursor),
                PageRequest.of(0, limit + 1, Sort.by("sequence"))).getContent();
        boolean more = rows.size() > limit;
        if (more) {
            rows = rows.subList(0, limit);
        }
        return new EventPageResponse(rows.stream().map(this::response).toList(),
                more ? String.valueOf(rows.getLast().getSequence()) : null);
    }

    @Transactional(readOnly = true)
    public ExportBundleResponse export(String actorId, String resourceId, Instant from, Instant to) {
        validateTimeRange(from, to);
        if ((actorId == null) == (resourceId == null)) {
            throw new IllegalArgumentException("Exactly one of actorId or resourceId is required");
        }
        List<AuditEventEntity> selected = repository.findAll(
                eventSpecification(actorId, null, resourceId, null, from, to, null), Sort.by("sequence"));
        List<AuditEventEntity> chain = repository.findAllByOrderBySequenceAsc();
        List<ExportRecord> records = selected.stream().map(this::exportRecord).toList();
        List<ChainWitness> witnesses = chain.stream()
                .map(event -> new ChainWitness(event.getSequence(), event.getEventId(), event.getPreviousHash(),
                        event.getContentHash()))
                .toList();
        return new ExportBundleResponse("audit-export-v1", AuditConstants.SHA_256, AuditConstants.HASH_VERSION,
                Instant.now(), new ExportCriteria(actorId, resourceId, from, to), records, witnesses, verify());
    }

    @Transactional(readOnly = true)
    public ClientAccountAccessReport clientAccountAccessReport(String clientAccountId, Instant from, Instant to) {
        validateTimeRange(from, to);
        if (clientAccountId == null || clientAccountId.isBlank()) {
            throw new IllegalArgumentException("clientAccountId is required");
        }
        List<EventResponse> events = repository.findAll(eventSpecification(null,
                        AuditConstants.CLIENT_ACCOUNT_RESOURCE_TYPE, clientAccountId,
                        AuditConstants.CLIENT_DATA_READ_EVENT_TYPE, from, to, null), Sort.by("sequence"))
                .stream().map(this::response).toList();
        return new ClientAccountAccessReport(clientAccountId, from, to, Instant.now(), events, verify());
    }

    @Transactional(readOnly = true)
    public VerificationResponse verify() {
        String previous = AuditConstants.GENESIS_HASH;
        long checked = 0;
        AuditEventEntity last = null;
        for (AuditEventEntity event : repository.findAllByOrderBySequenceAsc()) {
            if (!previous.equals(event.getPreviousHash())) {
                return broken(checked, event, "PREVIOUS_HASH_MISMATCH");
            }
            ObjectNode commitments = object(event.getCommitmentsJson());
            ObjectNode values = (ObjectNode) commitments.get("values");
            JsonNode payload = node(event.getPayloadJson());
            if (event.getStatus() == AuditEventEntity.Status.ACTIVE) {
                for (Map.Entry<String, JsonNode> entry : values.properties()) {
                    String commitment = HashUtils.sha256(commitments.get("salt").asText() + ":"
                            + canonical(path(payload, entry.getKey())));
                    if (!commitment.equals(entry.getValue().asText())) {
                        return broken(checked, event, "SENSITIVE_VALUE_COMMITMENT_MISMATCH");
                    }
                }
            }
            if (!HashUtils.sha256(canonical(hashInput(event, payload, values))).equals(event.getContentHash())) {
                return broken(checked, event, "CONTENT_HASH_MISMATCH");
            }
            previous = event.getContentHash();
            checked++;
            last = event;
        }
        AuditChainStateEntity chainState = chainStateRepository.findById(AuditChainStateEntity.SINGLETON_ID)
                .orElse(null);
        if (chainState == null && last != null) {
            return new VerificationResponse(false, checked,
                    new ViolationResponse(last.getSequence(), last.getEventId(), "CHAIN_HEAD_MISSING"));
        }
        if (chainState != null && (last == null
                || !Objects.equals(chainState.getHeadSequence(), last.getSequence())
                || !Objects.equals(chainState.getHeadEventId(), last.getEventId())
                || !Objects.equals(chainState.getHeadHash(), last.getContentHash()))) {
            return new VerificationResponse(false, checked, new ViolationResponse(chainState.getHeadSequence(),
                    chainState.getHeadEventId(), "CHAIN_HEAD_MISMATCH"));
        }
        return new VerificationResponse(true, checked, null);
    }

    @Transactional
    public EventResponse redact(String eventId, RedactionRequest request) {
        AuditEventEntity event = repository.findByEventId(eventId)
                .orElseThrow(() -> new AuditNotFoundException("Event not found"));
        ObjectNode values = (ObjectNode) object(event.getCommitmentsJson()).get("values");
        JsonNode payload = node(event.getPayloadJson());
        for (String path : request.paths()) {
            if (!values.has(path)) {
                throw new IllegalArgumentException("Path is not declared redaction-sensitive: " + path);
            }
            payload = replace(payload, path, new StringNode(AuditConstants.REDACTED_VALUE));
        }
        ObjectNode certificate = json.createObjectNode();
        certificate.put("actorId", request.actorId());
        certificate.put("reason", request.reason());
        certificate.put("redactedAt", Instant.now().toString());
        event.redact(canonical(payload), canonical(certificate));
        return response(event);
    }

    @Transactional
    public Map<String, Object> archive(Instant cutoff, String policy) {
        Instant resolvedCutoff = cutoff == null ? Instant.now().minus(retentionProperties.getDefaultWindow()) : cutoff;
        String resolvedPolicy = policy == null ? "default-retention-policy" : policy;
        List<AuditEventEntity> records = repository.findByRecordedAtBeforeAndStatusNot(resolvedCutoff,
                AuditEventEntity.Status.ARCHIVED);
        records.forEach(event -> event.archive(Instant.now(), resolvedPolicy));
        return Map.of("archived", records.size(), "cutoff", resolvedCutoff, "policy", resolvedPolicy);
    }

    private Specification<AuditEventEntity> eventSpecification(String actorId, String resourceType, String resourceId,
                                                                 String eventType, Instant from, Instant to,
                                                                 Long cursor) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            equalsIfSupplied(predicates, builder, root, "actorId", actorId);
            equalsIfSupplied(predicates, builder, root, "resourceType", resourceType);
            equalsIfSupplied(predicates, builder, root, "resourceId", resourceId);
            equalsIfSupplied(predicates, builder, root, "eventType", eventType);
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("recordedAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("recordedAt"), to));
            }
            if (cursor != null) {
                predicates.add(builder.greaterThan(root.get("sequence"), cursor));
            }
            return predicates.isEmpty() ? builder.conjunction()
                    : builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void equalsIfSupplied(List<Predicate> predicates,
                                  jakarta.persistence.criteria.CriteriaBuilder builder,
                                  jakarta.persistence.criteria.Root<AuditEventEntity> root,
                                  String field, String value) {
        if (value != null) {
            predicates.add(builder.equal(root.get(field), value));
        }
    }

    private EventResponse response(AuditEventEntity event) {
        EventResponse base = mapper.toResponse(event);
        ObjectNode commitments = object(event.getCommitmentsJson());
        return new EventResponse(base.sequence(), base.eventId(), base.eventType(), base.actorId(),
                base.resourceType(), base.resourceId(), base.occurredAt(), base.recordedAt(),
                node(event.getPayloadJson()), commitments.get("values").properties().stream()
                        .map(Map.Entry::getKey).sorted().toList(), base.previousHash(), base.contentHash(),
                base.status(), base.archivedAt(), base.archivePolicy(),
                event.getRedactionCertificateJson() == null ? null : node(event.getRedactionCertificateJson()));
    }

    private ExportRecord exportRecord(AuditEventEntity event) {
        ObjectNode commitments = object(event.getCommitmentsJson());
        ObjectNode values = (ObjectNode) commitments.get("values");
        JsonNode payload = node(event.getPayloadJson());
        return new ExportRecord(event.getSequence(), event.getEventId(), event.getEventType(), event.getActorId(),
                event.getResourceType(), event.getResourceId(), event.getOccurredAt(), event.getRecordedAt(),
                payload, protect(payload, values), values.properties().stream().map(Map.Entry::getKey).sorted().toList(),
                event.getPreviousHash(), event.getContentHash());
    }

    private VerificationResponse broken(long checked, AuditEventEntity event, String type) {
        return new VerificationResponse(false, checked,
                new ViolationResponse(event.getSequence(), event.getEventId(), type));
    }

    private Map<String, Object> hashInput(AuditEventEntity event, JsonNode payload, ObjectNode values) {
        Map<String, Object> input = new TreeMap<>();
        input.put("hashVersion", AuditConstants.HASH_VERSION);
        input.put("sequence", event.getSequence());
        input.put("eventId", event.getEventId());
        input.put("eventType", event.getEventType());
        input.put("actorId", event.getActorId());
        input.put("resourceType", event.getResourceType());
        input.put("resourceId", event.getResourceId());
        input.put("occurredAt", event.getOccurredAt());
        input.put("recordedAt", event.getRecordedAt());
        input.put("payload", protect(payload, values));
        input.put("sensitivePaths", values.properties().stream().map(Map.Entry::getKey).sorted().toList());
        input.put("previousHash", event.getPreviousHash());
        return input;
    }

    private JsonNode protect(JsonNode payload, ObjectNode values) {
        JsonNode protectedPayload = payload.deepCopy();
        for (Map.Entry<String, JsonNode> entry : values.properties()) {
            protectedPayload = replace(protectedPayload, entry.getKey(),
                    json.createObjectNode().put("commitment", entry.getValue().asText()));
        }
        return protectedPayload;
    }

    private JsonNode path(JsonNode node, String path) {
        JsonNode value = node.at("/" + path.replace(".", "/"));
        if (value.isMissingNode()) {
            throw new IllegalArgumentException("Sensitive path does not exist: " + path);
        }
        return value;
    }

    private JsonNode replace(JsonNode node, String path, JsonNode value) {
        ObjectNode copy = (ObjectNode) node.deepCopy();
        ObjectNode parent = copy;
        String[] segments = path.split("\\.");
        for (int index = 0; index < segments.length - 1; index++) {
            if (!(parent.get(segments[index]) instanceof ObjectNode child)) {
                throw new IllegalArgumentException("Sensitive path does not exist: " + path);
            }
            parent = child;
        }
        parent.set(segments[segments.length - 1], value);
        return copy;
    }

    private void validateTimeRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
    }

    private String canonical(Object value) {
        try {
            return json.writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private JsonNode node(String value) {
        try {
            return json.readTree(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ObjectNode object(String value) {
        return (ObjectNode) node(value);
    }
}
