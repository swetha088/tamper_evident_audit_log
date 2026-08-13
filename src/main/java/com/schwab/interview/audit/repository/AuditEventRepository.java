package com.schwab.interview.audit.repository;

import com.schwab.interview.audit.entity.AuditEventEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface AuditEventRepository
    extends JpaRepository<AuditEventEntity, Long>, JpaSpecificationExecutor<AuditEventEntity> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<AuditEventEntity> findFirstByOrderBySequenceDesc();

  Optional<AuditEventEntity> findByEventId(String eventId);

  List<AuditEventEntity> findAllByOrderBySequenceAsc();

  List<AuditEventEntity> findByRecordedAtBeforeAndStatusNot(
      Instant cutoff, AuditEventEntity.Status status);

  List<AuditEventEntity> findByActorIdOrderBySequenceAsc(String actorId);

  List<AuditEventEntity> findByResourceIdOrderBySequenceAsc(String resourceId);
}
