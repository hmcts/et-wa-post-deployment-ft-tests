package uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.idam;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CredentialRequest {
    private String credentialsKey;
    private boolean granularPermission;
}
