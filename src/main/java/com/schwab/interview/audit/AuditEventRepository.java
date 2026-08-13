package com.schwab.interview.audit;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditEvent> findFirstByOrderBySequenceDesc();
    Optional<AuditEvent> findByEventId(String eventId);
    List<AuditEvent> findAllByOrderBySequenceAsc();
    List<AuditEvent> findByRecordedAtBeforeAndStatus(Instant cutoff, AuditEvent.EventStatus status);
    Page<AuditEvent> findByActorIdAndSequenceGreaterThanOrderBySequenceAsc(String actorId, Long sequence, Pageable page);
    Page<AuditEvent> findByResourceTypeAndResourceIdAndSequenceGreaterThanOrderBySequenceAsc(String resourceType, String resourceId, Long sequence, Pageable page);
    Page<AuditEvent> findBySequenceGreaterThanOrderBySequenceAsc(Long sequence, Pageable page);
    List<AuditEvent> findByActorIdOrderBySequenceAsc(String actorId);
    List<AuditEvent> findByResourceIdOrderBySequenceAsc(String resourceId);
}
