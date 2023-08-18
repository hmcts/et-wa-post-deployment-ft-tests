package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.role.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum RoleCategory {
    JUDICIAL, LEGAL_OPERATIONS, ADMIN, CTSC, @JsonEnumDefaultValue UNKNOWN
}
