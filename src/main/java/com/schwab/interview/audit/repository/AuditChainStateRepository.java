package com.schwab.interview.audit.repository;

import com.schwab.interview.audit.entity.AuditChainStateEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AuditChainStateRepository extends JpaRepository<AuditChainStateEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from AuditChainStateEntity state where state.id = " + AuditChainStateEntity.SINGLETON_ID)
    Optional<AuditChainStateEntity> findSingletonForUpdate();
}
