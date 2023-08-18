package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.role.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Classification {
    PUBLIC, PRIVATE, RESTRICTED, @JsonEnumDefaultValue UNKNOWN
}
