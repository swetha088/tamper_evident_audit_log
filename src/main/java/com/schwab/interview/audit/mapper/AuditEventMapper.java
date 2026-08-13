package com.schwab.interview.audit.mapper;

import com.schwab.interview.audit.dto.AuditDtos.EventResponse;
import com.schwab.interview.audit.entity.AuditEventEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuditEventMapper {
  @Mapping(target = "payload", ignore = true)
  @Mapping(target = "sensitivePaths", ignore = true)
  @Mapping(target = "redactionCertificate", ignore = true)
  EventResponse toResponse(AuditEventEntity entity);
}
